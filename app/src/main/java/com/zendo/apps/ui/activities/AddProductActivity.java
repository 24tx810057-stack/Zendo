package com.zendo.apps.ui.activities;

import com.zendo.apps.R;
import com.zendo.apps.ui.adapters.ImagePreviewAdapter;
import com.zendo.apps.data.models.Product;
import com.zendo.apps.data.models.Brand;
import com.zendo.apps.data.models.Category;
import com.zendo.apps.viewmodels.ProductViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.zendo.apps.databinding.ActivityAddProductBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddProductActivity extends AppCompatActivity {

    private ActivityAddProductBinding binding;
    private ProductViewModel viewModel;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private com.google.firebase.storage.FirebaseStorage storage;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> existingImageUrls = new ArrayList<>();
    private ImagePreviewAdapter adapter;
    private boolean isEditMode = false;
    private Product existingProduct; 

    private List<Category> categoryList = new ArrayList<>();
    private List<Brand> brandList = new ArrayList<>();
    private String selectedPrefix = "SP";

    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (selectedImageUris.size() + existingImageUrls.size() >= 15) break;
                        selectedImageUris.add(uri);
                    }
                    updateImageAdapter();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        storage = com.google.firebase.storage.FirebaseStorage.getInstance();

        setupUI();
        observeViewModel();
        setupListeners();

        // Edit mode check
        existingProduct = (Product) getIntent().getSerializableExtra("edit_product");
        if (existingProduct != null) {
            isEditMode = true;
            fillData(existingProduct);
        } else {
            generateRandomId();
        }
    }

    private void setupUI() {
        // Toolbar setup
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView for images
        binding.rvAddProductImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Initial empty state for dropdowns
        setupDropdown(binding.spAddProductCategory, new ArrayList<>());
        setupDropdown(binding.spAddProductBrand, new ArrayList<>());
    }

    private void setupDropdown(AutoCompleteTextView textView, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        textView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(this, categories -> {
            this.categoryList = categories;
            List<String> names = new ArrayList<>();
            for (Category c : categories) names.add(c.getName());
            
            setupDropdown(binding.spAddProductCategory, names);
            
            // Set selection if in edit mode
            if (isEditMode && existingProduct != null) {
                binding.spAddProductCategory.setText(existingProduct.getCategory(), false);
                // Trigger brand load for the selected category
                for (Category c : categories) {
                    if (c.getName().equals(existingProduct.getCategory())) {
                        loadBrandsForCategory(c.getId());
                        break;
                    }
                }
            }
        });

        binding.spAddProductCategory.setOnItemClickListener((parent, view, position, id) -> {
            String categoryName = (String) parent.getItemAtPosition(position);
            for (Category c : categoryList) {
                if (c.getName().equals(categoryName)) {
                    loadBrandsForCategory(c.getId());
                    break;
                }
            }
        });
    }

    private void loadBrandsForCategory(String categoryId) {
        viewModel.getBrandsByCategory(categoryId).observe(this, brands -> {
            this.brandList = brands;
            List<String> names = new ArrayList<>();
            for (Brand b : brands) names.add(b.getName());
            
            setupDropdown(binding.spAddProductBrand, names);

            if (isEditMode && existingProduct != null) {
                binding.spAddProductBrand.setText(existingProduct.getBrand(), false);
            } else {
                binding.spAddProductBrand.setText("", false);
            }
        });
    }

    private void setupListeners() {
        binding.layoutPlaceholder.setOnClickListener(v -> {
            if (selectedImageUris.size() + existingImageUrls.size() < 15) pickImagesLauncher.launch("image/*");
            else Toast.makeText(this, "Tối đa 15 ảnh", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddMoreSpec.setOnClickListener(v -> showAddSpecDialog());
        binding.btnSaveProductLarge.setOnClickListener(v -> validateAndSave());
        
        binding.btnManageAttributes.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, ManageAttributesActivity.class);
            startActivity(intent);
        });
    }

    private void updateImageAdapter() {
        List<Object> displayList = new ArrayList<>();
        for (String url : existingImageUrls) displayList.add(url);
        for (Uri uri : selectedImageUris) displayList.add(uri);

        adapter = new ImagePreviewAdapter(displayList, position -> {
            if (position < existingImageUrls.size()) {
                existingImageUrls.remove(position);
            } else {
                selectedImageUris.remove(position - existingImageUrls.size());
            }
            updateImageAdapter();
        });
        binding.rvAddProductImages.setAdapter(adapter);
        
        // Update count label
        int total = existingImageUrls.size() + selectedImageUris.size();
        binding.tvImageCountLabel.setText("Hình ảnh sản phẩm (" + total + "/15)");
    }

    private void validateAndSave() {
        if (selectedImageUris.isEmpty() && existingImageUrls.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(binding.etAddProductName.getText()) || TextUtils.isEmpty(binding.etAddProductPrice.getText())) {
            Toast.makeText(this, "Vui lòng nhập Tên và Giá", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSaveProductLarge.setEnabled(false);
        binding.btnSaveProductLarge.setText("ĐANG LƯU...");

        if (selectedImageUris.isEmpty()) {
            saveProductToFirestore(existingImageUrls);
        } else {
            uploadImagesAndSave();
        }
    }

    private void uploadImagesAndSave() {
        final List<String> uploadedUrls = new ArrayList<>(existingImageUrls);
        final int totalToUpload = selectedImageUris.size();
        final AtomicInteger uploadCount = new AtomicInteger(0);

        for (Uri uri : selectedImageUris) {
            String base64 = convertUriToBase64(uri);
            if (base64 != null) {
                uploadedUrls.add(base64);
            }
        }
        
        // After converting all to Base64, save to Firestore
        saveProductToFirestore(uploadedUrls);
    }

    private String convertUriToBase64(Uri uri) {
        try {
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            // Nén ảnh để giảm kích thước (Base64 có giới hạn dung lượng trong Firestore)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, baos);
            byte[] data = baos.toByteArray();
            return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            android.util.Log.e("AddProduct", "Base64 conversion failed", e);
            return null;
        }
    }

    private void saveProductToFirestore(List<String> imageUrls) {
        String id = binding.tvAutoProductId.getText().toString();
        String name = binding.etAddProductName.getText().toString().trim();
        
        String category = binding.spAddProductCategory.getText().toString();
        String brand = binding.spAddProductBrand.getText().toString();
        
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(brand)) {
            Toast.makeText(this, "Vui lòng chọn danh mục và hãng", Toast.LENGTH_SHORT).show();
            binding.btnSaveProductLarge.setEnabled(true);
            binding.btnSaveProductLarge.setText("LƯU");
            return;
        }

        double price = Double.parseDouble(binding.etAddProductPrice.getText().toString());
        double oldPrice = binding.etAddProductOldPrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(binding.etAddProductOldPrice.getText().toString());
        int stock = binding.etAddProductStock.getText().toString().isEmpty() ? 0 : Integer.parseInt(binding.etAddProductStock.getText().toString());
        
        StringBuilder fullDesc = new StringBuilder();
        fullDesc.append("Chip: ").append(binding.etSpecChip.getText().toString()).append("\n");
        fullDesc.append("Màn hình: ").append(binding.etSpecScreen.getText().toString()).append("\n");
        fullDesc.append("RAM: ").append(binding.etSpecRam.getText().toString()).append("\n");
        fullDesc.append("Bộ nhớ trong: ").append(binding.etSpecRom.getText().toString()).append("\n");
        fullDesc.append("Pin: ").append(binding.etSpecPin.getText().toString()).append("\n");
        fullDesc.append("Hệ điều hành: ").append(binding.etSpecOs.getText().toString()).append("\n");
        fullDesc.append("Camera sau: ").append(binding.etSpecCameraRear.getText().toString()).append("\n");
        fullDesc.append("Camera trước: ").append(binding.etSpecCameraFront.getText().toString()).append("\n");
        
        for (int i = 0; i < binding.layoutDynamicSpecs.getChildCount(); i++) {
            View v = binding.layoutDynamicSpecs.getChildAt(i);
            EditText dName = v.findViewById(R.id.etDynamicSpecName);
            EditText dValue = v.findViewById(R.id.etDynamicSpecValue);
            fullDesc.append(dName.getText().toString()).append(": ").append(dValue.getText().toString()).append("\n");
        }
        fullDesc.append(binding.etAddProductDesc.getText().toString().trim());

        Set<String> tagsSet = new HashSet<>();
        tagsSet.add(brand.trim().toLowerCase());
        tagsSet.add(category.trim().toLowerCase());
        String tagsInput = binding.etAddProductTags.getText().toString().trim();
        if (!tagsInput.isEmpty()) {
            for (String p : tagsInput.split(",")) {
                if (!p.trim().isEmpty()) tagsSet.add(p.trim().toLowerCase());
            }
        }

        Product product;
        if (isEditMode && existingProduct != null) {
            product = existingProduct;
            product.setName(name);
            product.setPrice(price);
            product.setOldPrice(oldPrice);
            product.setStock(stock);
            product.setCategory(category);
            product.setBrand(brand);
            product.setDescription(fullDesc.toString().trim());
            product.setWarranty(binding.etAddProductWarranty.getText().toString().trim());
            product.setTags(new ArrayList<>(tagsSet));
            product.setImages(imageUrls);
            product.setImageUrl(imageUrls.get(0));
        } else {
            product = new Product(id, name, fullDesc.toString().trim(), price, imageUrls.get(0), category, stock, "N/A", brand, 5.0, 0, System.currentTimeMillis());
            product.setOldPrice(oldPrice);
            product.setImages(imageUrls);
            product.setCreatedAt(System.currentTimeMillis()); 
            product.setWarranty(binding.etAddProductWarranty.getText().toString().trim());
            product.setTags(new ArrayList<>(tagsSet));
        }

        db.collection("products").document(id).set(product)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.btnSaveProductLarge.setEnabled(true);
                    binding.btnSaveProductLarge.setText("LƯU");
                });
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
        btnRemove.setOnClickListener(v -> binding.layoutDynamicSpecs.removeView(specView));

        binding.layoutDynamicSpecs.addView(specView);
    }

    private void fillData(Product product) {
        binding.textViewTitle.setText("Cập nhật sản phẩm");
        binding.btnSaveProductLarge.setText("Cập nhật sản phẩm");
        binding.tvAutoProductId.setText(product.getId());
        binding.etAddProductName.setText(product.getName());
        binding.etAddProductPrice.setText(String.valueOf((long)product.getPrice()));
        if (product.getOldPrice() > 0) binding.etAddProductOldPrice.setText(String.valueOf((long)product.getOldPrice()));
        binding.etAddProductStock.setText(String.valueOf(product.getStock()));
        
        if (product.getTags() != null) {
            List<String> displayTags = new ArrayList<>(product.getTags());
            displayTags.remove(product.getBrand().toLowerCase());
            displayTags.remove(product.getCategory().toLowerCase());
            binding.etAddProductTags.setText(android.text.TextUtils.join(", ", displayTags));
        }

        binding.etAddProductWarranty.setText(product.getWarranty());
        
        // Parse specs from description
        String desc = product.getDescription();
        if (desc != null) {
            String[] lines = desc.split("\\r?\\n");
            StringBuilder generalDesc = new StringBuilder();

            for (String line : lines) {
                String trimmedLine = line.trim();
                String lowerLine = trimmedLine.toLowerCase();

                if (lowerLine.startsWith("chip:")) {
                    binding.etSpecChip.setText(trimmedLine.substring(5).trim());
                } else if (lowerLine.startsWith("màn hình:")) {
                    binding.etSpecScreen.setText(trimmedLine.substring(9).trim());
                } else if (lowerLine.startsWith("ram:")) {
                    binding.etSpecRam.setText(trimmedLine.substring(4).trim());
                } else if (lowerLine.startsWith("bộ nhớ trong:") || lowerLine.startsWith("bộ nhớ:")) {
                    int offset = lowerLine.startsWith("bộ nhớ trong:") ? 13 : 7;
                    binding.etSpecRom.setText(trimmedLine.substring(offset).trim());
                } else if (lowerLine.startsWith("pin:")) {
                    binding.etSpecPin.setText(trimmedLine.substring(4).trim());
                } else if (lowerLine.startsWith("hệ điều hành:") || lowerLine.startsWith("os:")) {
                    int offset = lowerLine.startsWith("hệ điều hành:") ? 13 : 3;
                    binding.etSpecOs.setText(trimmedLine.substring(offset).trim());
                } else if (lowerLine.startsWith("camera sau:")) {
                    binding.etSpecCameraRear.setText(trimmedLine.substring(11).trim());
                } else if (lowerLine.startsWith("camera trước:")) {
                    binding.etSpecCameraFront.setText(trimmedLine.substring(13).trim());
                } else if (trimmedLine.contains(":") && isDynamicSpec(trimmedLine)) {
                    String[] parts = trimmedLine.split(":", 2);
                    addDynamicSpecView(parts[0].trim(), parts[1].trim());
                } else if (!trimmedLine.isEmpty()) {
                    generalDesc.append(trimmedLine).append("\n");
                }
            }
            binding.etAddProductDesc.setText(generalDesc.toString().trim());
        }
        
        if (product.getImages() != null) {
            existingImageUrls.addAll(product.getImages());
            updateImageAdapter();
        }
    }

    private boolean isDynamicSpec(String line) {
        String lower = line.toLowerCase();
        return !lower.startsWith("chip:") && !lower.startsWith("màn hình:") && 
               !lower.startsWith("ram:") && !lower.startsWith("bộ nhớ trong:") && !lower.startsWith("bộ nhớ:") && 
               !lower.startsWith("pin:") && !lower.startsWith("hệ điều hành:") && !lower.startsWith("os:") &&
               !lower.startsWith("camera sau:") && !lower.startsWith("camera trước:");
    }

    private void generateRandomId() {
        binding.tvAutoProductId.setText(selectedPrefix + (new Random().nextInt(9000) + 1000));
    }
}
