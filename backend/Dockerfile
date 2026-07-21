# -- BUILDER
FROM eclipse-temurin:25-jdk-noble AS build
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
FROM eclipse-temurin:25-jre-noble AS runner
WORKDIR /app
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Shared browsers location so the non-root runtime user can read what root installed
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Copy Maven tooling to run Playwright install-deps
COPY --from=build /app/.mvn ./.mvn
COPY --from=build /app/mvnw ./mvnw
COPY --from=build /app/pom.xml ./pom.xml
COPY --from=build /root/.m2 /root/.m2

# Install all system dependencies required by Playwright & browsers
# (needs root for apt; chmod in the same layer to avoid duplicating ~500MB in a new layer)
RUN ./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium" -B \
    && chmod -R a+rX /ms-playwright

COPY --from=build /app/target/*.jar app.jar

# Run as a dedicated non-root user (Chromium also needs a writable home directory)
RUN useradd --system --create-home --shell /usr/sbin/nologin spring \
    && chown -R spring:spring /app

USER spring

CMD ["java", "-jar", "app.jar"]
