-- Создаём таблицу ShoppingList
INSERT INTO SHOPPING_LIST (id, name) VALUES (1, 'Супермаркет');
INSERT INTO SHOPPING_LIST (id, name) VALUES (2, 'Аптека');

-- Создаём таблицу Item
INSERT INTO ITEM (id, name, bought) VALUES (1, 'Хлеб', false);
INSERT INTO ITEM (id, name, bought) VALUES (2, 'Молоко', true);
INSERT INTO ITEM (id, name, bought) VALUES (3, 'Йогурт', false);
INSERT INTO ITEM (id, name, bought) VALUES (4, 'Витамины', false);

-- Связываем товары со списками
-- H2 поддерживает прямые связи через FOREIGN KEY, но если используешь @OneToMany с cascade, можно просто сохранять через JPA

