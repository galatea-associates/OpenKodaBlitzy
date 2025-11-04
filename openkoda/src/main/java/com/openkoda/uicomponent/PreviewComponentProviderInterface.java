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

package com.openkoda.uicomponent;

/**
 * Marker interface for preview mode component provider registration and dependency injection.
 * <p>
 * Empty interface used for type-based Spring bean selection and autowiring. Implementations provide
 * restricted service access during development/preview mode. Used by JsFlowRunner.runPreviewFlow to
 * inject preview-mode services instead of live services.
 * 
 * <p>
 * Enables safe flow preview execution without side effects (no actual emails sent, no database writes).
 * Spring @Autowired(required=false) allows graceful degradation when preview mode not configured.
 * 
 * <p>
 * Use case: Form designers can preview JavaScript flows without triggering real email sends or external API calls.
 * 
 * <p>
 * Note: No methods defined - purely for type identification and DI selection.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see JsFlowRunner
 * // LiveComponentProvider
 */
public interface PreviewComponentProviderInterface {

}
