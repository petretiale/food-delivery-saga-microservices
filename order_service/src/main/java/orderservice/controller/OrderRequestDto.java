package orderservice.controller;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import orderservice.model.OrderStatus;

public class OrderRequestDto {
    private String orderID;
    private String customerName;
    private String item;
    private double amount;
    private OrderStatus orderStatus;
}
