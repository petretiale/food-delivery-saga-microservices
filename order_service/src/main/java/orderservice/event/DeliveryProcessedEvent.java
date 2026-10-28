package orderservice.event;

public record DeliveryProcessedEvent(String orderID, boolean riderAssigned, String reason, double amount) {

}
