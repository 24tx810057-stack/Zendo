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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestReturnActivity extends AppCompatActivity {

    private ImageView ivProduct;
    private TextView tvProductName, tvOrderTotal;
    private Spinner spReason;
    private EditText etDesc;
    private RecyclerView rvImages;
    private View btnPickImages;
    private Button btnSubmit;
    
    private Order order;
    private FirebaseFirestore db;
    private List<String> base64Images = new ArrayList<>();
    private ImagePreviewAdapter adapter;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    private final String[] reasons = {
        "Chọn lý do trả hàng",
        "Sản phẩm bị lỗi, không hoạt động",
        "Sản phẩm khác với mô tả",
        "Giao sai sản phẩm/thiếu hàng",
        "Hàng giả/hàng nhái",
        "Sản phẩm bị bể vỡ/móp méo",
        "Không còn nhu cầu (Hàng nguyên seal)"
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
        setContentView(R.layout.activity_request_return);

        db = FirebaseFirestore.getInstance();
        order = (Order) getIntent().getSerializableExtra("order_data");

        if (order == null) {
            finish();
            return;
        }

        initViews();
        displayOrderInfo();
    }

    private void initViews() {
        ivProduct = findViewById(R.id.ivReturnProduct);
        tvProductName = findViewById(R.id.tvReturnProductName);
        tvOrderTotal = findViewById(R.id.tvReturnOrderTotal);
        spReason = findViewById(R.id.spReturnReason);
        etDesc = findViewById(R.id.etReturnDesc);
        rvImages = findViewById(R.id.rvReturnImages);
        btnPickImages = findViewById(R.id.btnPickReturnImages);
        btnSubmit = findViewById(R.id.btnSubmitReturn);

        findViewById(R.id.btnBackRequest).setOnClickListener(v -> finish());

        // Setup Spinner
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reasons);
        reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReason.setAdapter(reasonAdapter);

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

    private void displayOrderInfo() {
        tvOrderTotal.setText("Số tiền hoàn: " + formatter.format(order.getTotalAmount()) + "đ");
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem firstItem = order.getItems().get(0);
            tvProductName.setText(firstItem.getProductName() + (order.getItems().size() > 1 ? " và các sản phẩm khác" : ""));
            
            String imgData = firstItem.getProductImageUrl();
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
    }

    private void submitRequest() {
        int reasonPos = spReason.getSelectedItemPosition();
        if (reasonPos == 0) {
            Toast.makeText(this, "Vui lòng chọn lý do trả hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (base64Images.isEmpty()) {
            Toast.makeText(this, "Vui lòng cung cấp ít nhất 1 ảnh bằng chứng", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("ĐANG GỬI...");

        Map<String, Object> request = new HashMap<>();
        request.put("orderId", order.getId());
        request.put("userEmail", order.getUserEmail());
        request.put("reason", reasons[reasonPos]);
        request.put("description", etDesc.getText().toString().trim());
        request.put("evidenceImages", base64Images);
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());
        request.put("refundAmount", order.getTotalAmount());

        db.collection("return_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    // Cập nhật trạng thái đơn hàng sang "Yêu cầu trả hàng"
                    db.collection("orders").document(order.getId()).update("status", "Yêu cầu trả hàng")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã gửi yêu cầu trả hàng!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(this, OrderListActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("GỬI YÊU CẦU");
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
