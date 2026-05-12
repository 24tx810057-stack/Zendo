package com.example.buoi1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecommendationEngine {

    public static double calculateSimilarity(Set<String> a, Set<String> b) {
        if (a == null || b == null || (a.isEmpty() && b.isEmpty())) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private static Set<String> getEffectiveTags(Product p) {
        Set<String> tags = new HashSet<>(p.getTags());
        // Nếu không có tags, dùng Brand và Category làm fallback
        if (tags.isEmpty()) {
            if (p.getBrand() != null) tags.add(p.getBrand().toLowerCase());
            if (p.getCategory() != null) tags.add(p.getCategory().toLowerCase());
        }
        return tags;
    }

    public static List<Product> recommendSimilarProducts(
            Product currentProduct,
            List<Product> allProducts,
            int limit
    ) {
        if (currentProduct == null || allProducts == null) return new ArrayList<>();

        Set<String> currentTags = getEffectiveTags(currentProduct);
        List<Product> recommended = new ArrayList<>(allProducts);

        recommended.removeIf(p -> p.getId().equals(currentProduct.getId()));

        Collections.sort(recommended, (p1, p2) -> {
            double sim1 = calculateSimilarity(currentTags, getEffectiveTags(p1));
            double sim2 = calculateSimilarity(currentTags, getEffectiveTags(p2));
            return Double.compare(sim2, sim1);
        });

        if (recommended.size() > limit) {
            return recommended.subList(0, limit);
        }
        return recommended;
    }

    public static List<Product> recommendForUser(
            Set<String> purchaseTags,
            Set<String> cartTags,
            Set<String> likedTags,
            List<String> searchHistory,
            List<Product> allProducts,
            int limit
    ) {
        if (allProducts == null) return new ArrayList<>();
        List<Product> recommended = new ArrayList<>(allProducts);

        Collections.sort(recommended, (p1, p2) -> {
            double score1 = calculateWeightedScore(p1, purchaseTags, cartTags, likedTags, searchHistory);
            double score2 = calculateWeightedScore(p2, purchaseTags, cartTags, likedTags, searchHistory);
            
            if (score1 == score2) {
                return Integer.compare(p2.getSoldCount(), p1.getSoldCount());
            }
            return Double.compare(score2, score1);
        });

        if (recommended.size() > limit) return recommended.subList(0, limit);
        return recommended;
    }

    private static double calculateWeightedScore(
            Product p,
            Set<String> purchaseTags,
            Set<String> cartTags,
            Set<String> likedTags,
            List<String> searchHistory
    ) {
        Set<String> productTags = getEffectiveTags(p);
        String name = p.getName().toLowerCase();
        
        double score = 0;

        // 1. Ưu tiên Tìm kiếm (Trọng số 1.2)
        if (searchHistory != null) {
            for (String query : searchHistory) {
                String q = query.toLowerCase().trim();
                if (q.isEmpty()) continue;

                // Khớp chính xác hoặc chứa tên (Ưu tiên cao nhất)
                if (name.contains(q)) {
                    score += 1.2;
                } 
                // Gợi ý liên quan: Khớp với Thương hiệu hoặc Danh mục
                else if ((p.getBrand() != null && p.getBrand().toLowerCase().contains(q)) || 
                         (p.getCategory() != null && p.getCategory().toLowerCase().contains(q))) {
                    score += 1.0; 
                }
                
                // Khớp với Tags
                for (String tag : productTags) {
                    if (tag.contains(q) || q.contains(tag)) {
                        score += 0.6;
                        break; // Tránh cộng điểm nhiều lần cho cùng 1 query trên nhiều tags
                    }
                }
            }
        }

        // 2. Lịch sử Đơn hàng (Trọng số 1.0)
        score += calculateSimilarity(productTags, purchaseTags) * 1.0;

        // 3. Giỏ hàng (Trọng số 0.8)
        score += calculateSimilarity(productTags, cartTags) * 0.8;

        // 4. Yêu thích (Trọng số 0.6)
        score += calculateSimilarity(productTags, likedTags) * 0.6;

        return score;
    }
}
