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

/**
 * Services for PDF document generation and manipulation through HTML-to-PDF conversion.
 * <p>
 * This package provides enterprise-grade PDF generation capabilities by converting
 * Thymeleaf HTML templates into PDF documents. The primary use cases include report
 * generation, invoice printing, document exports, and template-based PDF creation.
 * 
 *
 * <b>Package Architecture</b>
 * <p>
 * The core component is {@link com.openkoda.core.service.pdf.PdfConstructor}, which
 * orchestrates the complete PDF generation workflow:
 * 
 * <ol>
 *     <li>Thymeleaf templates are rendered to HTML with variable substitution</li>
 *     <li>Jsoup normalizes the HTML to valid XHTML using XML parser</li>
 *     <li>Flying Saucer's ITextRenderer converts XHTML to PDF with CSS styling</li>
 * </ol>
 * <p>
 * The service supports multi-page PDF generation through varargs model parameters,
 * allowing multiple data models to be rendered as separate pages within a single PDF document.
 * Localization is handled automatically via Spring's {@code LocaleContextHolder} integration.
 * 
 *
 * <b>Library Integrations</b>
 * <p><b>Thymeleaf TemplateEngine:</b> Renders templates to HTML with variable substitution
 * from model maps. All templates receive a {@code baseUrl} variable for absolute resource URLs.</p>
 * <p><b>Jsoup with XML Parser:</b> Normalizes HTML output to XHTML format required by
 * the PDF renderer, ensuring well-formed XML structure.</p>
 * <p><b>Flying Saucer ITextRenderer:</b> Converts XHTML to PDF documents with CSS styling
 * support. Creates new renderer instances per generation call for thread safety.</p>
 * <p><b>Spring Framework:</b> Provides {@code @Service} bean registration, {@code @Value}
 * configuration injection, and {@code @Inject} dependency management.</p>
 *
 * <b>Configuration Properties</b>
 * <p>
 * The PDF generation service uses the following configurable properties:
 * 
 * <ul>
 *     <li><b>base.url</b> (default: {@code http://localhost:8080}): Base URL injected into
 *     all template contexts for resolving absolute resource paths</li>
 *     <li><b>mail.from</b> (default: empty): Email sender identifier for email-related templates</li>
 *     <li><b>font.path</b> (default: {@code /fonts/arialuni.ttf}): Path to custom font file
 *     for Unicode character support in generated PDFs</li>
 * </ul>
 *
 * <b>Usage Patterns</b>
 * <p><b>Single-page PDF generation:</b></p>
 * <pre>
 * byte[] pdf = pdfConstructor.writeDocumentToByteArray("invoice", modelMap);
 * </pre>
 * <p><b>Multi-page PDF with multiple models:</b></p>
 * <pre>
 * pdfConstructor.writeDocumentToOutputStream("report", outputStream, model1, model2);
 * </pre>
 * <p><b>Streaming PDF to HTTP response:</b></p>
 * <pre>
 * pdfConstructor.writeDocumentToOutputStream("template", response.getOutputStream(), data);
 * </pre>
 *
 * <b>CSS Styling Considerations</b>
 * <p>
 * CSS styles must be XHTML-compatible for Flying Saucer rendering. The renderer supports
 * a subset of CSS 2.1 specifications. Images and fonts should be referenced as embedded
 * resources or absolute URLs. Custom font embedding may be required for international
 * character support beyond standard Latin character sets.
 * 
 *
 * <b>Thread Safety</b>
 * <p>
 * The PDF generation service is designed for thread-safe operation. A new {@code ITextRenderer}
 * instance is created for each PDF generation call, ensuring no state sharing between
 * concurrent requests. The injected {@code TemplateEngine} is thread-safe per Thymeleaf's
 * design, allowing safe concurrent template rendering.
 * 
 *
 * <b>Error Handling</b>
 * <p>
 * Exceptions during PDF generation are caught and logged using the request-scoped logging
 * component. Errors are not propagated to callers; instead, the service returns control
 * after logging. Callers should monitor logs for generation failure detection. Output
 * streams are always closed in finally blocks to prevent resource leaks.
 * 
 *
 * <b>Integration with Other Modules</b>
 * <p>
 * This package depends on:
 * 
 * <ul>
 *     <li>{@code com.openkoda.core.tracker} for request-scoped logging with correlation IDs</li>
 *     <li>{@code com.openkoda.core.configuration} for Thymeleaf template resolution</li>
 * </ul>
 * <p>
 * It is used by:
 * 
 * <ul>
 *     <li>Report generation features in the application</li>
 *     <li>Document export controllers for invoice and receipt printing</li>
 *     <li>Email attachment services for PDF document delivery</li>
 * </ul>
 *
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.service.pdf;
