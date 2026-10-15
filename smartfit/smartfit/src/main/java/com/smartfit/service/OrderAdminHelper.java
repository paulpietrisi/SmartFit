package com.smartfit.service;

import com.smartfit.entity.Order;
import com.smartfit.entity.User;
import com.smartfit.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Operații pe comenzi pentru panourile admin / angajat magazin:
 * căutare+filtrare, schimbare status (cu logare).
 */
@Service
public class OrderAdminHelper {

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "SHIPPED", "DELIVERED", "CANCELLED");

    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    public OrderAdminHelper(OrderRepository orderRepository, AuditLogService auditLogService) {
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
    }

    public List<Order> search(String status, String email, LocalDateTime from, LocalDateTime to,
                               Double minTotal, Double maxTotal) {
        return orderRepository.search(
                blank(status), blank(email), from, to, minTotal, maxTotal);
    }

    public Order getOrder(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Comanda cu ID " + id + " nu a fost găsită"));
    }

    public Order updateStatus(Long id, String newStatus, User actor, HttpServletRequest request) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Status invalid. Valori permise: " + VALID_STATUSES);
        }

        Order order = getOrder(id);
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        auditLogService.log(actor, "ORDER_STATUS_CHANGE",
                "Status comandă #ORD-" + id + " schimbat: " + oldStatus + " → " + newStatus,
                "orders", id,
                "UPDATE orders SET status='" + newStatus + "' WHERE id=" + id,
                request);

        return saved;
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
