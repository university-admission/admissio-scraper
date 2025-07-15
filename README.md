# admissio-scraper
Сервіс для витягання даних з сайту vstup.edbo для проєкту **Admissio** — система прогнозування шансів вступу до університету.

## Технології
- Java 24
- Spring Boot
- Lombok
- Maven

## Запуск локально
Запусти проект:
```bash
./mvnw spring-boot:run
```

### Тестування
```bash
./mvnw test
```

## Запуск через Docker
```bash
docker build -t admissio-scraper .
docker run -p 8081:8081 admissio-scraper
```

## Перегляд ендпоіндів
http://localhost:8081/swagger-ui/index.html#/