package com.example.buoi1;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

        // Khởi tạo Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://provinces.open-api.vn/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AddressApiService.class);

        initViews();
        
        if (getIntent().hasExtra("edit_address")) {
            editingAddress = (UserAddress) getIntent().getSerializableExtra("edit_address");
            fillDataToFields(editingAddress);
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etFullName.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        etArea.addTextChangedListener(watcher);
        etDetailAddress.addTextChangedListener(watcher);

        btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void fetchProvinces() {
        apiService.getProvinces().enqueue(new Callback<List<AddressModels.Province>>() {
            @Override
            public void onResponse(Call<List<AddressModels.Province>> call, Response<List<AddressModels.Province>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showProvincePicker(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<AddressModels.Province>> call, Throwable t) {
                Toast.makeText(AddAddressActivity.this, "Lỗi tải danh sách tỉnh thành", Toast.LENGTH_SHORT).show();
            }
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
            AddressModels.Province province = provinces.get(position);
            selectedProvince = province.getName();
            fetchDistricts(province.getCode(), dialog);
        });

        dialog.show();
    }

    private void fetchDistricts(int provinceCode, BottomSheetDialog dialog) {
        apiService.getDistricts(provinceCode).enqueue(new Callback<AddressModels.Province>() {
            @Override
            public void onResponse(Call<AddressModels.Province> call, Response<AddressModels.Province> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showDistrictPicker(response.body().getDistricts(), dialog);
                }
            }
            @Override
            public void onFailure(Call<AddressModels.Province> call, Throwable t) {
                Toast.makeText(AddAddressActivity.this, "Lỗi tải danh sách quận huyện", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDistrictPicker(List<AddressModels.District> districts, BottomSheetDialog dialog) {
        ListView lvData = dialog.findViewById(R.id.lvPickerData);
        TextView tvTitle = dialog.findViewById(R.id.tvPickerTitle);
        tvTitle.setText("Chọn Quận / Huyện / Thành phố");

        ArrayAdapter<AddressModels.District> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, districts);
        lvData.setAdapter(adapter);

        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            AddressModels.District district = districts.get(position);
            selectedDistrict = district.getName();
            fetchWards(district.getCode(), dialog);
        });
    }

    private void fetchWards(int districtCode, BottomSheetDialog dialog) {
        apiService.getWards(districtCode).enqueue(new Callback<AddressModels.District>() {
            @Override
            public void onResponse(Call<AddressModels.District> call, Response<AddressModels.District> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showWardPicker(response.body().getWards(), dialog);
                }
            }
            @Override
            public void onFailure(Call<AddressModels.District> call, Throwable t) {
                Toast.makeText(AddAddressActivity.this, "Lỗi tải danh sách phường xã", Toast.LENGTH_SHORT).show();
            }
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
        etArea.setText(address.getWard() + ", " + address.getDistrict() + ", " + address.getProvinceCity());
        etDetailAddress.setText(address.getDetailAddress());
        swDefault.setChecked(address.isDefault());
    }

    private void updateSaveButtonState() {
        boolean isReady = !TextUtils.isEmpty(etFullName.getText()) &&
                          !TextUtils.isEmpty(etPhone.getText()) &&
                          !TextUtils.isEmpty(etArea.getText()) &&
                          !TextUtils.isEmpty(etDetailAddress.getText());

        if (isReady) {
            btnSave.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue_main)));
            btnSave.setEnabled(true);
            btnSave.setTextColor(Color.WHITE);
        } else {
            btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnSave.setEnabled(false);
            btnSave.setTextColor(Color.parseColor("#BDBDBD"));
        }
    }
}
