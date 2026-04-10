# -- BUILDER
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy project files
COPY ./.mvn ./.mvn
COPY ./mvnw ./mvnw
COPY ./pom.xml ./pom.xml
COPY ./src ./src

# Set permissions on mvnw
RUN chmod +x mvnw

# Install dependencies
RUN ./mvnw dependency:resolve -B

# Build project
RUN ./mvnw package -DskipTests -B

# -- RUNNER
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Copy Maven tooling to run Playwright install-deps
COPY --from=build /app/.mvn ./.mvn
COPY --from=build /app/mvnw ./mvnw
COPY --from=build /app/pom.xml ./pom.xml
COPY --from=build /root/.m2 /root/.m2

# Install all system dependencies required by Playwright & browsers
RUN ./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium" -B

# Remove Maven tooling
RUN rm -rf /root/.m2 .mvn mvnw pom.xml

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
