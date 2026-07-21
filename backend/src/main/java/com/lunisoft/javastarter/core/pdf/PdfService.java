package com.lunisoft.javastarter.core.pdf;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.property.PdfProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final Logger log = LoggerFactory.getLogger(PdfService.class);

    private final SpringTemplateEngine templateEngine;
    private final PlaywrightWorker playwrightWorker;
    private final PdfProperties pdfProperties;
    private final ResourceLoader resourceLoader;

    /** Cached data URI of the local Tailwind CSS browser build (loaded once, reused across renders). */
    private volatile String tailwindJsUri;

    /**
     * Renders an HTML template with the given data and converts it to a PDF.
     *
     * @param templateName the template file name without extension (e.g. "invoice"), resolved from
     *     resources/templates/{templateName}.html
     * @param data key-value pairs passed to the template as Thymeleaf variables
     * @return the PDF as a byte array
     */
    public byte[] generate(String templateName, Map<String, Object> data) {
        var html = renderHtml(templateName, data);

        return convertHtmlToPdf(html);
    }

    /** Renders an HTML template with Thymeleaf and returns the resulting HTML string. */
    private String renderHtml(String templateName, Map<String, Object> data) {
        var context = new Context();
        context.setVariables(data);

        // Expose the local Tailwind build to every template
        context.setVariable("tailwindJsUri", getTailwindJsUri());

        return templateEngine.process(templateName, context);
    }

    /** Returns the local Tailwind CSS browser build as a data URI, cached after the first load. */
    private String getTailwindJsUri() {
        if (tailwindJsUri == null) {
            tailwindJsUri = toDataUri("classpath:templates/assets/tailwind.js", "text/javascript");
        }

        return tailwindJsUri;
    }

    /**
     * Converts an HTML string to a PDF using Playwright's headless Chromium. The rendering runs on
     * the {@link PlaywrightWorker}'s dedicated thread because Playwright is not thread-safe.
     */
    private byte[] convertHtmlToPdf(String html) {
        try {
            return playwrightWorker.execute(browser -> renderPdf(browser, html));
        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);

            throw new BusinessRuleException(
                    "Failed to generate PDF", "PDF_GENERATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Renders the HTML in a fresh browser context and returns the PDF bytes. Waits for Tailwind CSS
     * CDN to finish rendering before generating the PDF. Runs on the worker thread only.
     */
    private byte[] renderPdf(Browser browser, String html) {
        try (var browserContext = browser.newContext();
                var page = browserContext.newPage()) {

            // Load HTML and wait for all network requests (Tailwind CDN) to complete
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // Wait for Tailwind to inject its <style> tag after processing utility classes
            // Uses ATTACHED state since <style> tags are not visible DOM elements
            page.waitForSelector(
                    "style",
                    new Page.WaitForSelectorOptions()
                            .setState(WaitForSelectorState.ATTACHED)
                            .setTimeout(pdfProperties.renderTimeoutMs()));

            return page.pdf(new Page.PdfOptions()
                    .setFormat(pdfProperties.format())
                    .setPrintBackground(pdfProperties.printBackground()));
        }
    }

    /**
     * Converts a resource (e.g. "static/avatar.png") to a base64 data URI that can be used
     * as an img src in HTML templates rendered by Playwright.
     *
     * @param filePath the path relative to the file
     * @param mimeType the MIME type (e.g. "image/png")
     * @return the data URI string (e.g. "data:image/png;base64,...")
     */
    public String toDataUri(String filePath, String mimeType) {
        try {
            var resource = resourceLoader.getResource(filePath);
            var bytes = resource.getContentAsByteArray();
            var base64 = Base64.getEncoder().encodeToString(bytes);

            return "data:%s;base64,%s".formatted(mimeType, base64);
        } catch (IOException e) {
            log.error("Failed to load resource '{}': {}", filePath, e.getMessage(), e);

            throw new BusinessRuleException(
                    "Failed to load resource: %s".formatted(filePath),
                    "RESOURCE_NOT_FOUND",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
