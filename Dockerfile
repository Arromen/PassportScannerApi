FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x ./mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libopencv-core4.5 \
    libopencv-imgproc4.5 \
    libopencv-highgui4.5 \
    libopencv-objdetect4.5 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar

COPY --from=builder /app/src/main/resources/haarcascade /app/haarcascade

RUN mkdir -p /app/uploaded-images

EXPOSE 8080

ENV LD_LIBRARY_PATH=/usr/lib/jni

ENTRYPOINT ["java", "-jar", "/app/app.jar"]