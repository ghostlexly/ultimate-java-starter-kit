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
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
