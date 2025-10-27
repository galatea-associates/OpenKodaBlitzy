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

package com.openkoda.repository;

import com.openkoda.model.common.SearchableRepositoryMetadata;
import com.openkoda.model.component.Form;
import org.springframework.stereotype.Repository;

import static com.openkoda.controller.common.URLConstants.FORM;

/**
 * Secure repository marker interface for Form entities with SearchableRepositoryMetadata.
 * <p>
 * Extends {@link SecureRepository} for privilege-enforced form operations. This interface
 * is annotated with {@link SearchableRepositoryMetadata} to enable dynamic repository discovery
 * by the SearchableRepositories system. The metadata configuration specifies the entity key
 * (FORM), description formula for search results, and the Form entity class for type safety.
 * </p>
 * <p>
 * As a secure repository, all query operations inherit privilege enforcement from the
 * {@link SecureRepository} contract, ensuring that form access respects the authenticated
 * user's privileges and organizational context.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see SecureRepository
 * @see Form
 * @see SearchableRepositoryMetadata
 */
@Repository
@SearchableRepositoryMetadata(
        entityKey = FORM,
        descriptionFormula = "(''||name)",
        entityClass = Form.class
)
public interface SecureFormRepository extends SecureRepository<Form> {


}
