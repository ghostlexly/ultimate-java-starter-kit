package com.lunisoft.javastarter.core.pdf;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Owns the {@link Playwright} and {@link Browser} instances and confines every interaction with
 * them to a single dedicated worker thread.
 *
 * <p>Playwright's Java client is not thread-safe: each calling thread pumps the driver's message
 * loop, so concurrent access from multiple request threads corrupts its internal object registry
 * (e.g. "Object doesn't exist: frame@...", "this.mainFrame is null"). Serializing all browser work
 * on one thread makes concurrent callers safe, and lets us detect and relaunch a crashed browser
 * between tasks.
 */
@Component
public class PlaywrightWorker {

  private final Logger log = LoggerFactory.getLogger(PlaywrightWorker.class);

  /** Single platform thread that owns the Playwright instance; tasks are serialized on it. */
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(Thread.ofPlatform().name("playwright-worker").factory());

  // Accessed only from the worker thread — never from request threads.
  private Playwright playwright;
  private Browser browser;

  /**
   * Runs the given task against the shared {@link Browser} on the dedicated worker thread and
   * returns its result. Tasks from concurrent callers are executed one at a time.
   */
  public <T> T execute(Function<Browser, T> task) {
    try {
      return executor.submit(() -> task.apply(ensureBrowser())).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      throw new IllegalStateException("Interrupted while waiting for the Playwright worker", e);
    } catch (ExecutionException e) {
      // Unwrap so callers see the original Playwright/business exception
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }

      throw new IllegalStateException("Playwright task failed", e.getCause());
    }
  }

  /**
   * Lazily launches the browser on first use, relaunching it if the previous instance crashed or
   * disconnected. Runs on the worker thread only.
   */
  private Browser ensureBrowser() {
    if (browser != null && browser.isConnected()) {
      return browser;
    }

    closeQuietly();
    log.info("Launching headless Chromium for PDF generation");
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

    return browser;
  }

  /** Closes the browser and Playwright on the worker thread, then stops the executor. */
  @PreDestroy
  void shutdown() {
    executor.submit(this::closeQuietly);
    executor.shutdown();

    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }

  /** Best-effort cleanup of the current Playwright resources. Runs on the worker thread only. */
  private void closeQuietly() {
    try {
      if (browser != null) {
        browser.close();
      }

      if (playwright != null) {
        playwright.close();
      }
    } catch (Exception e) {
      log.warn("Failed to close Playwright cleanly: {}", e.getMessage());
    } finally {
      browser = null;
      playwright = null;
    }
  }
}
