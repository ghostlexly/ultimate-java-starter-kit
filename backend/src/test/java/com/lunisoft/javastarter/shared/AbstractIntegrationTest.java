package com.lunisoft.javastarter.shared;

import com.lunisoft.javastarter.core.pdf.PlaywrightWorker;
import com.lunisoft.javastarter.core.security.JwtTokenProvider;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.email.service.EmailService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import com.redis.testcontainers.RedisContainer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared base class for API integration tests.
 *
 * <p>Boots the full Spring context against ephemeral Postgres and Redis containers (industry
 * standard via Testcontainers). Flyway runs the real production migrations against the throwaway
 * Postgres so the schema under test matches what ships.
 *
 * <p>Containers are singletons started once per JVM (no {@code @Testcontainers}) and shared across
 * every test class — Spring's context cache reuses the same wired beans across the whole run, so
 * the suite stays fast.
 *
 * <p>External adapters with no value in integration tests (Playwright browser, S3 client) are
 * replaced with Mockito mocks so the context can start without browser binaries or network access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestFixtures.class)
public abstract class AbstractIntegrationTest {

  @ServiceConnection
  protected static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17.9"));

  @ServiceConnection(name = "redis")
  protected static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:8.6.2-alpine"));

  static {
    POSTGRES.start();
    REDIS.start();
  }

  // Generated once per JVM — RSA keypair generation is expensive and tests only need
  // deterministic keys within a single run, not across runs.
  private static final KeyPair JWT_KEY_PAIR = generateRsaKeyPair();

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "app.jwt.private-key",
        () -> encodeBase64Pem(JWT_KEY_PAIR.getPrivate().getEncoded(), "PRIVATE KEY"));
    registry.add(
        "app.jwt.public-key",
        () -> encodeBase64Pem(JWT_KEY_PAIR.getPublic().getEncoded(), "PUBLIC KEY"));
    // JobRunr dashboard is disabled in test profile but still resolves its password placeholder.
    registry.add("APP_JOBRUNR_DASHBOARD_PASSWORD", () -> "test");
  }

  // ── Mocks for adapters we don't exercise from API tests ──────────────────

  @MockitoBean
  protected S3Client s3Client;
  @MockitoBean
  protected S3Presigner s3Presigner;
  @MockitoBean
  protected PlaywrightWorker playwrightWorker;
  @MockitoBean
  protected EmailService emailService;

  // ── Common collaborators tests will need ─────────────────────────────────

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  protected JsonMapper jsonMapper;
  @Autowired
  protected JwtTokenProvider jwtTokenProvider;
  @Autowired
  protected IntegrationTestFixtures fixtures;
  @Autowired
  private DataSource dataSource;
  @Autowired
  private PlatformTransactionManager transactionManager;

  /**
   * Truncate every application table after each test so cases stay isolated even though the
   * container (and Spring context) are shared. JobRunr's own tables are left alone — wiping them
   * would break its scheduler state across the suite.
   */
  @AfterEach
  void cleanDatabase() {
    var jdbc = new JdbcTemplate(dataSource);
    jdbc.execute(
        """
            TRUNCATE TABLE
                verification_token,
                session,
                customer,
                admin,
                media,
                app_config,
                account
            RESTART IDENTITY CASCADE
            """);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static KeyPair generateRsaKeyPair() {
    try {
      var generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);

      return generator.generateKeyPair();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate RSA key pair for tests", e);
    }
  }

  /**
   * Mirror the production wire format expected by {@link JwtTokenProvider}: base64-of(PEM-wrapped
   * key). Decoding goes through the exact same path the real app uses.
   */
  private static String encodeBase64Pem(byte[] der, String label) {
    var base64Der = Base64.getEncoder().encodeToString(der);
    var pem = "-----BEGIN %s-----%n%s%n-----END %s-----".formatted(label, base64Der, label);

    return Base64.getEncoder().encodeToString(pem.getBytes());
  }

  /**
   * Runs DB-state assertions inside a single read transaction so lazy associations can be traversed
   * after the request under test has already committed. Open Session In View is disabled, so
   * without this wrapper accessing a lazy relation here would throw
   * {@code LazyInitializationException}.
   */
  public void assertPersistedState(Runnable assertions) {
    new TransactionTemplate(transactionManager).executeWithoutResult(_ -> assertions.run());
  }

  /**
   * Builds an {@code Authorization} header value with a fresh access token for the account.
   */
  public String bearer(Account account) {
    var token =
        jwtTokenProvider.generateAccessToken(
            UUID.randomUUID(), account.getId(), account.getEmail(), account.getRole());

    return "Bearer %s".formatted(token);
  }
}
