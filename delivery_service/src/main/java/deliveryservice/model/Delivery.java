package deliveryservice.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    private String orderID;

    private String customerName;
    private boolean riderAssigned;
    private String riderName;
    private String status;

    public Delivery() {
    }

    public Delivery(String orderID, String customerName, boolean riderAssigned, String riderName, String status) {
        this.orderID = orderID;
        this.customerName = customerName;
        this.riderAssigned = riderAssigned;
        this.riderName = riderName;
        this.status = status;
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

    public boolean isRiderAssigned() {
        return riderAssigned;
    }

    public void setRiderAssigned(boolean riderAssigned) {
        this.riderAssigned = riderAssigned;
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
