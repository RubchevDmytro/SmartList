package com.example.mobile_backend.entity;

public class ItemCount {
    private String name;
    private Long count;

    public ItemCount(String name, Long count) {
        this.name = name;
        this.count = count;
    }

    // Геттеры и сеттеры
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
