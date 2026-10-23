package orderservice.controller;

public class OrderResponseDto {
    private String customerName;
    private String item;
    private double amount;

    public OrderResponseDto(String customerName, String item, double amount) {
        this.customerName = customerName;
        this.item = item;
        this.amount = amount;
    }
}
