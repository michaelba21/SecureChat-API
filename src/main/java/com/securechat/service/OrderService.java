package com.securechat.service;

import com.securechat.dto.OrderDto;
import com.securechat.model.Order;
import com.securechat.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service// Spring annotation: creates a singleton service bean for dependency injection
public class OrderService {

	private final OrderRepository orderRepos;
// Constructor injection of OrderRepository (Spring provides the repository instance)
	public OrderService(OrderRepository repos) {
		this.orderRepos = repos;
	}
// Create a new order from DTO and return the generated order ID
	public int createOrder(OrderDto newOrderDto) {
		Order o = new Order(newOrderDto.productname, newOrderDto.unitprice, newOrderDto.quantity);

		o = orderRepos.save(o);

		return o.getOrderid();
	}
// Create a new order from DTO and return the generated order ID
	public List<OrderDto> getOrders() {
		List<OrderDto> orderDtoList = new ArrayList<>();
		for (Order o : orderRepos.findAll()) {
			orderDtoList.add(transferToDto(o));
		}
		return orderDtoList;
	}
	// Retrieve a single order by ID, throw 404 if not found
	public OrderDto getOrder(int orderid) {
		Order o = orderRepos.findById(orderid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		return transferToDto(o);
	}
	// Calculate total amount for an order by ID, throw 404 if order not found
	public double getAmount(int orderid) {
		Order o = orderRepos.findById(orderid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		return o.calculateAmount();
	}
// Private helper method: converts Order entity to OrderDto (Data Transfer Object)
	private static OrderDto transferToDto(Order o) {
		OrderDto odto = new OrderDto();
		odto.orderid = o.getOrderid();
		odto.productname = o.getProductname();
		odto.unitprice = o.getUnitprice();
		odto.quantity = o.getQuantity();
		return odto;
	}
}
