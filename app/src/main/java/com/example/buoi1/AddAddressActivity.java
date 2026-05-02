package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddAddressActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etArea, etDetailAddress;
    private SwitchMaterial swDefault;
    private MaterialButtonToggleGroup toggleGroupType;
    private Button btnSave;
    private ImageView btnBack;
    
    private UserAddress editingAddress;
    private String selectedProvince = "", selectedDistrict = "", selectedWard = "";
    private AddressApiService apiService;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "").trim();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://provinces.open-api.vn/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AddressApiService.class);

        initViews();
        
        if (getIntent().hasExtra("edit_address")) {
            editingAddress = (UserAddress) getIntent().getSerializableExtra("edit_address");
            fillDataToFields(editingAddress);
            TextView tvTitle = findViewById(R.id.tvTitleAddAddress);
            if (tvTitle != null) tvTitle.setText("Sửa địa chỉ");
        }
        
        setupListeners();
        updateSaveButtonState();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etArea = findViewById(R.id.etArea);
        etDetailAddress = findViewById(R.id.etDetailAddress);
        swDefault = findViewById(R.id.swDefault);
        toggleGroupType = findViewById(R.id.toggleGroupType);
        btnSave = findViewById(R.id.btnSaveAddress);
        btnBack = findViewById(R.id.btnBackAddAddress);
        etArea.setFocusable(false);
        etArea.setClickable(true);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        etArea.setOnClickListener(v -> fetchProvinces());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSaveButtonState(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        etFullName.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        etArea.addTextChangedListener(watcher);
        etDetailAddress.addTextChangedListener(watcher);

        btnSave.setOnClickListener(v -> saveAddressToFirestore());
    }

    private void saveAddressToFirestore() {
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy email người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String detail = etDetailAddress.getText().toString().trim();
        boolean isDefault = swDefault.isChecked();
        String type = (toggleGroupType.getCheckedButtonId() == R.id.btnTypeOffice) ? "Văn phòng" : "Nhà riêng";

        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", userEmail);
        data.put("fullName", fullName);
        data.put("phone", phone);
        data.put("provinceCity", selectedProvince);
        data.put("district", selectedDistrict);
        data.put("ward", selectedWard);
        data.put("detailAddress", detail);
        data.put("default", isDefault);
        data.put("type", type);

        btnSave.setEnabled(false);
        btnSave.setText("ĐANG LƯU...");

        if (isDefault) {
            // Nếu đặt làm mặc định, bỏ mặc định của các địa chỉ khác trước
            db.collection("addresses")
                    .whereEqualTo("userEmail", userEmail)
                    .whereEqualTo("default", true)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            batch.update(document.getReference(), "default", false);
                        }
                        performFinalSave(data, batch);
                    })
                    .addOnFailureListener(e -> performFinalSave(data, null));
        } else {
            performFinalSave(data, null);
        }
    }

    private void performFinalSave(Map<String, Object> data, WriteBatch batch) {
        CollectionReference ref = db.collection("addresses");
        
        // Nếu địa chỉ đã có ID -> CẬP NHẬT. Nếu chưa có -> THÊM MỚI
        if (editingAddress != null && editingAddress.getId() != null) {
            DocumentReference docRef = ref.document(editingAddress.getId());
            if (batch != null) {
                batch.set(docRef, data);
                batch.commit().addOnCompleteListener(task -> onSaveComplete(task.isSuccessful()));
            } else {
                docRef.set(data).addOnCompleteListener(task -> onSaveComplete(task.isSuccessful()));
            }
        } else {
            if (batch != null) {
                DocumentReference newDoc = ref.document();
                batch.set(newDoc, data);
                batch.commit().addOnCompleteListener(task -> onSaveComplete(task.isSuccessful()));
            } else {
                ref.add(data).addOnCompleteListener(task -> onSaveComplete(task.isSuccessful()));
            }
        }
    }

    private void onSaveComplete(boolean success) {
        if (success) {
            Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            btnSave.setEnabled(true);
            btnSave.setText("HOÀN THÀNH");
            Toast.makeText(this, "Lỗi không thể lưu địa chỉ", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchProvinces() {
        apiService.getProvinces().enqueue(new Callback<List<AddressModels.Province>>() {
            @Override
            public void onResponse(Call<List<AddressModels.Province>> call, Response<List<AddressModels.Province>> response) {
                if (response.isSuccessful() && response.body() != null) showProvincePicker(response.body());
            }
            @Override
            public void onFailure(Call<List<AddressModels.Province>> call, Throwable t) {}
        });
    }

    private void showProvincePicker(List<AddressModels.Province> provinces) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address_picker, null);
        dialog.setContentView(view);
        TextView tvTitle = view.findViewById(R.id.tvPickerTitle);
        ListView lvData = view.findViewById(R.id.lvPickerData);
        tvTitle.setText("Chọn Tỉnh / Thành phố");
        ArrayAdapter<AddressModels.Province> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, provinces);
        lvData.setAdapter(adapter);
        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            selectedProvince = provinces.get(position).getName();
            fetchDistricts(provinces.get(position).getCode(), dialog);
        });
        dialog.show();
    }

    private void fetchDistricts(int provinceCode, BottomSheetDialog dialog) {
        apiService.getDistricts(provinceCode).enqueue(new Callback<AddressModels.Province>() {
            @Override
            public void onResponse(Call<AddressModels.Province> call, Response<AddressModels.Province> response) {
                if (response.isSuccessful() && response.body() != null) showDistrictPicker(response.body().getDistricts(), dialog);
            }
            @Override public void onFailure(Call<AddressModels.Province> call, Throwable t) {}
        });
    }

    private void showDistrictPicker(List<AddressModels.District> districts, BottomSheetDialog dialog) {
        ListView lvData = dialog.findViewById(R.id.lvPickerData);
        TextView tvTitle = dialog.findViewById(R.id.tvPickerTitle);
        tvTitle.setText("Chọn Quận / Huyện");
        ArrayAdapter<AddressModels.District> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, districts);
        lvData.setAdapter(adapter);
        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            selectedDistrict = districts.get(position).getName();
            fetchWards(districts.get(position).getCode(), dialog);
        });
    }

    private void fetchWards(int districtCode, BottomSheetDialog dialog) {
        apiService.getWards(districtCode).enqueue(new Callback<AddressModels.District>() {
            @Override
            public void onResponse(Call<AddressModels.District> call, Response<AddressModels.District> response) {
                if (response.isSuccessful() && response.body() != null) showWardPicker(response.body().getWards(), dialog);
            }
            @Override public void onFailure(Call<AddressModels.District> call, Throwable t) {}
        });
    }

    private void showWardPicker(List<AddressModels.Ward> wards, BottomSheetDialog dialog) {
        ListView lvData = dialog.findViewById(R.id.lvPickerData);
        TextView tvTitle = dialog.findViewById(R.id.tvPickerTitle);
        tvTitle.setText("Chọn Phường / Xã");
        ArrayAdapter<AddressModels.Ward> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wards);
        lvData.setAdapter(adapter);
        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            selectedWard = wards.get(position).getName();
            etArea.setText(selectedWard + ", " + selectedDistrict + ", " + selectedProvince);
            dialog.dismiss();
        });
    }

    private void fillDataToFields(UserAddress address) {
        if (address == null) return;
        etFullName.setText(address.getFullName());
        etPhone.setText(address.getPhone());
        selectedProvince = address.getProvinceCity() != null ? address.getProvinceCity() : "";
        selectedDistrict = address.getDistrict() != null ? address.getDistrict() : "";
        selectedWard = address.getWard() != null ? address.getWard() : "";
        String area = "";
        if (!selectedWard.isEmpty()) area += selectedWard;
        if (!selectedDistrict.isEmpty()) area += (area.isEmpty() ? "" : ", ") + selectedDistrict;
        if (!selectedProvince.isEmpty()) area += (area.isEmpty() ? "" : ", ") + selectedProvince;
        etArea.setText(area);
        etDetailAddress.setText(address.getDetailAddress());
        swDefault.setChecked(address.isDefault());
        if ("Văn phòng".equals(address.getType())) toggleGroupType.check(R.id.btnTypeOffice);
        else toggleGroupType.check(R.id.btnTypeHome);
    }

    private void updateSaveButtonState() {
        boolean isReady = !TextUtils.isEmpty(etFullName.getText()) && !TextUtils.isEmpty(etPhone.getText()) 
                         && !TextUtils.isEmpty(etArea.getText()) && !TextUtils.isEmpty(etDetailAddress.getText());
        btnSave.setEnabled(isReady);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(isReady ? ContextCompat.getColor(this, R.color.blue_main) : Color.parseColor("#E0E0E0")));
    }
}
