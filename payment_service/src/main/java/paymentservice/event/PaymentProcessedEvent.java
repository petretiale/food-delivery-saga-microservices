package paymentservice.event;

public record PaymentProcessedEvent(String orderID, boolean success, String customerName, String reason, double amount) {

}