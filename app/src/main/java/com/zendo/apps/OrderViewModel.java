package com.zendo.apps;

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
        filteredOrders.setValue(filtered);
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
