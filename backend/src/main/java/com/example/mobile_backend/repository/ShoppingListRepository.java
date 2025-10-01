package com.example.mobile_backend.repository;

import com.example.mobile_backend.entity.ShoppingList;
import com.example.mobile_backend.entity.ItemCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    @Query("SELECT new com.example.mobile_backend.entity.ItemCount(i.name, COUNT(i)) " +
           "FROM ShoppingList sl JOIN sl.items i GROUP BY i.name ORDER BY COUNT(i) DESC")
    List<ItemCount> findPopularItems(@Param("limit") int limit);
}
