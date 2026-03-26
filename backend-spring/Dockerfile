FROM gradle:8.14.2-jdk17 AS builder
WORKDIR /app
COPY . .

# gradlew 위치 루트/서브폴더 둘 다 처리
RUN set -eux; \
    if [ -f "./gradlew" ]; then \
      chmod +x ./gradlew; \
      ./gradlew clean bootJar -x test --no-daemon; \
      cp build/libs/*.jar /app/app.jar; \
    elif [ -f "./backend-spring/gradlew" ]; then \
      chmod +x ./backend-spring/gradlew; \
      ./backend-spring/gradlew -p backend-spring clean bootJar -x test --no-daemon; \
      cp backend-spring/build/libs/*.jar /app/app.jar; \
    else \
      echo "ERROR: gradlew not found in . or ./backend-spring"; \
      echo "Top-level dirs:"; ls -al; \
      exit 1; \
    fi

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/app.jar app.jar
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]