FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY AppApplication/gradlew .
COPY AppApplication/gradle gradle
COPY AppApplication/settings.gradle.kts .
COPY AppApplication/build.gradle.kts .

RUN chmod +x gradlew

COPY AppApplication/src src

RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
