package com.smartfit.service;

import com.smartfit.dto.CartItemDto;
import com.smartfit.dto.CheckoutFormDto;
import com.smartfit.entity.Order;
import com.smartfit.entity.OrderItem;
import com.smartfit.entity.Product;
import com.smartfit.entity.User;
import com.smartfit.repository.OrderRepository;
import com.smartfit.repository.ProductRepository;
import com.smartfit.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createOrder(User user,
                             List<CartItemDto> cartItems,
                             CheckoutFormDto form,
                             String paymentMethod,
                             String stripePaymentId) {

        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(paymentMethod);
        order.setStripePaymentId(stripePaymentId);
        order.setFullName(form.getFirstName() + " " + form.getLastName());
        order.setPhone(form.getPhone());
        order.setContactEmail(form.getEmail() != null ? form.getEmail() : user.getEmail());
        order.setShippingAddress(
                form.getAddress() + ", " + form.getCity() +
                        ", " + form.getCounty() + " " + form.getPostalCode()
        );
        order.setStatus("PENDING");

        double subtotal = 0;

        for (CartItemDto ci : cartItems) {
            Product product = productRepository.findById(ci.getId()).orElse(null);
            if (product == null) continue;

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(ci.getQty());
            item.setPriceAtPurchase(product.getPrice());
            order.getItems().add(item);

            subtotal += product.getPrice() * ci.getQty();

            // Scade stocul
            int newStock = Math.max(0, product.getStockQuantity() - ci.getQty());
            product.setStockQuantity(newStock);
            productRepository.save(product);
        }

        // Calculează discount XP
        int fitPts      = user.getFitPoints() != null ? user.getFitPoints() : 0;
        int discountPct = Math.min(25, 5 + fitPts / 100);
        double xpDisc   = subtotal * discountPct / 100.0;
        double shipping  = subtotal >= 200 ? 0 : 25;
        double total     = subtotal - xpDisc + shipping;

        order.setTotalPrice(Math.round(total * 100.0) / 100.0);
        order.setPointsUsed(discountPct);

        Order saved = orderRepository.save(order);

        // Acordă XP utilizatorului (1 XP per leu cheltuit)
        int earned = (int) subtotal;
        user.setFitPoints(fitPts + earned);
        userRepository.save(user);

        return saved;
    }

    public Order getOrderWithItems(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Comanda nu există"));
    }

    // 🔥 Metoda nouă pentru a obține comenzile unui utilizator
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}