.PHONY: start stop dev test clean build generate-keys

start:
	docker compose up -d

stop:
	docker compose down

dev:
	if [ -f .env ]; then set -a && . ./.env && set +a; fi && cd backend && ./mvnw spring-boot:run

test:
	cd backend && ./mvnw test

clean:
	cd backend && ./mvnw clean

build:
	cd backend && ./mvnw package -DskipTests

generate-keys:
	./backend/scripts/generate-jwt-keys.sh

db-reset:
	docker compose down -v && docker compose up -d
