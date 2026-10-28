package orderservice.dto;

import orderservice.model.OrderStatus;

public record OrderResponse(String orderID, String customerName, String item, Double amount, OrderStatus status) {
    
}
