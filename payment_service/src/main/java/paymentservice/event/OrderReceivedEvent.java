package paymentservice.event;

public class OrderReceivedEvent {

    private String orderID;
    private String customerName;
    private double amount;

    public OrderReceivedEvent() {
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

    public String getOrderID() {
        return orderID;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getAmount() {
        return amount;
    }
}
