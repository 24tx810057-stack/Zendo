package com.example.buoi1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestWarrantyActivity extends AppCompatActivity {

    private ImageView ivProduct;
    private TextView tvProductName, tvExpiryInfo;
    private Spinner spErrorType;
    private EditText etDesc;
    private RecyclerView rvImages;
    private View btnPickImages;
    private Button btnSubmit;

    private Order order;
    private CartItem cartItem;
    private Date expiryDate;
    private FirebaseFirestore db;
    private List<String> base64Images = new ArrayList<>();
    private ImagePreviewAdapter adapter;

    private final String[] errorTypes = {
            "Chọn nhóm lỗi",
            "Lỗi nguồn/Khởi động",
            "Lỗi màn hình/Cảm ứng",
            "Lỗi âm thanh/Loa/Mic",
            "Lỗi Pin/Sạc",
            "Lỗi phần mềm/Hệ điều hành",
            "Lỗi khác (Vui lòng mô tả)"
    };

    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null) {
                    for (Uri uri : uris) {
                        if (base64Images.size() >= 3) break;
                        String base64 = convertUriToBase64(uri);
                        if (base64 != null) base64Images.add(base64);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_warranty);

        db = FirebaseFirestore.getInstance();
        order = (Order) getIntent().getSerializableExtra("order_data");
        cartItem = (CartItem) getIntent().getSerializableExtra("cart_item");
        expiryDate = (Date) getIntent().getSerializableExtra("expiry_date");

        if (order == null || cartItem == null) {
            finish();
            return;
        }

        initViews();
        displayInfo();
    }

    private void initViews() {
        ivProduct = findViewById(R.id.ivWarrantyProduct);
        tvProductName = findViewById(R.id.tvWarrantyProductName);
        tvExpiryInfo = findViewById(R.id.tvWarrantyExpiryInfo);
        spErrorType = findViewById(R.id.spWarrantyErrorType);
        etDesc = findViewById(R.id.etWarrantyDesc);
        rvImages = findViewById(R.id.rvWarrantyImages);
        btnPickImages = findViewById(R.id.btnPickWarrantyImages);
        btnSubmit = findViewById(R.id.btnSubmitWarranty);

        findViewById(R.id.btnBackWarranty).setOnClickListener(v -> finish());

        // Setup Spinner
        ArrayAdapter<String> errorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, errorTypes);
        errorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spErrorType.setAdapter(errorAdapter);

        // Setup RecyclerView
        adapter = new ImagePreviewAdapter(base64Images, position -> {
            base64Images.remove(position);
            adapter.notifyDataSetChanged();
        });
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(adapter);

        btnPickImages.setOnClickListener(v -> {
            if (base64Images.size() < 3) pickImagesLauncher.launch("image/*");
            else Toast.makeText(this, "Tối đa 3 ảnh bằng chứng", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> submitRequest());
    }

    private void displayInfo() {
        tvProductName.setText(cartItem.getProductName());
        if (expiryDate != null) {
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expiryDate);
            tvExpiryInfo.setText("Hạn bảo hành: " + dateStr);
        }

        String imgData = cartItem.getProductImageUrl();
        if (imgData != null) {
            if (imgData.startsWith("http")) Glide.with(this).load(imgData).into(ivProduct);
            else {
                try {
                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivProduct.setImageBitmap(bitmap);
                } catch (Exception e) {}
            }
        }
    }

    private void submitRequest() {
        int errorPos = spErrorType.getSelectedItemPosition();
        if (errorPos == 0) {
            Toast.makeText(this, "Vui lòng chọn nhóm lỗi", Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = etDesc.getText().toString().trim();
        if (desc.isEmpty()) {
            Toast.makeText(this, "Vui lòng mô tả chi tiết tình trạng máy", Toast.LENGTH_SHORT).show();
            return;
        }

        if (base64Images.isEmpty()) {
            Toast.makeText(this, "Vui lòng cung cấp ít nhất 1 ảnh minh chứng lỗi", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("ĐANG GỬI...");

        Map<String, Object> request = new HashMap<>();
        request.put("orderId", order.getId());
        request.put("productId", cartItem.getProductId());
        request.put("userEmail", order.getUserEmail());
        request.put("errorType", errorTypes[errorPos]);
        request.put("description", desc);
        request.put("evidenceImages", base64Images);
        request.put("status", "pending_repair");
        request.put("type", "warranty"); // Phân biệt với refund
        request.put("timestamp", System.currentTimeMillis());

        db.collection("warranty_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Đã gửi yêu cầu bảo hành thành công!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("GỬI YÊU CẦU BẢO HÀNH");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
}
