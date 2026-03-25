package com.securechat.dto;

import java.util.Objects;

/**
 * Simple DTO for order transport without validation (demo-style to mirror teacher example).
 */
public class OrderDto {

	public int orderid;
	public String productname;
	public double unitprice;
	public int quantity;

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		OrderDto orderDto = (OrderDto) o;
		return Double.compare(unitprice, orderDto.unitprice) == 0
				&& quantity == orderDto.quantity
				&& Objects.equals(productname, orderDto.productname);
	}

	@Override
	public int hashCode() {
		return Objects.hash(productname, unitprice, quantity);
	}
}
