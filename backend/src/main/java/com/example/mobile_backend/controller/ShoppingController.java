package com.example.mobile_backend.controller;

import com.example.mobile_backend.entity.ShoppingList;
import com.example.mobile_backend.entity.Item;
import com.example.mobile_backend.entity.ItemCount;
import com.example.mobile_backend.dto.ProductPriceDTO;
import com.example.mobile_backend.repository.ShoppingListRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/lists")
@CrossOrigin(origins = "*")
public class ShoppingController {
    private final ShoppingListRepository repository;
    private final List<ProductPriceDTO> parsedPrices = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ShoppingController.class);

    public ShoppingController(ShoppingListRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ShoppingList> getAll() {
        return repository.findAll();
    }

    @GetMapping("/products")
    public List<Item> getAllProducts() {
        return repository.findAll().stream()
                .flatMap(list -> list.getItems().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    @PostMapping
    public ShoppingList create(@RequestBody ShoppingList list) {
        return repository.save(list);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingList> update(@PathVariable Long id, @RequestBody ShoppingList updatedList) {
        return repository.findById(id)
                .map(list -> {
                    list.setName(updatedList.getName());
                    list.getItems().clear();
                    list.getItems().addAll(updatedList.getItems());
                    return ResponseEntity.ok(repository.save(list));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/items/{itemId}/toggle")
    public ResponseEntity<ShoppingList> toggleItem(@PathVariable Long id, @PathVariable Long itemId) {
        return repository.findById(id)
                .map(list -> {
                    list.getItems().stream()
                            .filter(item -> item.getId().equals(itemId))
                            .findFirst()
                            .ifPresent(item -> item.setBought(!item.isBought()));
                    return ResponseEntity.ok(repository.save(list));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis/popular")
    public List<ItemCount> getPopularItems() {
        return repository.findPopularItems(10);
    }

    @GetMapping("/prices/update")
    public ResponseEntity<Map<String, String>> updatePricesGet() {
        return ResponseEntity.badRequest().body(Map.of("error", "GET not supported. Use POST for updating prices."));
    }

    @PostMapping("/prices/update")
    public ResponseEntity<Map<String, String>> updatePrices(@RequestBody List<ProductPriceDTO> products) {
        try {
            logger.debug("Received products for update: {}", products);
            parsedPrices.clear();
            parsedPrices.addAll(products);
            int updatedCount = 0;
            int addedCount = 0;

            // Получаем или создаём список для новых элементов
            ShoppingList defaultList = repository.findById(1L)
                    .orElseGet(() -> {
                        ShoppingList newList = new ShoppingList();
                        newList.setName("Default List");
                        return repository.save(newList);
                    });

            for (ProductPriceDTO product : products) {
                boolean found = false;
                // Проверяем все существующие списки
                List<ShoppingList> lists = repository.findAll();
                for (ShoppingList list : lists) {
                    for (Item item : list.getItems()) {
                        if (item.getName().equalsIgnoreCase(product.getName())) {
                            item.setPrice(Double.parseDouble(product.getPrice()));
                            item.setStore(product.getStore());
                            repository.save(list);
                            updatedCount++;
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                // Если продукт не найден, добавляем новый
                if (!found) {
                    Item newItem = new Item();
                    newItem.setName(product.getName());
                    newItem.setPrice(Double.parseDouble(product.getPrice()));
                    newItem.setStore(product.getStore());
                    newItem.setBought(false);
                    defaultList.getItems().add(newItem);
                    repository.save(defaultList);
                    addedCount++;
                }
            }

            logger.debug("Updated {} items and added {} new items in database", updatedCount, addedCount);
            return ResponseEntity.ok(Map.of("status", "Prices updated successfully", "count", String.valueOf(products.size()), "updated", String.valueOf(updatedCount), "added", String.valueOf(addedCount)));
        } catch (Exception e) {
            logger.error("Failed to update prices: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update prices: " + e.getMessage()));
        }
    }

    @GetMapping("/prices/{itemName}")
    public Map<String, Object> getPrices(@PathVariable String itemName, @RequestParam double lat, @RequestParam double lon) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> prices = new HashMap<>();
        String cheapestStore = null;
        double minPrice = Double.MAX_VALUE;

        for (ProductPriceDTO product : parsedPrices) {
            if (product.getName().equalsIgnoreCase(itemName)) {
                double price = Double.parseDouble(product.getPrice());
                prices.put(product.getStore(), price);
                if (price < minPrice) {
                    minPrice = price;
                    cheapestStore = product.getStore();
                }
            }
        }

        if (prices.isEmpty()) {
            prices.put("Lidl", 0.80);
            prices.put("Kaufland", 0.85);
            prices.put("Tesco", 0.90);
            cheapestStore = "Lidl";
        }

        response.put("item", itemName);
        response.put("prices", prices);
        response.put("cheapest", cheapestStore);
        response.put("location", "Nearest stores via lat/lon: " + lat + "," + lon);
        return response;
    }
}
