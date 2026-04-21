package com.example.buoi1;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddProductActivity extends AppCompatActivity {

    private TextView tvAutoId, tvTitle, btnSave, btnAddMoreSpec;
    private EditText etName, etPrice, etOldPrice, etStock, etDesc;
    private EditText etSpecChip, etSpecScreen, etSpecRam, etSpecRom, etSpecPin, etSpecOs, etSpecCameraRear, etSpecCameraFront;
    private LinearLayout layoutDynamicSpecs;
    private RecyclerView rvImages;
    private View layoutPlaceholder, btnCancel;
    private Spinner spCategory, spBrand;
    private FirebaseFirestore db;
    
    private List<String> base64Images = new ArrayList<>();
    private ImagePreviewAdapter adapter;
    private boolean isEditMode = false;
    private Product existingProduct; 

    private String[] categories = {"Điện thoại", "Laptop", "Phụ kiện", "Máy tính bảng"};
    private String[] brands = {"iPhone", "Samsung", "Oppo", "Xiaomi", "Realme", "Vivo"};
    private String selectedPrefix = "SP";

    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (base64Images.size() >= 15) break;
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
        setContentView(R.layout.activity_add_product);

        db = FirebaseFirestore.getInstance();

        // Bind views
        tvTitle = findViewById(R.id.textViewTitle);
        btnSave = findViewById(R.id.btnSaveProduct);
        btnCancel = findViewById(R.id.btnCancelAddProduct);
        tvAutoId = findViewById(R.id.tvAutoProductId);
        
        etName = findViewById(R.id.etAddProductName);
        spCategory = findViewById(R.id.spAddProductCategory);
        spBrand = findViewById(R.id.spAddProductBrand); 
        etPrice = findViewById(R.id.etAddProductPrice);
        etOldPrice = findViewById(R.id.etAddProductOldPrice);
        etStock = findViewById(R.id.etAddProductStock);
        etDesc = findViewById(R.id.etAddProductDesc);
        
        etSpecChip = findViewById(R.id.etSpecChip);
        etSpecScreen = findViewById(R.id.etSpecScreen);
        etSpecRam = findViewById(R.id.etSpecRam);
        etSpecRom = findViewById(R.id.etSpecRom);
        etSpecPin = findViewById(R.id.etSpecPin);
        etSpecOs = findViewById(R.id.etSpecOs);
        etSpecCameraRear = findViewById(R.id.etSpecCameraRear);
        etSpecCameraFront = findViewById(R.id.etSpecCameraFront);
        
        layoutDynamicSpecs = findViewById(R.id.layoutDynamicSpecs);
        btnAddMoreSpec = findViewById(R.id.btnAddMoreSpec);

        rvImages = findViewById(R.id.rvAddProductImages);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);

        // Setup RecyclerView for Images
        adapter = new ImagePreviewAdapter(base64Images, position -> {
            base64Images.remove(position);
            adapter.notifyDataSetChanged();
        });
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(adapter);

        // Spinners
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brands);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBrand.setAdapter(brandAdapter);

        // Listeners
        layoutPlaceholder.setOnClickListener(v -> {
            if (base64Images.size() < 15) pickImagesLauncher.launch("image/*");
            else Toast.makeText(this, "Tối đa 15 ảnh", Toast.LENGTH_SHORT).show();
        });

        btnAddMoreSpec.setOnClickListener(v -> showAddSpecDialog());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            if (base64Images.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(etName.getText()) || TextUtils.isEmpty(etPrice.getText())) {
                Toast.makeText(this, "Vui lòng nhập Tên và Giá", Toast.LENGTH_SHORT).show();
            } else {
                saveProduct();
            }
        });

        // Edit mode check
        existingProduct = (Product) getIntent().getSerializableExtra("edit_product");
        if (existingProduct != null) {
            isEditMode = true;
            fillData(existingProduct);
        } else {
            generateRandomId();
        }
    }

    private void showAddSpecDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_spec, null);
        EditText etSpecName = dialogView.findViewById(R.id.etDialogSpecName);
        EditText etSpecValue = dialogView.findViewById(R.id.etDialogSpecValue);

        new AlertDialog.Builder(this)
                .setTitle("Thêm thông số kỹ thuật")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = etSpecName.getText().toString().trim();
                    String value = etSpecValue.getText().toString().trim();
                    if (!name.isEmpty() && !value.isEmpty()) {
                        addDynamicSpecView(name, value);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addDynamicSpecView(String name, String value) {
        View specView = LayoutInflater.from(this).inflate(R.layout.item_dynamic_spec, null);
        EditText etName = specView.findViewById(R.id.etDynamicSpecName);
        EditText etValue = specView.findViewById(R.id.etDynamicSpecValue);
        View btnRemove = specView.findViewById(R.id.btnRemoveSpec);

        etName.setText(name);
        etValue.setText(value);
        btnRemove.setOnClickListener(v -> layoutDynamicSpecs.removeView(specView));

        layoutDynamicSpecs.addView(specView);
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos); 
            byte[] data = baos.toByteArray();
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    private void fillData(Product product) {
        tvTitle.setText("Cập nhật sản phẩm");
        btnSave.setText("Lưu");
        tvAutoId.setText(product.getId());
        etName.setText(product.getName());
        etPrice.setText(String.valueOf((long)product.getPrice()));
        if (product.getOldPrice() > 0) etOldPrice.setText(String.valueOf((long)product.getOldPrice()));
        etStock.setText(String.valueOf(product.getStock()));
        
        String desc = product.getDescription();
        if (desc != null) {
            String[] lines = desc.split("\n");
            StringBuilder additionalDesc = new StringBuilder();
            for (String line : lines) {
                if (line.contains(": ")) {
                    String[] parts = line.split(": ", 2);
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    
                    if (key.equalsIgnoreCase("Chip")) etSpecChip.setText(val);
                    else if (key.equalsIgnoreCase("Màn hình")) etSpecScreen.setText(val);
                    else if (key.equalsIgnoreCase("RAM")) etSpecRam.setText(val);
                    else if (key.equalsIgnoreCase("Bộ nhớ trong")) etSpecRom.setText(val);
                    else if (key.equalsIgnoreCase("Pin")) etSpecPin.setText(val);
                    else if (key.equalsIgnoreCase("Hệ điều hành")) etSpecOs.setText(val);
                    else if (key.equalsIgnoreCase("Camera sau")) etSpecCameraRear.setText(val);
                    else if (key.equalsIgnoreCase("Camera trước")) etSpecCameraFront.setText(val);
                    else addDynamicSpecView(key, val);
                } else {
                    additionalDesc.append(line).append("\n");
                }
            }
            etDesc.setText(additionalDesc.toString().trim());
        }
        
        if (product.getImages() != null) {
            base64Images.clear();
            base64Images.addAll(product.getImages());
            adapter.notifyDataSetChanged();
        }
    }

    private void generateRandomId() {
        tvAutoId.setText(selectedPrefix + (new Random().nextInt(9000) + 1000));
    }

    private void saveProduct() {
        String id = tvAutoId.getText().toString();
        String name = etName.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String brand = spBrand.getSelectedItem().toString();
        double price = Double.parseDouble(etPrice.getText().toString());
        double oldPrice = etOldPrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(etOldPrice.getText().toString());
        int stock = etStock.getText().toString().isEmpty() ? 0 : Integer.parseInt(etStock.getText().toString());
        
        StringBuilder fullDesc = new StringBuilder();
        // Add default specs
        if (!etSpecChip.getText().toString().isEmpty()) fullDesc.append("Chip: ").append(etSpecChip.getText().toString()).append("\n");
        if (!etSpecScreen.getText().toString().isEmpty()) fullDesc.append("Màn hình: ").append(etSpecScreen.getText().toString()).append("\n");
        if (!etSpecRam.getText().toString().isEmpty()) fullDesc.append("RAM: ").append(etSpecRam.getText().toString()).append("\n");
        if (!etSpecRom.getText().toString().isEmpty()) fullDesc.append("Bộ nhớ trong: ").append(etSpecRom.getText().toString()).append("\n");
        if (!etSpecPin.getText().toString().isEmpty()) fullDesc.append("Pin: ").append(etSpecPin.getText().toString()).append("\n");
        if (!etSpecOs.getText().toString().isEmpty()) fullDesc.append("Hệ điều hành: ").append(etSpecOs.getText().toString()).append("\n");
        if (!etSpecCameraRear.getText().toString().isEmpty()) fullDesc.append("Camera sau: ").append(etSpecCameraRear.getText().toString()).append("\n");
        if (!etSpecCameraFront.getText().toString().isEmpty()) fullDesc.append("Camera trước: ").append(etSpecCameraFront.getText().toString()).append("\n");
        
        // Add dynamic specs
        for (int i = 0; i < layoutDynamicSpecs.getChildCount(); i++) {
            View v = layoutDynamicSpecs.getChildAt(i);
            EditText dName = v.findViewById(R.id.etDynamicSpecName);
            EditText dValue = v.findViewById(R.id.etDynamicSpecValue);
            fullDesc.append(dName.getText().toString()).append(": ").append(dValue.getText().toString()).append("\n");
        }
        
        fullDesc.append(etDesc.getText().toString().trim());

        Product product = new Product(id, name, fullDesc.toString().trim(), price, base64Images.get(0), category, stock, "N/A", brand, 4.8, 0, System.currentTimeMillis());
        product.setOldPrice(oldPrice);
        product.setImages(base64Images);

        db.collection("products").document(id).set(product)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
