# Установка

Репозиторий с общими тестами подключен как git submodule, поэтому после клонирования нужно выполнить 
```bash
git submodule init && git submodule update
```

# Сборка

```bash
./gradlew installDist 
```

После этого в папке `build/install/TypesProject/bin/` будут находиться скрипты для запуска приложения.

# Запуск приложения

Unix:
```bash
./build/install/TypesProject/bin/TypesProject
```
Windows:
```bash
./build/install/TypesProject/bin/TypesProject.bat
```

# Запуск приложения на общих тестах

Можно запустить приложение на всех общих тестах и посмотреть ее вывод. Для этого запустите скрипт `run_test_files.sh`.

Убедитесь, что submodule с тестами загружен (см. [Установка](#установка)).


