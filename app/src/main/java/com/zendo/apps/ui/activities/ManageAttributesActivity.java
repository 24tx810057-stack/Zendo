package com.zendo.apps.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.zendo.apps.R;
import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.Brand;
import com.zendo.apps.data.models.Category;
import com.zendo.apps.databinding.ActivityManageAttributesBinding;
import com.zendo.apps.ui.adapters.AttributeAdapter;
import com.zendo.apps.viewmodels.ProductViewModel;

import java.util.ArrayList;

public class ManageAttributesActivity extends AppCompatActivity {

    private ActivityManageAttributesBinding binding;
    private ProductViewModel viewModel;
    private AttributeAdapter adapter;
    private int currentTab = 0; // 0: Category, 1: Brand

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageAttributesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        setupRecyclerView();
        setupListeners();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new AttributeAdapter(new ArrayList<>(), item -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa mục này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (item instanceof Category) {
                            viewModel.deleteCategory(((Category) item).getId()).observe(this, this::handleActionResult);
                        } else if (item instanceof Brand) {
                            viewModel.deleteBrand(((Brand) item).getId()).observe(this, this::handleActionResult);
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        binding.rvAttributes.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackManage.setOnClickListener(v -> finish());

        binding.tabLayoutAttributes.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                observeData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnAddAttribute.setOnClickListener(v -> showAddDialog());
    }

    private void observeData() {
        binding.pbManage.setVisibility(View.VISIBLE);
        if (currentTab == 0) {
            viewModel.getCategories().observe(this, categories -> {
                binding.pbManage.setVisibility(View.GONE);
                if (currentTab == 0) adapter.updateList(categories);
            });
        } else {
            viewModel.getBrands().observe(this, brands -> {
                binding.pbManage.setVisibility(View.GONE);
                if (currentTab == 1) adapter.updateList(brands);
            });
        }
    }

    private void showAddDialog() {
        if (currentTab == 0) {
            showAddCategoryDialog();
        } else {
            showAddBrandDialog();
        }
    }

    private void showAddCategoryDialog() {
        EditText et = new EditText(this);
        et.setHint("Tên danh mục mới");
        
        new AlertDialog.Builder(this)
                .setTitle("Thêm danh mục")
                .setView(et)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = et.getText().toString().trim();
                    if (!TextUtils.isEmpty(name)) {
                        viewModel.addCategory(name).observe(this, this::handleActionResult);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddBrandDialog() {
        viewModel.getCategories().observe(this, categories -> {
            if (categories == null || categories.isEmpty()) {
                Toast.makeText(this, "Vui lòng thêm danh mục trước!", Toast.LENGTH_SHORT).show();
                return;
            }

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            layout.setPadding(padding, padding, padding, padding);

            Spinner spinner = new Spinner(this);
            List<String> catNames = new ArrayList<>();
            for (Category c : categories) catNames.add(c.getName());
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, catNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            EditText etName = new EditText(this);
            etName.setHint("Tên thương hiệu mới");
            etName.setPadding(0, padding, 0, 0);

            androidx.appcompat.widget.AppCompatTextView label = new androidx.appcompat.widget.AppCompatTextView(this);
            label.setText("Chọn Danh mục:");
            layout.addView(label);
            layout.addView(spinner);
            layout.addView(etName);

            new AlertDialog.Builder(this)
                    .setTitle("Thêm thương hiệu")
                    .setView(layout)
                    .setPositiveButton("Thêm", (dialog, which) -> {
                        String name = etName.getText().toString().trim();
                        int selectedPos = spinner.getSelectedItemPosition();
                        if (!TextUtils.isEmpty(name) && selectedPos != -1) {
                            String categoryId = categories.get(selectedPos).getId();
                            viewModel.addBrand(name, categoryId).observe(this, this::handleActionResult);
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void handleActionResult(AuthResultState<Void> state) {
        switch (state.getStatus()) {
            case LOADING:
                binding.pbManage.setVisibility(View.VISIBLE);
                break;
            case SUCCESS:
                binding.pbManage.setVisibility(View.GONE);
                Toast.makeText(this, "Thao tác thành công!", Toast.LENGTH_SHORT).show();
                break;
            case ERROR:
                binding.pbManage.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi: " + state.getMessage(), Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
