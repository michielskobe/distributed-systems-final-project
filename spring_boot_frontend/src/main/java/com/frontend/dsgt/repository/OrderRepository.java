package com.frontend.dsgt.repository;

import com.frontend.dsgt.model.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findByUserEmail(String userEmail, Pageable pageable);
}
