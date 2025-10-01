package com.example.mobile_backend.dto;

public class ProductPriceDTO {
    private String name;
    private String price; // Оставляем как String, чтобы принять "1.45"
    private String store;

    // Конструкторы, геттеры, сеттеры
    public ProductPriceDTO() {}
    public ProductPriceDTO(String name, String price, String store) {
        this.name = name;
        this.price = price;
        this.store = store;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }
}
