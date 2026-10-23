package orderservice.model;


import jakarta.persistence.*;

@Entity // 1. Dice a Spring che questa classe rappresenta una tabella del DB
@Table(name = "orders") // 2. Il nome della tabella in PostgreSQL sarà "orders"
public class Order {

    @Id // 3. Dice che questo campo è la CHIAVE PRIMARIA (Primary Key)
    private String orderID;
    private String customerName;
    private String item;
    private double amount;

    @Enumerated(EnumType.STRING) // 4. Salva l'enum nel DB come testo (es. "PENDING") anziché come numero (0, 1, 2)
    private OrderStatus orderStatus;

    public Order() {
    }

    public Order(String orderID, String customerName, String item, double amount, OrderStatus orderStatus) {
        this.orderID = orderID;
        this.customerName = customerName;
        this.item = item;
        this.amount = amount;
        this.orderStatus = orderStatus;
    }

    public String getOrderID() {
        return orderID;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getItem() {
        return item;
    }

    public double getAmount() {
        return amount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }
}
