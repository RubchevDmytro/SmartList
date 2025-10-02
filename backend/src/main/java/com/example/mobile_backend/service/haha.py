import asyncio
from playwright.async_api import async_playwright
import requests
import json
from googletrans import Translator
import time
translator = Translator()

async def send_batch(products, spring_url):
    try:
        response = requests.post(spring_url, json=products)
        response.raise_for_status()
        print(f"[INFO] Успешно отправлена партия из {len(products)} товаров на Spring Boot: {response.json()}")
    except requests.exceptions.RequestException as e:
        print(f"[ERROR] Ошибка при отправке партии на Spring Boot: {e}")

async def scrape_lidl_zlavy(url: str):
    print("[INFO] Запускаем playwright...")
    async with async_playwright() as pw:
        print("[INFO] Открываем Chromium...")
        browser = await pw.chromium.launch(headless=True)
        page = await browser.new_page()

        await page.set_extra_http_headers({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        })

        print(f"[INFO] Загружаем страницу: {url}")
        try:
            await page.goto(url, wait_until="domcontentloaded", timeout=60000)
            print("[INFO] Страница загружена.")
        except Exception as e:
            print(f"[ERROR] Не удалось загрузить страницу: {e}")
            await browser.close()
            return json.dumps([])

        try:
            print("[INFO] Проверяем баннер cookies...")
            await page.locator("#onetrust-accept-btn-handler").click(timeout=5000)
            print("[INFO] Баннер cookies закрыт.")
        except Exception as e:
            print(f"[WARN] Баннер cookies не найден или не удалось закрыть: {e}")

        await page.wait_for_selector("ol.odsc-tile-grid", timeout=20000)

        print("[INFO] Начинаем постепенную прокрутку...")
        scroll_step = 500
        current_scroll = 0
        max_scroll_attempts = 20
        consecutive_stalls = 0
        prev_product_count = 0

        while consecutive_stalls < 3 and current_scroll < await page.evaluate("document.body.scrollHeight"):
            current_scroll += scroll_step
            await page.evaluate(f"window.scrollTo(0, {current_scroll})")
            await page.wait_for_timeout(500)
            product_links = await page.locator("ol.odsc-tile-grid a.odsc-tile__link").all()
            product_count = len(product_links)
            print(f"[INFO] Текущий счёт продуктов: {product_count}, текущая прокрутка: {current_scroll}px")

            try:
                more_button = await page.locator('button.s-load-more__button')
                if more_button and await more_button.is_visible():
                    print("[INFO] Найдена кнопка 'Viac produktov', нажимаем до 4 раз...")
                    for _ in range(4):
                        await more_button.click(timeout=5000)
                        await asyncio.sleep(1)
                    await asyncio.sleep(2)
                    product_links = await page.locator("ol.odsc-tile-grid a.odsc-tile__link").all()
                    product_count = len(product_links)
                    print(f"[INFO] После нажатия кнопки счёт продуктов: {product_count}")
            except Exception as e:
                print(f"[WARN] Кнопка 'Viac produktov' не найдена, не видима или произошла ошибка: {e}")

            if product_count == prev_product_count:
                consecutive_stalls += 1
            else:
                consecutive_stalls = 0
            prev_product_count = product_count

        product_links = await page.locator("ol.odsc-tile-grid a.odsc-tile__link").all()
        print(f"[INFO] Финальное количество товаров: {len(product_links)}")

        products = []
        batch_size = 4
        spring_url = "http://localhost:8080/api/lists/prices/update"

        for idx, item in enumerate(product_links, start=1):
            print(f"[DEBUG] Обрабатываем товар #{idx}")
            name = None
            price = None

            try:
                name = await item.inner_text(timeout=60000)
                print(f"[DEBUG] Название: {name}")
            except Exception as e:
                print(f"[WARN] Не удалось извлечь название: {e}")

            try:
                price = await item.locator("xpath=..").locator(".ods-price__value").inner_text(timeout=60000)
                print(f"[DEBUG] Цена: {price}")
            except Exception as e:
                print(f"[WARN] Не удалось извлечь цену: {e}")

            if name and price:
                try:
                    clean_price = price.replace("€", "").replace(",", ".").strip()
                    translated = translator.translate(name.strip(), src="sk", dest="en").text
                    products.append({"name": translated, "price": clean_price, "store": "Lidl"})
                except Exception as e:
                    print(f"[WARN] Ошибка обработки цены для товара #{idx}: {e}")
            else:
                print(f"[WARN] Пропущен товар #{idx} — нет данных.")

            # Отправляем данные партиями по 4
            if len(products) >= batch_size:
                await send_batch(products[:batch_size], spring_url)
                products = products[batch_size:]

        # Отправляем оставшиеся продукты, если они есть
        if products:
            await send_batch(products, spring_url)

        await browser.close()
        print("[INFO] Браузер закрыт.")

        return json.dumps(products, ensure_ascii=False)

if __name__ == "__main__":
    time.sleep(5)
    url = "https://www.lidl.sk/c/jedlo-a-napoje/s10068374?offset=85"
    print("[INFO] Запуск парсинга...")
    results = asyncio.run(scrape_lidl_zlavy(url))
    print("[INFO] Результаты (JSON):")
    print(results)
