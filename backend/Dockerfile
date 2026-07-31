# syntax=docker/dockerfile:1

# =========================================================
# Stage 1: Build
# Dùng đầy đủ JDK để compile source code thành file JAR.
# =========================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy Maven Wrapper và pom.xml trước.
# Docker có thể cache bước tải dependency nếu source code thay đổi
# nhưng pom.xml không thay đổi.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw -B -ntp dependency:go-offline

# Sau khi dependency đã được tải, mới copy source code.
COPY src/ src/

# CI sẽ chạy test riêng.
# Docker build chỉ chịu trách nhiệm tạo production artifact.
RUN ./mvnw -B -ntp clean package -DskipTests \
    && cp "$(find target \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name '*-sources.jar' \
        | head -n 1)" target/app.jar


# =========================================================
# Stage 2: Runtime
# Chỉ cần JRE để chạy JAR, không cần Maven hoặc compiler.
# =========================================================
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

# Không nên chạy application bằng root.
RUN groupadd --system bebo \
    && useradd --system \
        --gid bebo \
        --create-home \
        bebo

COPY --from=builder /workspace/target/app.jar ./app.jar

USER bebo

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
