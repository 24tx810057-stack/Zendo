package com.zendo.apps.utils;

import java.util.HashMap;
import java.util.Map;

public class AutoResponseEngine {

    private static final Map<String, String> responses = new HashMap<>();

    static {
        responses.put("còn hàng", "Dạ sản phẩm này hiện vẫn còn hàng ạ. Bạn đặt sớm để shop đóng gói luôn nhé!");
        responses.put("hết hàng", "Dạ sản phẩm này hiện vẫn còn hàng ạ. Bạn đặt sớm để shop đóng gói luôn nhé!");
        responses.put("địa chỉ", "Shop mình ở TP. Hồ Chí Minh, bạn có thể xem địa chỉ cụ thể trong phần thông tin Shop ạ.");
        responses.put("ở đâu", "Shop mình ở TP. Hồ Chí Minh, bạn có thể xem địa chỉ cụ thể trong phần thông tin Shop ạ.");
        responses.put("giảm giá", "Bạn kiểm tra mục 'Voucher' của Shop để thu thập mã giảm giá mới nhất nhé!");
        responses.put("voucher", "Bạn kiểm tra mục 'Voucher' của Shop để thu thập mã giảm giá mới nhất nhé!");
        responses.put("bảo hành", "Sản phẩm được bảo hành chính hãng 12 tháng, bạn yên tâm mua sắm nhé!");
        responses.put("chào", "Zendo Shop chào bạn! Mình có thể giúp gì cho bạn về sản phẩm này không?");
        responses.put("hi", "Zendo Shop chào bạn! Mình có thể giúp gì cho bạn về sản phẩm này không?");
    }

    public static String getResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return null;
        }

        String lowerMessage = userMessage.toLowerCase().trim();

        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "Cảm ơn bạn đã quan tâm! Shop đã nhận được tin nhắn và sẽ phản hồi bạn sớm nhất có thể.";
    }

    public static String[] getQuickReplies() {
        return new String[]{
                "Sản phẩm còn hàng không?",
                "Shop ở đâu vậy?",
                "Có mã giảm giá không shop?",
                "Chính sách bảo hành thế nào?"
        };
    }
}
