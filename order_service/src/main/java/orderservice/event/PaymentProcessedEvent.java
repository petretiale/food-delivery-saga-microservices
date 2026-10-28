package orderservice.event;

public record PaymentProcessedEvent(String orderID, boolean success, String reason, String customerName, double amount) {

}
