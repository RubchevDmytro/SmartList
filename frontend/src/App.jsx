import React, { useEffect, useState } from "react";
import axios from "axios";
import './App.css';

function App() {
  const [lists, setLists] = useState([]);
  const [newListName, setNewListName] = useState("");
  const [newItemName, setNewItemName] = useState("");
  const [position, setPosition] = useState(null);
  const [prices, setPrices] = useState(null);
  const [popularItems, setPopularItems] = useState([]);
  const [allProducts, setAllProducts] = useState([]);
  const [showProducts, setShowProducts] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Загрузка списков, популярных элементов и всех продуктов
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const listsResponse = await axios.get("http://localhost:8080/api/lists");
        setLists(listsResponse.data);
        const popularResponse = await axios.get("http://localhost:8080/api/lists/analysis/popular");
        setPopularItems(popularResponse.data);
        const productsResponse = await axios.get("http://localhost:8080/api/lists/products");
        setAllProducts(productsResponse.data);
      } catch (err) {
        setError("Nepodarilo sa načítať údaje. Skúste znova.");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, []);

  // Geolocation
  useEffect(() => {
    navigator.geolocation.getCurrentPosition(
      pos => {
        setPosition({ lat: pos.coords.latitude, lon: pos.coords.longitude });
      },
      err => {
        setError("Nepodarilo sa získať polohu. Použite predvolené nastavenia.");
      }
    );
  }, []);

  // Создать список
  const createList = async () => {
    if (!newListName.trim()) {
      setError("Názov zoznamu nemôže byť prázdny.");
      return;
    }
    try {
      const response = await axios.post("http://localhost:8080/api/lists", { name: newListName });
      setLists([...lists, response.data]);
      setNewListName("");
      setError(null);
    } catch (err) {
      setError("Nepodarilo sa vytvoriť zoznam.");
    }
  };

  // Добавить элемент
  const addItem = async (listId) => {
    if (!newItemName.trim()) {
      setError("Názov položky nemôže byť prázdny.");
      return;
    }
    try {
      const list = lists.find(l => l.id === listId);
      const updatedItems = [...list.items, { name: newItemName, bought: false, price: 0, store: "" }];
      await axios.put(`http://localhost:8080/api/lists/${listId}`, { name: list.name, items: updatedItems });
      setNewItemName("");
      setError(null);
      const response = await axios.get("http://localhost:8080/api/lists");
      setLists(response.data);
      const productsResponse = await axios.get("http://localhost:8080/api/lists/products");
      setAllProducts(productsResponse.data);
    } catch (err) {
      setError("Nepodarilo sa pridať položku.");
    }
  };

  // Отметить элемент как купленный/некупленный
  const toggleItem = async (listId, itemId) => {
    try {
      await axios.post(`http://localhost:8080/api/lists/${listId}/items/${itemId}/toggle`);
      const response = await axios.get("http://localhost:8080/api/lists");
      setLists(response.data);
      const productsResponse = await axios.get("http://localhost:8080/api/lists/products");
      setAllProducts(productsResponse.data);
    } catch (err) {
      setError("Nepodarilo sa aktualizovať položku.");
    }
  };

  // Получить цены для элемента
  const getPricesForItem = async (itemName) => {
    if (!position) {
      setError("Poloha nie je dostupná.");
      return;
    }
    try {
      const response = await axios.get(`http://localhost:8080/api/lists/prices/${encodeURIComponent(itemName)}?lat=${position.lat}&lon=${position.lon}`);
      setPrices(response.data);
      setError(null);
    } catch (err) {
      setError("Nepodarilo sa načítať ceny.");
    }
  };

  // Показать/скрыть модальное окно с продуктами
  const toggleProductsModal = () => {
    setShowProducts(!showProducts);
  };

  return (
    <div className="app-container">
      <h1>Moje nákupné zoznamy (Slovensko)</h1>

      {error && <div className="error">{error}</div>}
      {loading && <div className="loading">Načítavanie...</div>}

      {/* Кнопка для всех продуктов */}
      <div className="form-section">
        <button onClick={toggleProductsModal}>Všetky produkty</button>
      </div>

      {/* Форма создания списка */}
      <div className="form-section">
        <input
          type="text"
          placeholder="Názov zoznamu"
          value={newListName}
          onChange={(e) => setNewListName(e.target.value)}
        />
        <button onClick={createList}>Vytvoriť zoznam</button>
      </div>

      {/* Списки покупок */}
      <div className="lists-section">
        {lists.length === 0 ? (
          <p>Žiadne zoznamy. Vytvorte nový!</p>
        ) : (
          lists.map(list => (
            <div key={list.id} className="list-card">
              <h2>{list.name}</h2>
              <div className="add-item-form">
                <input
                  type="text"
                  placeholder="Pridať položku"
                  value={newItemName}
                  onChange={(e) => setNewItemName(e.target.value)}
                />
                <button onClick={() => addItem(list.id)}>Pridať</button>
              </div>
              <ul>
                {list.items.map(item => (
                  <li key={item.id} className="item">
                    <span>
                      {item.name}
                      {item.price > 0 && item.store ? ` - ${item.price.toFixed(2)} € (${item.store})` : " - Cena neznáma"}
                      {item.bought ? " ✅" : " ❌"}
                    </span>
                    <div>
                      <button onClick={() => toggleItem(list.id, item.id)}>Toggle</button>
                      <button onClick={() => getPricesForItem(item.name)}>Ceny</button>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ))
        )}
      </div>

      {/* Популярные элементы */}
      <div className="popular-section">
        <h2>Populárne položky (odporúčania)</h2>
        {popularItems.length === 0 ? (
          <p>Žiadne populárne položky.</p>
        ) : (
          <ul>
            {popularItems.map(item => (
              <li key={item.name}>{item.name} (objavené {item.count}x)</li>
            ))}
          </ul>
        )}
      </div>

      {/* Цены */}
      {prices && (
        <div className="prices-section">
          <h2>Ceny pre {prices.item}</h2>
          <ul>
            {Object.entries(prices.prices).map(([store, price]) => (
              <li key={store}>{store}: {price.toFixed(2)} €</li>
            ))}
          </ul>
          <p>Najlacnejšie: {prices.cheapest} (blízko: {prices.location})</p>
        </div>
      )}

      {/* Модальное окно для всех продуктов */}
      {showProducts && (
        <div className="modal">
          <div className="modal-content">
            <h2>Všetky produkty</h2>
            {allProducts.length === 0 ? (
              <p>Žiadne produkty.</p>
            ) : (
              <ul>
                {allProducts.map(product => (
                  <li key={product.id}>
                    {product.name}
                    {product.price > 0 && product.store ? ` - ${product.price.toFixed(2)} € (${product.store})` : " - Cena neznáma"}
                  </li>
                ))}
              </ul>
            )}
            <button onClick={toggleProductsModal}>Zatvoriť</button>
          </div>
        </div>
      )}

      <p className="note">
        Poznámka: Použi GPS pre blízke obchody. Integruj Google Shopping API pre reálne ceny.
      </p>
    </div>
  );
}

export default App;
