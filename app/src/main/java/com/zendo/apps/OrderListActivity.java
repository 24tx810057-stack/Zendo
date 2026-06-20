package com.zendo.apps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.zendo.apps.databinding.ActivityOrderListBinding;
import java.util.ArrayList;

public class OrderListActivity extends AppCompatActivity {

    private ActivityOrderListBinding binding;
    private OrderAdapter adapter;
    private OrderViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString("user_email", "");
        String userRole = sharedPref.getString("user_role", "user");

        initViews();
        setupViewModel(userEmail, userRole);
        setupTabs();
        
        // Mở đúng Tab được yêu cầu từ Intent
        int tabIndex = getIntent().getIntExtra("tab_index", 0);
        if (tabIndex < binding.tabLayoutOrders.getTabCount()) {
            TabLayout.Tab tab = binding.tabLayoutOrders.getTabAt(tabIndex);
            if (tab != null) {
                tab.select();
            }
        }
        
        // Đổi tiêu đề dựa trên Role
        if ("admin".equals(userRole)) {
            binding.tvOrderListTitle.setText("Quản lý đơn hàng");
        } else {
            binding.tvOrderListTitle.setText("Đơn mua của tôi");
        }

        // Tự động mở search nếu được yêu cầu từ trang Detail
        if (getIntent().getBooleanExtra("open_search", false)) {
            showSearchBar();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("open_search", false)) {
            showSearchBar();
        }
    }

    private void initViews() {
        binding.btnBackOrderList.setOnClickListener(v -> finish());
        
        binding.btnSearchOrder.setOnClickListener(v -> showSearchBar());
        binding.btnCancelSearchOrder.setOnClickListener(v -> hideSearchBar());

        binding.etSearchOrder.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (viewModel != null) {
                    viewModel.setSearchQuery(s.toString().trim());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.rvOrderList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, new ArrayList<>());
        binding.rvOrderList.setAdapter(adapter);
    }

    private void setupViewModel(String email, String role) {
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        binding.pbOrderList.setVisibility(View.VISIBLE);
        viewModel.init(email, role);
        
        viewModel.getFilteredOrders().observe(this, orders -> {
            binding.pbOrderList.setVisibility(View.GONE);
            adapter.updateList(orders);
            
            if (orders == null || orders.isEmpty()) {
                binding.layoutEmptyOrder.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmptyOrder.setVisibility(View.GONE);
            }
        });
    }

    private void setupTabs() {
        binding.tabLayoutOrders.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (viewModel != null) {
                    viewModel.setFilter(tab.getText().toString());
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showSearchBar() {
        binding.layoutSearchOrder.setVisibility(View.VISIBLE);
        binding.etSearchOrder.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(binding.etSearchOrder, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearchBar() {
        binding.layoutSearchOrder.setVisibility(View.GONE);
        binding.etSearchOrder.setText("");
        if (viewModel != null) viewModel.setSearchQuery("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etSearchOrder.getWindowToken(), 0);
    }
}
