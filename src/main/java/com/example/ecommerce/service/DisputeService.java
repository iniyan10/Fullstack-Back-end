package com.example.ecommerce.service;

import com.example.ecommerce.dto.DisputeRequest;
import com.example.ecommerce.dto.DisputeResponse;
import com.example.ecommerce.dto.DtoMapper;
import com.example.ecommerce.entity.Dispute;
import com.example.ecommerce.entity.DisputeStatus;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.ApiException;
import com.example.ecommerce.repository.DisputeRepository;
import com.example.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;

    public DisputeService(DisputeRepository disputeRepository, OrderRepository orderRepository, UserService userService) {
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    @Transactional
    public DisputeResponse create(String email, Long orderId, DisputeRequest request) {
        User user = userService.getByEmail(email);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        // Customers can only raise disputes on their own orders (reported as 404 to avoid leaking ids).
        if (!order.getUser().getId().equals(user.getId())) {
            throw ApiException.notFound("Order not found");
        }
        Dispute dispute = new Dispute();
        dispute.setOrder(order);
        dispute.setCustomer(user);
        dispute.setSubject(request.subject().trim());
        dispute.setDescription(request.description().trim());
        return DtoMapper.toDisputeDto(disputeRepository.save(dispute));
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> mine(String email) {
        User user = userService.getByEmail(email);
        return disputeRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(DtoMapper::toDisputeDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> all() {
        return disputeRepository.findAllByOrderByCreatedAtDesc().stream().map(DtoMapper::toDisputeDto).toList();
    }

    @Transactional
    public DisputeResponse resolve(Long id, String resolution) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Dispute not found"));
        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw ApiException.conflict("Dispute is already resolved");
        }
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution.trim());
        dispute.setResolvedAt(Instant.now());
        return DtoMapper.toDisputeDto(disputeRepository.save(dispute));
    }
}
