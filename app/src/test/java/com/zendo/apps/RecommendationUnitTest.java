package com.zendo.apps;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecommendationUnitTest {

    @Test
    public void testJaccardSimilarity() {
        Set<String> setA = new HashSet<>(Arrays.asList("apple", "iphone", "5g"));
        Set<String> setB = new HashSet<>(Arrays.asList("apple", "ipad", "wifi"));
        
        // Giao: {apple} (1)
        // Hợp: {apple, iphone, 5g, ipad, wifi} (5)
        // Similarity: 1/5 = 0.2
        
        double similarity = RecommendationEngine.calculateSimilarity(setA, setB);
        assertEquals(0.2, similarity, 0.001);
    }

    @Test
    public void testJaccardSimilarityIdentical() {
        Set<String> setA = new HashSet<>(Arrays.asList("tea", "milk", "sweet"));
        Set<String> setB = new HashSet<>(Arrays.asList("tea", "milk", "sweet"));
        
        double similarity = RecommendationEngine.calculateSimilarity(setA, setB);
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    public void testJaccardSimilarityNoOverlap() {
        Set<String> setA = new HashSet<>(Arrays.asList("coffee", "bitter"));
        Set<String> setB = new HashSet<>(Arrays.asList("tea", "sweet"));
        
        double similarity = RecommendationEngine.calculateSimilarity(setA, setB);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    public void testRecommendForUserWithSearch() {
        Product p1 = new Product();
        p1.setId("101");
        p1.setName("Samsung Galaxy S23");
        p1.setBrand("Samsung");
        p1.setTags(Arrays.asList("samsung", "android"));

        Product p2 = new Product();
        p2.setId("102");
        p2.setName("iPhone 15");
        p2.setBrand("Apple");
        p2.setTags(Arrays.asList("apple", "ios"));

        List<Product> all = Arrays.asList(p1, p2);
        
        // Giả sử user trước đây mua Samsung (purchaseTags có 'samsung')
        Set<String> purchaseTags = new HashSet<>(Arrays.asList("samsung"));
        Set<String> emptySet = new HashSet<>();
        
        // Nhưng hiện tại đang Search 'iphone'
        List<String> searchHistory = Arrays.asList("iphone");

        List<Product> recommended = RecommendationEngine.recommendForUser(
                purchaseTags, emptySet, emptySet, searchHistory, all, 2);

        // iPhone phải lên đầu vì Search có trọng số cao hơn Mua hàng cũ
        assertEquals("102", recommended.get(0).getId());
    }

    @Test
    public void testRelatedSuggestionByBrand() {
        Product p1 = new Product();
        p1.setId("101");
        p1.setName("MacBook Pro");
        p1.setBrand("Apple");
        p1.setCategory("Laptop");

        Product p2 = new Product();
        p2.setId("102");
        p2.setName("Surface Laptop");
        p2.setBrand("Microsoft");
        p2.setCategory("Laptop");

        List<Product> all = Arrays.asList(p1, p2);
        
        Set<String> emptySet = new HashSet<>();
        // User search 'iphone' -> related to 'Apple' brand products
        List<String> searchHistory = Arrays.asList("apple");

        List<Product> recommended = RecommendationEngine.recommendForUser(
                emptySet, emptySet, emptySet, searchHistory, all, 2);

        // MacBook phải lên đầu vì Brand là Apple khớp với search query
        assertEquals("101", recommended.get(0).getId());
    }
}
