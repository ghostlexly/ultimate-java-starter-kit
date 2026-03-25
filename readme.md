Structure du projet

config/          → JpaConfig, JwtProperties, SecurityConfig
core/
dto/           → ErrorResponse, Violation
exception/     → BusinessRuleException, GlobalExceptionHandler
security/      → JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
entity/          → BaseEntity, Account, Session, Customer, Admin, VerificationToken, Media, AppConfigEntity, Role, VerificationType
repository/      → 7 Spring Data JPA repositories
module/
auth/          → AuthController, AuthService, AuthConstants + DTOs + Events
customer/      → CustomerController, CustomerService + DTOs
admin/         → AdminController, AdminService

Fonctionnalites (equivalentes au projet TS)

┌────────────────────────────┬────────────────────────┬────────────────────────────────────────────┐
│          Feature           │      TS (NestJS)       │            Java (Spring Boot 4)            │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Auth par code de connexion │ @nestjs/cqrs + events  │ ApplicationEventPublisher + @EventListener │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ JWT RSA256                 │ @nestjs/jwt + passport │ jjwt + custom filter                       │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Cookies httpOnly           │ Cookie helper          │ jakarta.servlet.http.Cookie                │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Roles (ADMIN/CUSTOMER)     │ @Roles() decorator     │ @PreAuthorize("hasRole()")                 │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Validation                 │ Zod                    │ Jakarta Bean Validation                    │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Gestion erreurs globale    │ ExceptionFilter        │ @RestControllerAdvice                      │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Protection brute-force     │ 5 tentatives max       │ Identique                                  │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Cooldown entre codes       │ 60 secondes            │ Identique                                  │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Migrations DB              │ Prisma Migrate         │ Flyway                                     │
├────────────────────────────┼────────────────────────┼────────────────────────────────────────────┤
│ Docker                     │ compose.yml            │ compose.yml (PostgreSQL + Redis)           │
└────────────────────────────┴────────────────────────┴────────────────────────────────────────────┘

API Endpoints

- POST /api/auth/send-code — Envoie un code 4 chiffres (public)
- POST /api/auth/verify-code — Verifie le code, cree la session, retourne les JWT (public)
- POST /api/auth/refresh — Rafraichit les tokens (public)
- GET /api/auth/me — Info utilisateur (authentifie)
- GET/POST /api/customer/profile — Profil client (role CUSTOMER)
- GET /api/admin/stats — Statistiques (role ADMIN)

Pour demarrer

make start                          # Lance PostgreSQL + Redis
./scripts/generate-jwt-keys.sh      # Genere les cles JWT
# Copier les cles dans .env
make dev                            # Lance l'app