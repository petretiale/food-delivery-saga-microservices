package paymentservice.event;

public record OrderReceivedEvent(String orderID, String customerName, double amount) {

}
