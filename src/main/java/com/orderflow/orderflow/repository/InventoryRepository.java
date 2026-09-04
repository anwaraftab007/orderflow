package com.orderflow.orderflow.repository;
import com.orderflow.orderflow.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    @Modifying
    @Query(value = """
            update inventory
            set quantity = quantity - :amount, updated_at = CURRENT_TIMESTAMP
            where product_id = :productId and quantity >= :amount
            """, nativeQuery = true)
    int decreaseStock(@Param("productId") Long productId, @Param("amount") int amount);

    @Modifying
    @Query(value = """
            update inventory
            set quantity = quantity + :amount, updated_at = CURRENT_TIMESTAMP
            where product_id = :productId
            """, nativeQuery = true)
    int increaseStock(@Param("productId") Long productId, @Param("amount") int amount);
}
