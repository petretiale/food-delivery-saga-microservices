# Stage 1: Build dell'applicazione
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copia prima solo il pom.xml per sfruttare la cache dei layer Docker sulle dipendenze
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia i sorgenti e compila saltando i test (i test li abbiamo già verificati in locale)
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime leggero
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia solo il file .jar generato dallo stage precedente
COPY --from=builder /app/target/*.jar app.jar

# Espone la porta e avvia l'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]
