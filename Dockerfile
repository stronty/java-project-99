FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Wrapper and build files first, so dependency downloads are cached
COPY AppApplication/gradlew AppApplication/settings.gradle.kts AppApplication/build.gradle.kts ./
COPY AppApplication/gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY AppApplication/src src
RUN ./gradlew --no-daemon bootJar -x test

# Pick the runnable jar without depending on the project name or version
RUN cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)" /app/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
