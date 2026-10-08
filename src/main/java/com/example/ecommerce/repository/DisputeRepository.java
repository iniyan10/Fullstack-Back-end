package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Dispute;
import com.example.ecommerce.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findAllByOrderByCreatedAtDesc();
    List<Dispute> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    long countByStatus(DisputeStatus status);
}
