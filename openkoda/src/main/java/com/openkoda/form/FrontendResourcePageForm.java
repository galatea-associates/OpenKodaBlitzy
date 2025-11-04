/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, 
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice 
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS 
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES 
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.form;

import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.helper.UrlHelper;
import com.openkoda.dto.FrontendResourcePageDto;
import com.openkoda.dto.file.FileDto;
import com.openkoda.model.component.FrontendResource;
import com.openkoda.model.file.File;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;

import java.util.regex.Pattern;

/**
 * Form adapter for frontend resource pages with URL-path validation and file URL generation.
 * <p>
 * Extends {@link FrontendResourceForm} to add URL-path validation rules ensuring page names
 * follow the pattern [a-z\-\/0-9]+ without leading slashes. Provides a static helper method
 * {@link #toFileDto(File)} for converting File entities to FileDto with URL generation via
 * {@link UrlHelper#getFileURL(File)}.
 * 
 * <p>
 * This form is specifically designed for HTML page frontend resources with sitemap inclusion.
 * The validate method enforces strict URL-path formatting rules: lowercase letters, hyphens,
 * forward slashes, and digits only, with no leading slash allowed.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendResourceForm
 * @see FrontendResourcePageDto
 * @see UrlHelper#getFileURL(File)
 */
public class FrontendResourcePageForm extends FrontendResourceForm<FrontendResourcePageDto> implements ReadableCode {

    /**
     * Default constructor initializing the form with frontend resource page form mapping definition.
     * <p>
     * Uses {@link FrontendMappingDefinitions#frontendResourcePageForm} to configure form fields
     * and validation rules specific to HTML page frontend resources.
     * 
     */
    public FrontendResourcePageForm() {
        super(FrontendMappingDefinitions.frontendResourcePageForm);
    }

    /**
     * Constructor initializing the form with organization context and existing frontend resource entity.
     * <p>
     * Creates a new {@link FrontendResourcePageDto} and populates the form from the provided
     * frontend resource entity within the specified organization context. Uses the frontend
     * resource page form mapping definition for field configuration.
     * 
     *
     * @param organizationId the organization ID for tenant-scoped operations
     * @param frontendResource the existing frontend resource entity to populate from
     */
    public FrontendResourcePageForm(Long organizationId, FrontendResource frontendResource) {
        super(organizationId, new FrontendResourcePageDto(), frontendResource, FrontendMappingDefinitions.frontendResourcePageForm);
    }

    /**
     * Validates the frontend resource page name according to URL-path formatting rules.
     * <p>
     * Enforces the following validation rules on dto.name:
     * 
     * <ul>
     * <li>Name must not be blank (rejects with "not.empty" if blank)</li>
     * <li>Name must match pattern [a-z\-\/0-9]+ - lowercase letters, hyphens, forward slashes,
     * and digits only (rejects with "simple.path" if pattern mismatch)</li>
     * <li>Name must not start with a forward slash (rejects with "not.slash.prefix" if starts with /)</li>
     * </ul>
     * <p>
     * Delegates to parent {@link FrontendResourceForm#validate(BindingResult)} for additional
     * validation rules inherited from the base form.
     * 
     *
     * @param br the Spring BindingResult to collect validation errors
     * @return this form instance for fluent chaining
     */
    @Override
    public FrontendResourcePageForm validate(BindingResult br) {
        if (StringUtils.isBlank(dto.name)) {
            br.rejectValue("dto.name", "not.empty");
        }
        if (not(Pattern.matches("[a-z\\-\\/0-9]+", dto.name))) {
            br.rejectValue("dto.name", "simple.path");
        }
        if (StringUtils.startsWith(dto.name, "/")) {
            br.rejectValue("dto.name", "not.slash.prefix");
        }
        return this;
    }

    /**
     * Converts a File entity to FileDto with URL generation using UrlHelper.
     * <p>
     * Creates a {@link FileDto} populated with file metadata including id, organizationId,
     * filename, contentType, and a generated file URL via {@link UrlHelper#getFileURL(File)}.
     * This helper method is used to transform File entities into DTOs suitable for
     * frontend resource page representation.
     * 
     *
     * @param a the File entity to convert
     * @return FileDto with file metadata and generated URL
     * @see UrlHelper#getFileURL(File)
     * @see FileDto
     */
    public static FileDto toFileDto(File a) {
        return new FileDto(a.getId(), a.getOrganizationId(), a.getFilename(), a.getContentType(), UrlHelper.getFileURL(a));
    }

    /**
     * Populates the frontend resource entity from this form's data.
     * <p>
     * Sets the entity name using {@link #getSafeValue(Object, String)} with URL_PATH_ prefix,
     * configures the resource type as HTML, and enables sitemap inclusion. This method is
     * part of the form lifecycle's populateTo phase.
     * 
     *
     * @param entity the frontend resource entity to populate
     * @return the populated frontend resource entity
     */
    @Override
    protected FrontendResource populateTo(FrontendResource entity) {
        entity.setName(getSafeValue(entity.getName(), URL_PATH_));
        entity.setType(FrontendResource.Type.HTML);
        entity.setIncludeInSitemap(true);
        return entity;
    }
}