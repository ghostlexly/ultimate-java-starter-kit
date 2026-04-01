.PHONY: start stop dev test clean build generate-keys

help: ## Show this help page
	@grep -E '(^[a-zA-Z0-9_-]+:.*?##.*$$)|(^##)' Makefile | awk 'BEGIN {FS = ":.*?##"}{printf "\033[32m%-30s\033[0m %s\n", $$1, $$2}' | sed -e 's/\[32m##/[33m/'

start: ## Start the containers
	docker compose up -d --renew-anon-volumes

start-prod: ## Start the production environment (ignores compose.override.yml)
	docker compose -f compose.yml up -d --renew-anon-volumes

stop: ## Stop the containers
	docker compose down

dev: ## Start the development backend
	cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

test: ## Run unit tests
	cd backend && ./mvnw test

clean: ## Maven Clean
	cd backend && ./mvnw clean

build: ## Maven Build
	cd backend && ./mvnw package -DskipTests

generate-keys: ## Generate SSH keys
	./backend/scripts/generate-jwt-keys.sh

db-reset:
	docker compose down -v && docker compose up -d
