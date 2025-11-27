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

package com.openkoda.core.form;

import com.openkoda.model.MapEntity;
import org.springframework.validation.BindingResult;

/**
 * Map-based form for handling MapEntity dynamic entities.
 * <p>
 * This form uses {@link OrganizationRelatedMap} DTO for flexible key-value storage
 * without predefined JavaBean structure. It supports dynamic entities where field
 * structure is determined at runtime from {@link FrontendMappingDefinition}.
 * <p>
 * The form stores arbitrary key-value pairs in the database with JSON serialization,
 * enabling dynamic data models without schema changes. Supports partial updates via
 * the {@code singleFieldToUpdate} mechanism for efficient single-field modifications.
 * <p>
 * Example usage:
 * <pre>{@code
 * MapEntityForm form = new MapEntityForm(mapping, orgId, entity);
 * form.populateFrom(entity);
 * form.populateTo(entity);
 * }</pre>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @see MapEntity
 * @see OrganizationRelatedMap
 * @see AbstractOrganizationRelatedEntityForm
 * @since 1.7.1
 */
public class MapEntityForm extends AbstractOrganizationRelatedEntityForm<OrganizationRelatedMap, MapEntity> {

    /**
     * Creates a new MapEntityForm for handling new entities.
     * <p>
     * This constructor is used when creating a new MapEntity without an existing
     * database entity. The form will use the provided mapping definition to
     * determine field structure at runtime.
     * 
     *
     * @param frontendMappingDefinition the frontend mapping definition that defines
     *                                  field structure and validation rules
     */
    public MapEntityForm(FrontendMappingDefinition frontendMappingDefinition) {
        super(frontendMappingDefinition);
    }

    /**
     * Creates a MapEntityForm for handling existing entities.
     * <p>
     * This constructor initializes the form with an existing MapEntity from the
     * database, along with its organization context. The form will populate its
     * internal {@link OrganizationRelatedMap} DTO from the entity's value map.
     * 
     *
     * @param frontendMappingDefinition the frontend mapping definition that defines
     *                                  field structure and validation rules
     * @param organizationId the organization ID for multi-tenant context
     * @param entity the existing MapEntity to populate from
     */
    public MapEntityForm(FrontendMappingDefinition frontendMappingDefinition, Long organizationId, MapEntity entity) {
        super(organizationId, new OrganizationRelatedMap(), entity, frontendMappingDefinition);
    }

    /**
     * Validates the form data.
     * <p>
     * This implementation returns null as validation is handled by the parent
     * class {@link AbstractOrganizationRelatedEntityForm}. The parent class
     * performs validation based on the {@link FrontendMappingDefinition}.
     * 
     *
     * @param br the binding result for capturing validation errors
     * @param <F> the form type
     * @return null, as validation is delegated to parent class
     */
    @Override
    public <F extends Form> F validate(BindingResult br) {
        return null;
    }

    /**
     * Populates the form from a MapEntity.
     * <p>
     * Retrieves the entity's value map directly via {@link MapEntity#getValueAsMap()}
     * without using reflection. This approach is more efficient for dynamic entities
     * with runtime-defined field structure.
     * 
     *
     * @param entity the MapEntity to populate from
     * @return this form instance for method chaining
     */
    @Override
    protected MapEntityForm populateFrom(MapEntity entity) {
        dto = entity.getValueAsMap();
        return this;
    }

    /**
     * Populates a MapEntity from the form data.
     * <p>
     * Supports two update modes:
     * 
     * <ul>
     * <li><b>Partial update</b>: When {@code singleFieldToUpdate} is set, updates only
     * that field via {@link MapEntity#getValueAsMap()}.put() followed by
     * {@link MapEntity#updateValueFromMap()} to persist the change.</li>
     * <li><b>Full update</b>: When {@code singleFieldToUpdate} is null, replaces the
     * entire value map via {@link MapEntity#setValueAsMap(OrganizationRelatedMap)}.</li>
     * </ul>
     *
     * @param entity the MapEntity to populate to
     * @return the populated entity
     */
    @Override
    protected MapEntity populateTo(MapEntity entity) {
        if (singleFieldToUpdate != null) {
            entity.getValueAsMap().put(singleFieldToUpdate, dto.get(singleFieldToUpdate));
            entity.updateValueFromMap();
        } else {
            entity.setValueAsMap(dto);
        }
        return entity;
    }

    /**
     * Indicates this form uses map-based DTO binding.
     * <p>
     * Returns {@code true} to enable {@code dto[field]} binding style in HTML forms,
     * which is required for dynamic entities with runtime-defined fields. This allows
     * Thymeleaf templates to bind form inputs to map keys rather than JavaBean properties.
     * 
     *
     * @return true, indicating map-based DTO binding is used
     */
    @Override
    public boolean isMapDto() {
        return true;
    }

}