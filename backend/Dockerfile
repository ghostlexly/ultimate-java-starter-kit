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

# Install Playwright Chromium binaries (cached in this stage)
RUN ./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium" -B

# -- RUNNER
FROM eclipse-temurin:25-jre
WORKDIR /app

# Install Chromium system dependencies required by Playwright
RUN apt-get update && apt-get install -y --no-install-recommends \
    libnss3 \
    libatk1.0-0t64 \
    libatk-bridge2.0-0t64 \
    libcups2t64 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2t64 \
    libxshmfence1 \
    fonts-liberation \
    fonts-noto-color-emoji \
    && rm -rf /var/lib/apt/lists/*

# Copy Playwright browser binaries from builder
COPY --from=build /root/.cache/ms-playwright /root/.cache/ms-playwright

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
