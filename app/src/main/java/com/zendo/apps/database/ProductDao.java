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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Query("DELETE FROM products")
    void deleteAll();
}
