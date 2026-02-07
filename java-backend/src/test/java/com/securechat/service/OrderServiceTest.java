

package com.securechat.service;

import com.securechat.dto.OrderDto;
import com.securechat.model.Order;
import com.securechat.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito for mocking dependencies
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;  // Mock the repository layer

    @InjectMocks
    private OrderService orderService;  // Service under test with injected mocks

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        // Initialize test Order entities before each test
        order1 = new Order("Banana", 2.0, 3);  // Create order with product details
        order1.setOrderid(1);  // Set ID separately (assuming orderid is not in constructor)

        order2 = new Order("Apple", 1.5, 5);
        order2.setOrderid(2);
    }

    // ────────────────────────────────────────────────────────────────
    // createOrder
    // ────────────────────────────────────────────────────────────────

    @Test
    void createOrder_success() {
        // Test successful order creation
        OrderDto dto = new OrderDto();
        dto.productname = "Banana";
        dto.unitprice = 2.0;
        dto.quantity = 3;  // Test data matches order1 setup

        Order savedOrder = new Order("Banana", 2.0, 3);
        savedOrder.setOrderid(100);  // Mock ID returned by repository

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);  // Mock repository save

        int result = orderService.createOrder(dto);  // Call service method

        assertThat(result).isEqualTo(100);  // Verify returned order ID
        verify(orderRepository).save(any(Order.class));  // Verify repository interaction
    }

    // ────────────────────────────────────────────────────────────────
    // getOrders
    // ────────────────────────────────────────────────────────────────

    @Test
    void getOrders_emptyList() {
        // Test edge case: no orders in repository
        when(orderRepository.findAll()).thenReturn(List.of());  // Mock empty list

        List<OrderDto> result = orderService.getOrders();  // Call service

        assertThat(result).isEmpty(); 
        verify(orderRepository).findAll();  // Verify repository call
    }

    @Test
    void getOrders_withItems_callsTransferToDto() {
        // Test normal case: multiple orders exist
        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));  // Mock two orders

        List<OrderDto> result = orderService.getOrders();  // Call service

        assertThat(result).hasSize(2);  // Verify correct number of DTOs
        // Verify DTO conversion preserves data
        assertThat(result.get(0).productname).isEqualTo("Banana"); 
        assertThat(result.get(1).productname).isEqualTo("Apple");   // Second order
        verify(orderRepository).findAll();  // Verify repository call
    }

    // ────────────────────────────────────────────────────────────────
    // getOrder
    // ────────────────────────────────────────────────────────────────

    @Test
    void getOrder_found_returnsDto() {
        // Test successful retrieval of existing order
        when(orderRepository.findById(1)).thenReturn(Optional.of(order1));  // Mock found order

        OrderDto result = orderService.getOrder(1);  // Call service with ID 1

        // Verify all DTO fields are correctly populated from entity
        assertThat(result.orderid).isEqualTo(1);  
        assertThat(result.productname).isEqualTo("Banana");  // Product name preserved
        assertThat(result.unitprice).isEqualTo(2.0);  
        assertThat(result.quantity).isEqualTo(3);  // Quantity preserved
    }

    @Test
    void getOrder_notFound_throwsNotFound() {
        // Test error case: order doesn't exist
        when(orderRepository.findById(999)).thenReturn(Optional.empty());  // Mock not found

        // Verify exception is thrown with correct details
        assertThatThrownBy(() -> orderService.getOrder(999))  // Should throw when order not found
                .isInstanceOf(ResponseStatusException.class)  // Correct exception type
                .hasMessageContaining("Order not found")  // Error message contains expected text
                .extracting("status")  // Extract status field from exception
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);  // Verify HTTP 404 status
    }

    // ────────────────────────────────────────────────────────────────
    // getAmount
    // ────────────────────────────────────────────────────────────────

    @Test
    void getAmount_found_returnsCalculatedAmount() {
        // Test order amount calculation for existing order
        when(orderRepository.findById(1)).thenReturn(Optional.of(order1));  // Mock found order

        // Assume Order.calculateAmount() = unitprice * quantity
        double result = orderService.getAmount(1);  // Call amount calculation service

        assertThat(result).isEqualTo(6.0); // 2.0 * 3 = 6.0 (unitprice * quantity)
    }

    @Test
    void getAmount_notFound_throwsNotFound() {
        // Test error case: amount calculation for non-existent order
        when(orderRepository.findById(999)).thenReturn(Optional.empty());  // Mock not found

        // Verify exception is thrown
        assertThatThrownBy(() -> orderService.getAmount(999))  // Should throw
                .isInstanceOf(ResponseStatusException.class)  // Correct exception type
                .hasMessageContaining("Order not found");  // Appropriate error message
    }
}