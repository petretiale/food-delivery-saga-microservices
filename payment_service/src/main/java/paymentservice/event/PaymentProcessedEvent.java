package paymentservice.event;

public class PaymentProcessedEvent {

    private String orderID;
    private boolean success;
    private String reason;
    private String customerName;
    private double amount;


    public PaymentProcessedEvent() {
    }


    public PaymentProcessedEvent(String orderID, boolean success, String customerName, String reason, double amount) {
        this.orderID = orderID;
        this.amount = amount;
        this.customerName = customerName;
        this.reason = reason;
        this.success = success;
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

    public void setOrderID(String orderID) {
        this.orderID = orderID;
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