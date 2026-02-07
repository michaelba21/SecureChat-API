package com.securechat.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity// Marks this class as a JPA entity (maps to database table)
@Table(name = "orders")
public class Order {
	@Id// Marks this field as the primary key
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int orderid;

	private String productname;
	private double unitprice;
	private int quantity;

	public Order() {
		// Default constructor required by JPA
	}
// Parameterized constructor for creating new orders without ID (ID will be auto-generated)
	public Order(String prodname, double price, int quantity) {
		this.productname = prodname;
		this.unitprice = price;
		this.quantity = quantity;
	}
//getter and setter methods
	public int getOrderid() {
		return orderid;
	}

	public void setOrderid(int orderid) {
		this.orderid = orderid;
	}

	public String getProductname() {
		return productname;
	}

	public void setProductname(String productname) {
		this.productname = productname;
	}

	public double getUnitprice() {
		return unitprice;
	}

	public void setUnitprice(double unitprice) {
		this.unitprice = unitprice;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	// Business logic method: calculates total amount (quantity × unit price)
	public double calculateAmount() {
		return this.quantity * this.unitprice;
	}
}
