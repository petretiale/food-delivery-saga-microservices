package paymentservice.event;

public class DeliveryProcessedEvent {

    private String orderID;
    private boolean riderAssigned;
    private String reason;
    private double amount;

    public DeliveryProcessedEvent() {
    }

    public DeliveryProcessedEvent(String orderID, boolean riderAssigned, String reason, double amount) {
        this.orderID = orderID;
        this.riderAssigned = riderAssigned;
        this.reason = reason;
        this.amount = amount;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public boolean isRiderAssigned() {
        return riderAssigned;
    }

    public void setRiderAssigned(boolean riderAssigned) {
        this.riderAssigned = riderAssigned;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
