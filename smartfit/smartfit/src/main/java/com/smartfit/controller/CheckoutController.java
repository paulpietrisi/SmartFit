package com.smartfit.controller;

import com.smartfit.dto.CartItemDto;
import com.smartfit.dto.CheckoutFormDto;
import com.smartfit.entity.Order;
import com.smartfit.entity.Product;
import com.smartfit.entity.User;
import com.smartfit.repository.ProductRepository;
import com.smartfit.repository.UserRepository;
import com.smartfit.service.EmailService;
import com.smartfit.service.OrderService;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class CheckoutController {

    private final OrderService orderService;
    private final EmailService emailService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    public CheckoutController(OrderService orderService,
                              EmailService emailService,
                              ProductRepository productRepository,
                              UserRepository userRepository) {
        this.orderService = orderService;
        this.emailService = emailService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // ── 1. JS trimite coșul → stocăm în sesiune ─────────────────────────
    @PostMapping("/checkout/cart")
    @ResponseBody
    public ResponseEntity<String> saveCart(@RequestBody List<CartItemDto> items,
                                           HttpSession session) {
        session.setAttribute("cartItems", new ArrayList<>(items));
        return ResponseEntity.ok("OK");
    }

    // ── 2. GET /checkout – afișăm pagina ─────────────────────────────────
    @GetMapping("/checkout")
    @SuppressWarnings("unchecked")
    public String checkoutPage(Model model, HttpSession session, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        List<CartItemDto> cartItems = (List<CartItemDto>) session.getAttribute("cartItems");
        if (cartItems == null || cartItems.isEmpty()) return "redirect:/magazin";

        User user = userRepository.findByEmail(auth.getName());

        // Construiește sumarul coșului
        List<Map<String, Object>> summary = new ArrayList<>();
        double subtotal = 0;

        for (CartItemDto ci : cartItems) {
            Product p = productRepository.findById(ci.getId()).orElse(null);
            if (p == null) continue;
            double lineTotal = p.getPrice() * ci.getQty();
            summary.add(Map.of(
                    "name",      p.getName(),
                    "icon",      p.getFallbackEmoji(),
                    "cat",       p.getCategoryName(),
                    "price",     p.getPrice(),
                    "qty",       ci.getQty(),
                    "lineTotal", lineTotal
            ));
            subtotal += lineTotal;
        }

        int fitPts      = user.getFitPoints() != null ? user.getFitPoints() : 0;
        int discountPct = Math.min(25, 5 + fitPts / 100);
        double xpDisc   = Math.round(subtotal * discountPct) / 100.0;
        double shipping  = subtotal >= 200 ? 0 : 25;
        double total     = subtotal - xpDisc + shipping;

        model.addAttribute("cartSummary",    summary);
        model.addAttribute("subtotal",       subtotal);
        model.addAttribute("xpDiscount",     xpDisc);
        model.addAttribute("discountPct",    discountPct);
        model.addAttribute("shipping",       shipping);
        model.addAttribute("total",          total);
        model.addAttribute("user",           user);
        model.addAttribute("userInitials",   String.valueOf(user.getFirstName().charAt(0)).toUpperCase());
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("stripeKey",      stripePublishableKey);

        return "checkout";
    }

    // ── 3a. Procesare CASH ────────────────────────────────────────────────
    @PostMapping("/checkout/process-cash")
    @SuppressWarnings("unchecked")
    public String processCash(@RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String email,
                              @RequestParam String phone,
                              @RequestParam String address,
                              @RequestParam String city,
                              @RequestParam String county,
                              @RequestParam String postalCode,
                              HttpSession session,
                              Authentication auth) {

        User user = userRepository.findByEmail(auth.getName());
        List<CartItemDto> cartItems = (List<CartItemDto>) session.getAttribute("cartItems");
        if (cartItems == null || cartItems.isEmpty()) return "redirect:/magazin";

        CheckoutFormDto form = new CheckoutFormDto();
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setEmail(email);
        form.setPhone(phone);
        form.setAddress(address);
        form.setCity(city);
        form.setCounty(county);
        form.setPostalCode(postalCode);

        Order order = orderService.createOrder(user, cartItems, form, "CASH", null);
        session.removeAttribute("cartItems");
        emailService.sendOrderConfirmation(order);

        return "redirect:/order-success/" + order.getId();
    }

    // ── 3b. Stripe – salvează datele de livrare + creează PaymentIntent ──
    @PostMapping("/checkout/stripe/prepare")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> stripePrepare(@RequestBody CheckoutFormDto form,
                                           HttpSession session,
                                           Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName());
            List<CartItemDto> cartItems = (List<CartItemDto>) session.getAttribute("cartItems");
            if (cartItems == null || cartItems.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Coș gol"));

            session.setAttribute("checkoutForm", form);

            // Calculează totalul în bani (1 RON = 100 bani)
            double subtotal = 0;
            for (CartItemDto ci : cartItems) {
                Product p = productRepository.findById(ci.getId()).orElse(null);
                if (p != null) subtotal += p.getPrice() * ci.getQty();
            }
            int fitPts      = user.getFitPoints() != null ? user.getFitPoints() : 0;
            int discountPct = Math.min(25, 5 + fitPts / 100);
            double xpDisc   = subtotal * discountPct / 100.0;
            double shipping  = subtotal >= 200 ? 0 : 25;
            double total     = subtotal - xpDisc + shipping;
            long amountBani  = Math.round(total * 100);

            Stripe.apiKey = stripeSecretKey;
            PaymentIntent intent = PaymentIntent.create(
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountBani)
                            .setCurrency("ron")
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true).build()
                            )
                            .putMetadata("userId", user.getId().toString())
                            .build()
            );

            session.setAttribute("stripeIntentId", intent.getId());
            return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 3c. Stripe – callback după plată reușită ──────────────────────────
    @GetMapping("/checkout/stripe/complete")
    @SuppressWarnings("unchecked")
    public String stripeComplete(@RequestParam("payment_intent") String paymentIntentId,
                                 @RequestParam("redirect_status") String redirectStatus,
                                 HttpSession session,
                                 Authentication auth,
                                 Model model) {
        if (!"succeeded".equals(redirectStatus)) {
            model.addAttribute("paymentError", "Plata a eșuat. Încearcă din nou.");
            return "redirect:/checkout?paymentError=1";
        }

        try {
            Stripe.apiKey = stripeSecretKey;
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if (!"succeeded".equals(intent.getStatus()))
                return "redirect:/checkout?paymentError=1";

            User user = userRepository.findByEmail(auth.getName());
            List<CartItemDto> cartItems = (List<CartItemDto>) session.getAttribute("cartItems");
            CheckoutFormDto form = (CheckoutFormDto) session.getAttribute("checkoutForm");

            if (cartItems == null || form == null) return "redirect:/magazin";

            Order order = orderService.createOrder(user, cartItems, form, "CARD", paymentIntentId);
            session.removeAttribute("cartItems");
            session.removeAttribute("checkoutForm");
            session.removeAttribute("stripeIntentId");

            emailService.sendOrderConfirmation(order);
            return "redirect:/order-success/" + order.getId();

        } catch (Exception e) {
            return "redirect:/checkout?paymentError=1";
        }
    }

    // ── 4. Pagina de succes ───────────────────────────────────────────────
    @GetMapping("/order-success/{id}")
    @Transactional(readOnly = true)   // ← ADAUGĂ
    public String orderSuccess(@PathVariable Long id, Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";

        Order order = orderService.getOrderWithItems(id);

        if (!order.getUser().getEmail().equals(auth.getName()))
            return "redirect:/magazin";

        model.addAttribute("order", order);
        model.addAttribute("user", order.getUser());
        model.addAttribute("userInitials",
                String.valueOf(order.getUser().getFirstName().charAt(0)).toUpperCase());
        model.addAttribute("isAuthenticated", true);

        return "order-success";
    }
}