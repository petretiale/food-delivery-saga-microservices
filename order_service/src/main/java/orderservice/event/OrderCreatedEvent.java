package orderservice.event;

public class OrderCreatedEvent {

    private String orderID;
    private String customerName;
    private double amount;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(String orderID, String customerName, double amount) {
        this.orderID = orderID;
        this.customerName = customerName;
        this.amount = amount;
    }

    public String getOrderID() {
        return orderID;
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

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
