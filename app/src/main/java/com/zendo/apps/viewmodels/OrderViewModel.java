package com.zendo.apps.viewmodels;

import com.zendo.apps.data.repositories.OrderRepository;

import com.zendo.apps.data.models.CartItem;

import com.zendo.apps.data.models.Order;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderViewModel extends ViewModel {
    private OrderRepository repository = new OrderRepository();
    private MutableLiveData<UserParams> userParams = new MutableLiveData<>();
    private MutableLiveData<StatusParams> statusParams = new MutableLiveData<>();
    
    private MutableLiveData<String> filterStatus = new MutableLiveData<>("Tất cả");
    private MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private MediatorLiveData<List<Order>> filteredOrders = new MediatorLiveData<>();

    public LiveData<List<Order>> orders = Transformations.switchMap(userParams, params -> 
        repository.getOrders(params.email, params.role)
    );

    public LiveData<List<Order>> ordersByStatus = Transformations.switchMap(statusParams, params -> 
        repository.getOrdersByStatus(params.status, params.start, params.end)
    );

    public OrderViewModel() {
        filteredOrders.addSource(orders, this::applyFilters);
        filteredOrders.addSource(filterStatus, status -> applyFilters(orders.getValue()));
        filteredOrders.addSource(searchQuery, query -> applyFilters(orders.getValue()));
    }

    public void init(String email, String role) {
        setUser(email, role);
    }

    public void setUser(String email, String role) {
        userParams.setValue(new UserParams(email, role));
    }

    public void setFilter(String status) {
        filterStatus.setValue(status);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<Order> getOrderDetail(String orderId) {
        return repository.getOrderById(orderId);
    }

    public LiveData<List<Order>> getFilteredOrders() {
        return filteredOrders;
    }

    private void applyFilters(List<Order> orderList) {
        if (orderList == null) {
            filteredOrders.setValue(null);
            return;
        }

        String status = filterStatus.getValue();
        String query = searchQuery.getValue();

        List<Order> filtered = new ArrayList<>();
        for (Order order : orderList) {
            boolean matchesStatus = "Tất cả".equals(status) || status.equals(order.getStatus());
            
            boolean matchesQuery = true;
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                boolean idMatches = order.getId() != null && order.getId().toLowerCase().contains(lowerQuery);
                boolean productMatches = false;
                if (order.getItems() != null) {
                    for (CartItem item : order.getItems()) {
                        if (item.getProductName() != null && item.getProductName().toLowerCase().contains(lowerQuery)) {
                            productMatches = true;
                            break;
                        }
                    }
                }
                matchesQuery = idMatches || productMatches;
            }

            if (matchesStatus && matchesQuery) {
                filtered.add(order);
            }
        }

        // Thực hiện sắp xếp theo ưu tiên: Đang xử lý lên đầu
        java.util.Collections.sort(filtered, (o1, o2) -> {
            int p1 = getStatusPriority(o1.getStatus());
            int p2 = getStatusPriority(o2.getStatus());
            if (p1 != p2) return Integer.compare(p1, p2);

            // Nếu cùng độ ưu tiên, sắp xếp theo thời gian mới nhất lên đầu
            long t1 = o1.getTimestamp() != null ? o1.getTimestamp().getTime() : 0;
            long t2 = o2.getTimestamp() != null ? o2.getTimestamp().getTime() : 0;
            return Long.compare(t2, t1);
        });

        filteredOrders.setValue(filtered);
    }

    private int getStatusPriority(String status) {
        if (status == null) return 99;
        switch (status) {
            case "Chờ xác nhận":
            case "Chờ lấy hàng":
                return 1;
            case "Đang giao":
                return 2;
            case "Yêu cầu hủy":
                return 3;
            case "Đã giao":
            case "Hoàn thành":
                return 4;
            case "Đã hủy":
                return 5;
            default:
                return 99;
        }
    }

    public void setStatusFilter(String status, Date start, Date end) {
        statusParams.setValue(new StatusParams(status, start, end));
    }

    public void updateStatus(String orderId, String status, OrderRepository.OnCompleteListener listener) {
        repository.updateOrderStatus(orderId, status, listener);
    }

    private static class UserParams {
        String email;
        String role;

        UserParams(String email, String role) {
            this.email = email;
            this.role = role;
        }
    }

    private static class StatusParams {
        String status;
        Date start;
        Date end;

        StatusParams(String status, Date start, Date end) {
            this.status = status;
            this.start = start;
            this.end = end;
        }
    }
}



