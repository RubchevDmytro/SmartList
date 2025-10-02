#!/bin/bash

FRONTEND_DIR="./frontend"
BACKEND_DIR="./backend"

# Освобождаем порт 8080
echo "Закрываем процессы на порте 8080..."
PID=$(lsof -t -i:8080)
if [ -n "$PID" ]; then
    kill -9 $PID
    echo "Убил процесс $PID на порте 8080"
else
    echo "Ничего не слушает порт 8080"
fi

# Проверка директорий
if [ ! -d "$FRONTEND_DIR" ]; then
    echo "Папка $FRONTEND_DIR не найдена!"
    exit 1
fi

if [ ! -d "$BACKEND_DIR" ]; then
    echo "Папка $BACKEND_DIR не найдена!"
    exit 1
fi

# Запуск frontend в новой вкладке
echo "Запускаем frontend в новой вкладке iTerm2..."
osascript <<EOF
tell application "iTerm"
    if not (exists current window) then
        create window with default profile
    end if
    tell current window
        set newTab to (create tab with default profile)
        tell current session of newTab
            write text "cd \"$(pwd)/$FRONTEND_DIR\" && npm run dev"
        end tell
    end tell
end tell
EOF

# Запуск backend в новой вкладке
echo "Запускаем backend в новой вкладке iTerm2..."
osascript <<EOF
tell application "iTerm"
    if not (exists current window) then
        create window with default profile
    end if
    tell current window
        set newTab to (create tab with default profile)
        tell current session of newTab
            write text "cd \"$(pwd)/$BACKEND_DIR\" && watchexec -r -e java,kt,xml,py,properties 'mvn spring-boot:run'"
        end tell
    end tell
end tell
EOF
