/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR
A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.service.pdf;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.util.Collections;
import java.util.Map;

/**
 * Service for generating PDF documents from Thymeleaf templates.
 * <p>
 * This service orchestrates a complete PDF generation workflow:
 * Thymeleaf template rendering → HTML generation → Jsoup XHTML normalization → Flying Saucer PDF conversion.
 * Supports multi-page PDF generation by accepting multiple model objects as varargs parameters.
 * </p>
 * <p>
 * <b>Library Integration:</b>
 * </p>
 * <ul>
 * <li>Thymeleaf {@link TemplateEngine} for HTML rendering from templates</li>
 * <li>Jsoup for XHTML normalization (Flying Saucer requires well-formed XHTML)</li>
 * <li>Flying Saucer {@link ITextRenderer} for PDF generation from XHTML</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * byte[] pdf = pdfConstructor.writeDocumentToByteArray("invoice", modelMap);
 * }</pre>
 * </p>
 * <p>
 * <b>Thread Safety:</b> This service is thread-safe. Each PDF generation creates a new {@link ITextRenderer}
 * instance, promoting safe concurrent usage. The injected {@link TemplateEngine} is thread-safe.
 * </p>
 * <p>
 * Extends {@link LoggingComponentWithRequestId} for request-scoped tracing and correlation.
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @since 1.7.1
 */
@Service
public class PdfConstructor implements LoggingComponentWithRequestId {

    /**
     * Base URL injected into Thymeleaf context for absolute resource references.
     * <p>
     * This URL is automatically added to the template context as {@code baseUrl} variable,
     * enabling templates to construct absolute URLs for images, CSS, and other resources.
     * Sourced from {@code base.url} property with default {@code http://localhost:8080}.
     * </p>
     */
    @Value("${base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Email sender name for email-related PDF templates.
     * <p>
     * Sourced from {@code mail.from} property with default empty string.
     * Used in templates that generate email attachments or email-style documents.
     * </p>
     */
    @Value("${mail.from:}")
    private String emailNameFrom;

    /**
     * Path for custom font embedding in PDF documents.
     * <p>
     * Sourced from {@code font.path} property with default {@code /fonts/arialuni.ttf}.
     * Note: Font registration is currently commented out at line 114 in {@link #writeDocumentToOutputStream}.
     * Enabling font embedding may be required for full international character support.
     * </p>
     */
    @Value("${font.path:/fonts/arialuni.ttf}")
    private String fontPath;

    /**
     * Thymeleaf template engine for HTML rendering from templates.
     * <p>
     * Injected thread-safe shared instance used throughout PDF generation.
     * Processes template files with variable substitution to produce HTML output.
     * </p>
     */
    @Inject
    private TemplateEngine templateEngine;

    /**
     * Creates a locale-aware Thymeleaf context for template rendering.
     * <p>
     * Uses {@link LocaleContextHolder#getLocale()} to retrieve the current locale,
     * enabling internationalization support for templates. Returns a new {@link Context}
     * instance per call with debug logging for traceability.
     * </p>
     *
     * @return new Thymeleaf {@link Context} initialized with current locale
     */
    private Context getContext() {
        debug("[getContext]");
        return new Context(LocaleContextHolder.getLocale());
    }

    /**
     * Renders a Thymeleaf template with the provided model to produce HTML content.
     * <p>
     * <b>Workflow:</b>
     * </p>
     * <ol>
     * <li>Creates locale-aware {@link Context} via {@link #getContext()}</li>
     * <li>Injects {@code baseUrl} variable for absolute resource references</li>
     * <li>Copies all entries from model map to context as template variables</li>
     * <li>Processes template through {@link TemplateEngine} to generate HTML</li>
     * </ol>
     *
     * @param templateName the template resource name (e.g., "invoice" or "report/summary")
     * @param model        map of variables to inject into template context
     * @return rendered HTML string ready for XHTML normalization and PDF conversion
     */
    public String prepareContent(String templateName, Map<String, Object> model) {
        debug("[prepareContent] {}", templateName);
        final Context ctx = getContext();
        ctx.setVariable("baseUrl", baseUrl);

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            ctx.setVariable(entry.getKey(), entry.getValue());
        }

        return templateEngine.process(templateName, ctx);
    }

    /**
     * Generates a PDF document as a byte array with separate pages for each model provided.
     * <p>
     * When models is null or empty, the template is rendered with an empty model and generates a single-page PDF.
     * For multiple models, each model generates a separate page in the resulting PDF document.
     * </p>
     * <p>
     * <b>Memory Usage Note:</b> The entire PDF is loaded into memory as a byte array.
     * For very large PDFs, consider using {@link #writeDocumentToStream} or {@link #writeDocumentToOutputStream}.
     * </p>
     * <p>
     * Example:
     * <pre>{@code
     * byte[] pdf = pdfConstructor.writeDocumentToByteArray("invoice", invoiceModel);
     * }</pre>
     * </p>
     *
     * @param templateName the Thymeleaf template name to render
     * @param models       varargs array of model maps, one per page (null or empty generates single page)
     * @return byte array containing the complete PDF document
     */
    public byte[] writeDocumentToByteArray(String templateName, Map<String, Object> ... models) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeDocumentToOutputStream(templateName, baos, models);
        return baos.toByteArray();
    }

    /**
     * Generates a PDF document as an InputStream with separate pages for each model provided.
     * <p>
     * Convenience method that wraps {@link #writeDocumentToByteArray} result in a {@link ByteArrayInputStream}.
     * When models is null or empty, generates a single-page PDF with empty model.
     * Useful for streaming scenarios where an InputStream is required (e.g., HTTP response bodies).
     * </p>
     *
     * @param templateName the Thymeleaf template name to render
     * @param models       varargs array of model maps, one per page (null or empty generates single page)
     * @return InputStream wrapping the complete PDF document bytes
     */
    public InputStream writeDocumentToStream(String templateName, Map<String, Object> ... models) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeDocumentToOutputStream(templateName, baos, models);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Generates a PDF document to an OutputStream with separate pages for each model provided.
     * <p>
     * <b>Multi-Page Generation Workflow:</b>
     * </p>
     * <ol>
     * <li>Creates new {@link ITextRenderer} instance for this PDF generation</li>
     * <li>Renders first model (or empty map if models is null/empty) through {@link #prepareContent}</li>
     * <li>Normalizes HTML to XHTML via Jsoup {@link Parser#xmlParser()} (Flying Saucer requirement)</li>
     * <li>Calls {@link ITextRenderer#createPDF(OutputStream, boolean)} with {@code false} flag to keep renderer open</li>
     * <li>For each additional model (lines 126-132), renders content and calls {@link ITextRenderer#writeNextDocument()}</li>
     * <li>Calls {@link ITextRenderer#finishPDF()} to complete the PDF document</li>
     * </ol>
     * <p>
     * <b>Error Handling Strategy:</b> All exceptions are caught and logged via {@link #error(Throwable, String, Object...)}.
     * The method always returns {@code true} even on errors. Callers must check logs for failure detection.
     * The {@link OutputStream} is closed in the finally block; IOExceptions during close are swallowed.
     * </p>
     * <p>
     * <b>CSS Styling Considerations:</b> CSS must be XHTML-compatible for Flying Saucer renderer.
     * Complex CSS3 features may not be supported. Test templates thoroughly.
     * </p>
     * <p>
     * <b>Font Embedding Note:</b> Unicode font embedding is commented out at line 114.
     * Enable {@code renderer.getFontResolver().addFont(fontPath, ...)} for full international character support.
     * </p>
     *
     * @param templateName the Thymeleaf template name to render
     * @param os           the OutputStream to write PDF bytes (closed in finally block)
     * @param models       varargs array of model maps, one per page (null or empty generates single page)
     * @return always returns {@code true}, even on error (check logs for failures)
     */
    public boolean writeDocumentToOutputStream(String templateName, OutputStream os, Map<String, Object> ... models) {
        debug("[writeDocumentToStream] {}", templateName);
        try {


            ITextRenderer renderer = new ITextRenderer();
//            renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Map<String, Object> firstModel = models == null || models.length == 0 ? Collections.emptyMap() : models[0];
            String htmlContent = prepareContent(templateName, firstModel);
            // we need to create the target PDF
            // we'll create one page per input string, but we call layout for the first
            String html = Jsoup.parse(htmlContent, "", Parser.xmlParser()).html();

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os, false);

            //creating next pages
            for (int i = 1; i < models.length; i++) {
                String nextContent = prepareContent(templateName, models[i]);
                String nextHtml = Jsoup.parse(nextContent, "", Parser.xmlParser()).html();
                renderer.setDocumentFromString(nextHtml);
                renderer.layout();
                renderer.writeNextDocument();
            }
            // complete the PDF
            renderer.finishPDF();

        } catch (Exception e) {
            error(e, "Error generating pdf for template {} and models {} ", templateName, models);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) { /*ignore*/ }
            }
        }
        return true;
    }

}
