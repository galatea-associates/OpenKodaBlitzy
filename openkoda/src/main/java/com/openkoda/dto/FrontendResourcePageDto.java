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

package com.openkoda.dto;

import com.openkoda.dto.system.FrontendResourceDto;

/**
 * Specialized subtype of FrontendResourceDto for page-scoped frontend resources.
 * <p>
 * This class serves as a marker type with no additional fields or behavior beyond
 * what is inherited from FrontendResourceDto. It enables type differentiation in CMS
 * and frontend resource management systems, allowing controllers and services to
 * distinguish page resources from other resource types such as snippets or templates.
 * 
 * <p>
 * The type distinction is useful for routing, filtering, and applying page-specific
 * business rules without adding runtime overhead or additional data fields.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResourceDto
 */
public class FrontendResourcePageDto extends FrontendResourceDto {
}