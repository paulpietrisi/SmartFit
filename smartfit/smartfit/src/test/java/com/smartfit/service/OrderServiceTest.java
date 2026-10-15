package com.smartfit.service;

import com.smartfit.dto.CartItemDto;
import com.smartfit.dto.CheckoutFormDto;
import com.smartfit.entity.Order;
import com.smartfit.entity.Product;
import com.smartfit.entity.User;
import com.smartfit.repository.OrderRepository;
import com.smartfit.repository.ProductRepository;
import com.smartfit.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Teste creare comenzi")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private CheckoutFormDto formDto;
    private CartItemDto cartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@smartfit.ro");
        testUser.setFitPoints(500); // 500 XP → discount 5+5=10%

        testProduct = new Product();
        testProduct.setId(10L);
        testProduct.setName("Whey Protein Gold 2kg");
        testProduct.setPrice(149.0);
        testProduct.setStockQuantity(20);

        formDto = new CheckoutFormDto();
        formDto.setFirstName("Paul");
        formDto.setLastName("Pietrisi");
        formDto.setEmail("test@smartfit.ro");
        formDto.setPhone("0721234567");
        formDto.setAddress("Str. Mihai Eminescu 12");
        formDto.setCity("Galati");
        formDto.setCounty("Galati");
        formDto.setPostalCode("800000");

        cartItem = new CartItemDto();
        cartItem.setId(10L);
        cartItem.setQty(2);
    }

    @Test
    @DisplayName("createOrder – status PENDING și total corect cu discount și transport")
    void createOrder_calculeazaTotalCorect() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                testUser, List.of(cartItem), formDto, "CARD", null);

        // Subtotal = 2 * 149 = 298
        // Discount = 10% → 29.8
        // Transport: 0 (subtotal >= 200)
        // Total = 298 - 29.8 + 0 = 268.2
        assertEquals(268.2, order.getTotalPrice(), 0.01);
        assertEquals("PENDING", order.getStatus());
        assertEquals(10, order.getPointsUsed()); // discountPct = 10
    }

    @Test
    @DisplayName("createOrder scade stocul corect")
    void createOrder_scadeStocul() {
        int stocInitial = 20;
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(testUser, List.of(cartItem), formDto, "CARD", null);

        assertEquals(stocInitial - cartItem.getQty(), testProduct.getStockQuantity());
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("createOrder – adresa de livrare formatată corect")
    void createOrder_adresaCorecta() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                testUser, List.of(cartItem), formDto, "CARD", null);

        String expected = "Str. Mihai Eminescu 12, Galati, Galati 800000";
        assertEquals(expected, order.getShippingAddress());
    }

    @Test
    @DisplayName("createOrder – acordă XP utilizatorului (subtotal)")
    void createOrder_acordaXp() {
        int xpInitial = testUser.getFitPoints(); // 500
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // Verificăm că XP a crescut cu subtotal (298)
            assertEquals(xpInitial + 298, u.getFitPoints());
            return u;
        });

        orderService.createOrder(testUser, List.of(cartItem), formDto, "CARD", null);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("createOrder – două produse diferite, total corect")
    void createOrder_douaProduse() {
        Product produs2 = new Product();
        produs2.setId(11L);
        produs2.setName("Creatina 500g");
        produs2.setPrice(89.0);
        produs2.setStockQuantity(15);

        CartItemDto item2 = new CartItemDto();
        item2.setId(11L);
        item2.setQty(1);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(productRepository.findById(11L)).thenReturn(Optional.of(produs2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                testUser, Arrays.asList(cartItem, item2), formDto, "CARD", null);

        // Subtotal = 2*149 + 1*89 = 387
        // Discount 10% → 38.7
        // Transport = 0 (>=200)
        // Total = 387 - 38.7 = 348.3
        assertEquals(348.3, order.getTotalPrice(), 0.01);
    }
}