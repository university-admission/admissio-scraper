FROM amazoncorretto:24

WORKDIR /app

COPY . .

RUN yum install -y tar gzip
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/scraper-0.0.1-SNAPSHOT.jar"]
