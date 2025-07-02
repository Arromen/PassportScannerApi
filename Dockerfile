# Используем образ с поддержкой Java 21
FROM eclipse-temurin:21-jdk-jammy

# Этап сборки
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Копируем файлы сборки
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Делаем скрипт исполняемым
RUN chmod +x ./mvnw

# Загружаем зависимости
RUN ./mvnw dependency:go-offline

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN ./mvnw clean package -DskipTests

# Финальный этап
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Копируем JAR из этапа сборки
COPY --from=builder /app/target/*.jar app.jar

# Создаём директорию для загруженных файлов
RUN mkdir -p /app/uploaded-images

EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "/app/app.jar"]