package com.example.buoi1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

    private TextView tvAutoId, tvTitle;
    private EditText etName, etPrice, etOldPrice, etStock, etDesc;
    private EditText etSpecChip, etSpecScreen, etSpecRam, etSpecRom, etSpecPin, etSpecCamera, etSpecOs;
    private RecyclerView rvImages;
    private View layoutPlaceholder;
    private Spinner spCategory, spBrand;
    private Button btnSave, btnCancel;
    private FirebaseFirestore db;
    
    private List<String> base64Images = new ArrayList<>();
    private ImagePreviewAdapter adapter;
    private boolean isEditMode = false;
    private Product existingProduct; 

    private String[] categories = {"Điện thoại"};
    private String[] brands = {"iPhone", "Samsung", "Oppo", "Xiaomi"};
    private String selectedPrefix = "SP";

    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uriListValid(uris)) {
                    for (Uri uri : uris) {
                        String base64 = convertUriToBase64(uri);
                        if (base64 != null) base64Images.add(base64);
                    }
                    adapter.notifyDataSetChanged();
                    checkPlaceholder();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.textViewTitle);
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
        etSpecCamera = findViewById(R.id.etSpecCamera);
        etSpecOs = findViewById(R.id.etSpecOs);

        rvImages = findViewById(R.id.rvAddProductImages);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);
        btnSave = findViewById(R.id.btnSaveProduct);
        btnCancel = findViewById(R.id.btnCancelAddProduct);

        adapter = new ImagePreviewAdapter(base64Images, position -> {
            base64Images.remove(position);
            adapter.notifyDataSetChanged();
            checkPlaceholder();
        });
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(adapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brands);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBrand.setAdapter(brandAdapter);

        layoutPlaceholder.setOnClickListener(v -> pickImagesLauncher.launch("image/*"));
        rvImages.setOnClickListener(v -> pickImagesLauncher.launch("image/*"));

        existingProduct = (Product) getIntent().getSerializableExtra("edit_product");
        if (existingProduct != null) {
            isEditMode = true;
            fillData(existingProduct);
        } else {
            generateRandomId();
        }

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            if (base64Images.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            } else {
                saveProduct();
            }
        });
    }

    private boolean uriListValid(List<Uri> uris) {
        return uris != null && !uris.isEmpty();
    }

    private void checkPlaceholder() {
        if (base64Images.isEmpty()) {
            rvImages.setVisibility(View.GONE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
        } else {
            rvImages.setVisibility(View.VISIBLE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos); 
            byte[] data = baos.toByteArray();
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    private void fillData(Product product) {
        tvTitle.setText("CẬP NHẬT SẢN PHẨM");
        btnSave.setText("CẬP NHẬT");
        tvAutoId.setText(product.getId());
        etName.setText(product.getName());
        etPrice.setText(String.valueOf((long)product.getPrice()));
        if (product.getOldPrice() > 0) {
            etOldPrice.setText(String.valueOf((long)product.getOldPrice()));
        } else {
            etOldPrice.setText("");
        }
        etStock.setText(String.valueOf(product.getStock()));
        
        String desc = product.getDescription();
        if (desc != null) {
            String[] lines = desc.split("\n");
            StringBuilder additionalDesc = new StringBuilder();
            for (String line : lines) {
                String lower = line.toLowerCase();
                if (lower.startsWith("chip:")) etSpecChip.setText(line.substring(5).trim());
                else if (lower.startsWith("màn hình:")) etSpecScreen.setText(line.substring(9).trim());
                else if (lower.startsWith("ram:")) etSpecRam.setText(line.substring(4).trim());
                else if (lower.startsWith("bộ nhớ trong:")) etSpecRom.setText(line.substring(13).trim());
                else if (lower.startsWith("pin:")) etSpecPin.setText(line.substring(4).trim());
                else if (lower.startsWith("camera:")) etSpecCamera.setText(line.substring(7).trim());
                else if (lower.startsWith("hệ điều hành:")) etSpecOs.setText(line.substring(13).trim());
                else additionalDesc.append(line).append("\n");
            }
            etDesc.setText(additionalDesc.toString().trim());
        }
        
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            base64Images.clear();
            base64Images.addAll(product.getImages());
            adapter.notifyDataSetChanged();
            checkPlaceholder();
        } else if (product.getImageUrl() != null) {
            base64Images.clear();
            base64Images.add(product.getImageUrl());
            adapter.notifyDataSetChanged();
            checkPlaceholder();
        }

        for (int i = 0; i < brands.length; i++) {
            if (brands[i].equals(product.getBrand())) {
                spBrand.setSelection(i);
                break;
            }
        }
        
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(product.getCategory())) {
                spCategory.setSelection(i);
                break;
            }
        }
    }

    private void generateRandomId() {
        int randomNumber = new Random().nextInt(9000) + 1000;
        tvAutoId.setText(selectedPrefix + randomNumber);
    }

    private void saveProduct() {
        String id = tvAutoId.getText().toString();
        String name = etName.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String brand = spBrand.getSelectedItem().toString();
        String priceStr = etPrice.getText().toString().trim();
        String oldPriceStr = etOldPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();
        
        StringBuilder fullDesc = new StringBuilder();
        if (!etSpecChip.getText().toString().isEmpty()) fullDesc.append("Chip: ").append(etSpecChip.getText().toString()).append("\n");
        if (!etSpecScreen.getText().toString().isEmpty()) fullDesc.append("Màn hình: ").append(etSpecScreen.getText().toString()).append("\n");
        if (!etSpecRam.getText().toString().isEmpty()) fullDesc.append("RAM: ").append(etSpecRam.getText().toString()).append("\n");
        if (!etSpecRom.getText().toString().isEmpty()) fullDesc.append("Bộ nhớ trong: ").append(etSpecRom.getText().toString()).append("\n");
        if (!etSpecPin.getText().toString().isEmpty()) fullDesc.append("Pin: ").append(etSpecPin.getText().toString()).append("\n");
        if (!etSpecCamera.getText().toString().isEmpty()) fullDesc.append("Camera: ").append(etSpecCamera.getText().toString()).append("\n");
        if (!etSpecOs.getText().toString().isEmpty()) fullDesc.append("Hệ điều hành: ").append(etSpecOs.getText().toString()).append("\n");
        
        fullDesc.append(etDesc.getText().toString().trim());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Vui lòng nhập Tên và Giá", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        double oldPrice = oldPriceStr.isEmpty() ? 0 : Double.parseDouble(oldPriceStr);
        int stock = stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr);
        
        int discount = 0;
        if (oldPrice > price) {
            discount = (int) Math.round(((oldPrice - price) / oldPrice) * 100);
        } else if (oldPrice > 0 && oldPrice <= price) {
            // Trường hợp nhập sai, giá cũ lại thấp hơn giá mới, ta coi như không có giá cũ
            oldPrice = 0;
            discount = 0;
        }
        
        long currentTime = System.currentTimeMillis();
        String mainImage = base64Images.get(0);

        int currentSoldCount = 0;
        double currentRating = 4.8; 
        if (isEditMode && existingProduct != null) {
            currentSoldCount = existingProduct.getSoldCount();
            currentRating = existingProduct.getRating();
            currentTime = existingProduct.getCreatedAt();
        }

        Product product = new Product(id, name, fullDesc.toString().trim(), price, mainImage, category, stock, "N/A", brand, currentRating, currentSoldCount, currentTime);
        product.setOldPrice(oldPrice);
        product.setDiscountPercent(discount);
        product.setImages(base64Images);

        db.collection("products").document(id).set(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
