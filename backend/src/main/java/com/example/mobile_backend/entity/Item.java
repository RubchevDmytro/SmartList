package com.example.mobile_backend.entity;

import jakarta.persistence.*;

@Entity
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private boolean bought = false;
    private double price = 0.0;
    private String store = "";  // e.g., "Lidl"

    // Конструкторы
    public Item() {}
    public Item(String name) {
        this.name = name;
    }

    // Геттеры/сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isBought() { return bought; }
    public void setBought(boolean bought) { this.bought = bought; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }
}
