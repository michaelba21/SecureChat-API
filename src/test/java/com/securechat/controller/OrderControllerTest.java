

package com.securechat.controller;

import com.securechat.dto.InvoiceDto;
import com.securechat.dto.OrderDto;
import com.securechat.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Enables Mockito annotations for dependency injection
@DisplayName("OrderController → Comprehensive Test Suite") 
class OrderControllerTest {

    @Mock
    private OrderService orderService;  // Mock the service layer to isolate controller testing

    @InjectMocks
    private OrderController orderController;  // Inject mocks into controller under test

    private OrderDto sampleOrderDto;
    private static final int ORDER_ID = 42;  // Test constant for consistent order ID
    private static final double SAMPLE_AMOUNT = 159.90;  // Test constant for amount validation

    @BeforeEach
    void setUp() {
        // Sample DTO initialization - actual fields would be set based on OrderDto structure
        sampleOrderDto = new OrderDto();
      

        // Setup ServletRequestAttributes for ServletUriComponentsBuilder.fromCurrentRequest()
        // This is necessary for testing URI building in createOrder endpoint
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/orders");  
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);  // Set thread-local request context
    }

    @AfterEach
    void tearDown() {
        // Clean up RequestContextHolder to avoid test pollution between tests
        RequestContextHolder.resetRequestAttributes();  
    }

    @Test
    @DisplayName("GET /api/orders → should return all orders")
    void shouldReturnAllOrders() {
        // arrange - prepare test data
        List<OrderDto> orders = List.of(
                createOrderDto(1),   // Test with different IDs
                createOrderDto(7),
                createOrderDto(19)
        );
        when(orderService.getOrders()).thenReturn(orders);  // Mock service response

        // act - call controller method
        ResponseEntity<List<OrderDto>> response = orderController.retrieveOrders();

        // assert - verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK); 
        assertThat(response.getBody()).isSameAs(orders);  // Verify same list returned
        verify(orderService).getOrders(); 
        verifyNoMoreInteractions(orderService);  // Ensure no unexpected service calls
    }

    @Test
    @DisplayName("GET /api/orders → empty list when no orders exist")
    void shouldReturnEmptyListWhenNoOrders() {
        // Test edge case: service returns empty list
        when(orderService.getOrders()).thenReturn(List.of());

        ResponseEntity<List<OrderDto>> response = orderController.retrieveOrders();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();  // Verify empty response body
    }

    @Test
    @DisplayName("POST /api/orders → should create order and return 201 with location")
    void shouldCreateOrderAndReturnCreatedWithLocation() {
        // arrange
        OrderDto input = createOrderDto(null);  // Input without ID (to be assigned)
        when(orderService.createOrder(any(OrderDto.class))).thenReturn(ORDER_ID);

        // act
        ResponseEntity<Integer> response = orderController.createOrder(input);

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);  
        assertThat(response.getBody()).isEqualTo(ORDER_ID);  // Verify returned order ID

        // Verify Location header for RESTful resource creation
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();  // Location header must be present
        assertThat(location.toString()).endsWith("/api/orders/" + ORDER_ID);  // Verify URI pattern

        verify(orderService).createOrder(input);  // Verify service call with correct input
        verifyNoMoreInteractions(orderService);  
    }

    @Test
    @DisplayName("GET /api/orders/{id} → should return existing order")
    void shouldReturnExistingOrder() {
        // Test retrieving single order by ID
        OrderDto expected = createOrderDto(ORDER_ID);
        when(orderService.getOrder(ORDER_ID)).thenReturn(expected);

        ResponseEntity<OrderDto> response = orderController.retrieveOrder(ORDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);  // Same instance from service
        verify(orderService).getOrder(ORDER_ID);  // Verify correct ID passed to service
    }

    @Test
    @DisplayName("GET /api/orders/{id}/invoice → should return invoice with calculated amount")
    void shouldReturnInvoiceWithAmount() {
        // Test invoice generation endpoint
        when(orderService.getAmount(ORDER_ID)).thenReturn(SAMPLE_AMOUNT);

        ResponseEntity<InvoiceDto> response = orderController.getAmount(ORDER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InvoiceDto invoice = response.getBody();
        assertThat(invoice).isNotNull();  // Invoice should not be null
        assertThat(invoice.orderid).isEqualTo(ORDER_ID);  // Verify order ID in response
        assertThat(invoice.amount).isEqualTo(SAMPLE_AMOUNT); 

        verify(orderService).getAmount(ORDER_ID);  // Verify service call
        verifyNoMoreInteractions(orderService);
    }

    // ────────────────────────────────────────────────
    //  Helper methods
    // ────────────────────────────────────────────────

    private OrderDto createOrderDto(Integer id) {
        // Factory method for creating test OrderDto objects
        // Note: This method needs actual field assignments based on OrderDto structure
        OrderDto dto = new OrderDto();
        
        return dto;  // Currently returns empty DTO - needs implementation
    }

    // Optional: if you want to verify the exact URI construction more precisely
    @Test
    @DisplayName("POST → verifies URI construction logic (path appended correctly)")
    void createOrder_locationShouldContainCorrectPath() {
        // Additional test focusing specifically on URI construction
        when(orderService.createOrder(any())).thenReturn(1001);  // Different ID for variation

        ResponseEntity<Integer> response = orderController.createOrder(new OrderDto());

        URI location = response.getHeaders().getLocation();
        assertThat(location.toString())
                .matches(".*/api/orders/1001$");  // Regex match for URI pattern validation
    }
}