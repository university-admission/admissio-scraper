FROM openjdk:24-jdk-slim

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/scraper-0.0.1-SNAPSHOT.jar"]
