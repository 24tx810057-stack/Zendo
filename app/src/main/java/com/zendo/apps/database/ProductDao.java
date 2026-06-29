package com.zendo.apps.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT * FROM products")
    List<ProductEntity> getAllProducts();

    @Query("SELECT * FROM products WHERE category = :category")
    List<ProductEntity> getProductsByCategory(String category);

    @Query("SELECT * FROM products WHERE brand = :brand")
    List<ProductEntity> getProductsByBrand(String brand);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Query("DELETE FROM products")
    void deleteAll();
}
