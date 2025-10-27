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

package com.openkoda.core.customisation;

import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.core.repository.common.ScopedSecureRepository;

/**
 * Immutable data transfer object that atomically bundles a frontend mapping definition
 * with its corresponding repository for data access operations.
 * <p>
 * This record pairs form metadata ({@link FrontendMappingDefinition}) with a scoped secure
 * repository ({@link ScopedSecureRepository}) to create a complete mapping registration unit.
 * The atomic pairing ensures that form definitions are always registered with their
 * corresponding data access layer, preventing misconfiguration where a form might be
 * registered without an appropriate repository.
 * </p>
 * <p>
 * Instances of this record are registered in {@link FrontendMappingMap} for lookup operations
 * and are typically created during module initialization via
 * {@link CustomisationService#registerFrontendMapping}. The registry uses the mapping name
 * from {@code definition.name} as the key for retrieval.
 * </p>
 * <p>
 * As a Java record, this class is inherently immutable and thread-safe. All fields are
 * final and the canonical constructor performs defensive initialization. Records provide
 * automatic implementations of {@code equals()}, {@code hashCode()}, and {@code toString()}
 * based on component values.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * FrontendMapping mapping = new FrontendMapping(formDefinition, secureRepository);
 * customisationService.registerFrontendMapping(mapping);
 * }</pre>
 * </p>
 *
 * @param definition the frontend mapping definition containing form metadata, field specifications,
 *                   validation rules, and UI rendering instructions. Must not be null.
 * @param repository the scoped secure repository providing privilege-enforced data access operations
 *                   for the entity associated with this mapping. Must not be null.
 *
 * @see FrontendMappingMap
 * @see CustomisationService#registerFrontendMapping(FrontendMapping)
 * @see FrontendMappingDefinition
 * @see ScopedSecureRepository
 * @since 1.7.1
 * @author OpenKoda Team
 */
public record FrontendMapping(FrontendMappingDefinition definition, ScopedSecureRepository repository) { }
