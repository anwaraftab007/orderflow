package com.orderflow.orderflow.repository;
import com.orderflow.orderflow.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.orderflow.orderflow.entity.OrderStatus;
import java.util.Collection;
import java.util.List;
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("update CustomerOrder o set o.status = :cancelled where o.id = :id and o.status in :eligible")
    int markCancelledIfEligible(@Param("id") Long id,
                                @Param("cancelled") OrderStatus cancelled,
                                @Param("eligible") Collection<OrderStatus> eligible);
}
