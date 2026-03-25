package com.securechat.repository;

import com.securechat.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
// Extends JpaRepository<Order, Integer> where:
// - Order: Entity type this repository manages
// - Integer: Type of the entity's primary key (orderid is int/Integer)
public interface OrderRepository extends JpaRepository<Order, Integer> {
}
