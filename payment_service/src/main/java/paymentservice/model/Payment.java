package paymentservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String orderID;
    private String customerName;
    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String reason;

    public Payment() {
    }

    public Payment(String orderID, String customerName, double amount, PaymentStatus status, String reason) {
        this.orderID = orderID;
        this.customerName = customerName;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
