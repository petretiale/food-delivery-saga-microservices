package orderservice.event;

public class PaymentProcessedEvent {

    private String orderID;
    private boolean success;
    private String reason;

    public PaymentProcessedEvent() {
    }

    public PaymentProcessedEvent(String orderID, boolean success, String reason) {
        this.orderID = orderID;
        this.success = success;
        this.reason = reason;
    }

    public String getOrderID() {
        return orderID;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
