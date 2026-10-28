package orderservice.event;

public record OrderCreatedEvent(String orderID, String customerName, double amount) {

}