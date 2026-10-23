package deliveryservice.event;

public class PaymentProcessedEvent {

    private String orderID;
    private boolean success;
    private String reason;
    private String customerName;
    private double amount;

    public PaymentProcessedEvent() {
    }

    public PaymentProcessedEvent(String orderID, boolean success, String reason, String customerName, double amount) {
        this.orderID = orderID;
        this.success = success;
        this.reason = reason;
        this.customerName = customerName;
        this.amount = amount;
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

    public String getCustomerName() {
        return customerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setOrderID(String orderId) {
        this.orderID = orderId;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
