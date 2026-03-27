package com.securechat.controller;

import com.securechat.dto.InvoiceDto;
import com.securechat.dto.OrderDto;
import com.securechat.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")// Base path for all order-related endpoints
public class OrderController {
	private final OrderService service;
// Constructor injection for OrderService
	public OrderController(OrderService service) {
		this.service = service;
	}
// GET endpoint to retrieve all orders
	@GetMapping
	public ResponseEntity<List<OrderDto>> retrieveOrders() {
		return ResponseEntity.ok(service.getOrders());
	}
	// POST endpoint to create a new order
	@PostMapping
	public ResponseEntity<Integer> createOrder(@RequestBody OrderDto newOrderDto) {
		int orderid = service.createOrder(newOrderDto);
		// Build Location header URI for the newly created resource
		URI uri = URI.create(
				ServletUriComponentsBuilder
						.fromCurrentRequest()
						.path("/" + orderid).toUriString());
// Return 201 Created with Location header and order ID in response body
		return ResponseEntity.created(uri).body(orderid);
	}

	// GET endpoint to get invoice/amount for a specific order
	@GetMapping("/{id}")
	public ResponseEntity<OrderDto> retrieveOrder(@PathVariable int id) {
		OrderDto odto = service.getOrder(id);
		return ResponseEntity.ok(odto);
	}
// GET endpoint to get invoice/amount for a specific order
	@GetMapping("/{id}/invoice")
	public ResponseEntity<InvoiceDto> getAmount(@PathVariable int id) {
		InvoiceDto invoiceDto = new InvoiceDto();
		invoiceDto.orderid = id;
		invoiceDto.amount = service.getAmount(id);// Calculate amount via service
		return ResponseEntity.ok(invoiceDto);
	}
}