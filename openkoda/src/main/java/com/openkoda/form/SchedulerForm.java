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

package com.openkoda.form;

import com.openkoda.core.form.AbstractOrganizationRelatedEntityForm;
import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.dto.system.SchedulerDto;
import com.openkoda.model.component.Scheduler;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.validation.BindingResult;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Form adapter for validating and processing cron expression and event data for {@link Scheduler} entities.
 * <p>
 * This form extends {@link AbstractOrganizationRelatedEntityForm} to provide validation and data transfer
 * between the web layer and the {@link Scheduler} domain model. It validates the cron expression using
 * {@link CronSequenceGenerator#isValidExpression(String)} to ensure proper cron syntax, and validates that
 * eventData is present. Validation errors are recorded in the {@link BindingResult} with appropriate
 * error codes ('cron.invalid' for invalid cron expressions, 'not.empty' for blank fields).
 * </p>
 * <p>
 * The form lifecycle follows the standard pattern: populateFrom (entity to DTO), validate (business rules),
 * populateTo (DTO to entity). The populateTo method uses getSafeValue() to safely apply validated values
 * while preserving existing entity data when form fields are unchanged.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Scheduler
 * @see SchedulerDto
 * @see AbstractOrganizationRelatedEntityForm
 * @see CronSequenceGenerator
 */
public class SchedulerForm extends AbstractOrganizationRelatedEntityForm<SchedulerDto, Scheduler> {

    /**
     * Default constructor for creating an empty SchedulerForm.
     * <p>
     * Initializes the form with the predefined scheduler frontend mapping definition from
     * {@link FrontendMappingDefinitions#schedulerForm}. This constructor is typically used
     * when creating a new Scheduler entity via the web interface.
     * </p>
     */
    public SchedulerForm() {
        super(FrontendMappingDefinitions.schedulerForm);
    }

    /**
     * Constructs a SchedulerForm for editing an existing Scheduler entity within an organization context.
     * <p>
     * Creates a new {@link SchedulerDto} instance and initializes the form with the provided entity.
     * This constructor is typically used when loading an existing Scheduler for editing in the web interface.
     * </p>
     *
     * @param organizationId the organization ID to which this Scheduler belongs (tenant scope)
     * @param entity the existing Scheduler entity to populate the form from
     */
    public SchedulerForm(Long organizationId, Scheduler entity) {
        super(organizationId, new SchedulerDto(), entity, FrontendMappingDefinitions.schedulerForm);
    }

    /**
     * Full constructor providing complete control over form initialization with custom DTO and mapping definition.
     * <p>
     * This constructor allows for advanced customization scenarios where a pre-populated DTO or custom
     * frontend mapping definition is required. Useful for testing or specialized form processing workflows.
     * </p>
     *
     * @param organizationId the organization ID to which this Scheduler belongs (tenant scope)
     * @param dto the SchedulerDto containing form data (may be pre-populated)
     * @param entity the Scheduler entity to be edited or null for new entities
     * @param frontendMappingDefinition custom frontend mapping definition for form field rendering
     */
    public SchedulerForm(Long organizationId, SchedulerDto dto, Scheduler entity, FrontendMappingDefinition frontendMappingDefinition) {
        super(organizationId, dto, entity, frontendMappingDefinition);
    }

    /**
     * Populates this form's DTO with data from the provided Scheduler entity.
     * <p>
     * Transfers the following fields from the entity to the DTO:
     * cronExpression, eventData, organizationId, and onMasterOnly. This method implements
     * the entity-to-form direction of the form lifecycle, preparing the form for display
     * in the web interface with existing entity values.
     * </p>
     *
     * @param entity the Scheduler entity to populate from
     * @return this SchedulerForm instance for method chaining
     */
    @Override
    public SchedulerForm populateFrom(Scheduler entity) {
        dto.cronExpression = entity.getCronExpression();
        dto.eventData = entity.getEventData();
        dto.organizationId = entity.getOrganizationId();
        dto.onMasterOnly = entity.isOnMasterOnly();
        return this;
    }

    /**
     * Applies validated DTO values to the Scheduler entity using safe value resolution.
     * <p>
     * Transfers validated form data from the DTO to the entity, using {@code getSafeValue()} to handle
     * field-level changes. The getSafeValue method preserves the existing entity value when the form field
     * is unchanged, ensuring partial updates are handled correctly. Updates the following fields:
     * cronExpression, eventData, organizationId, and onMasterOnly.
     * </p>
     *
     * @param entity the Scheduler entity to populate (may be existing or new)
     * @return the populated Scheduler entity
     */
    @Override
    protected Scheduler populateTo(Scheduler entity) {

        entity.setCronExpression(getSafeValue(entity.getCronExpression(), CRON_EXPRESSION_));
        entity.setEventData(getSafeValue(entity.getEventData(), EVENT_DATA_));
        entity.setOrganizationId(getSafeValue(entity.getOrganizationId(), ORGANIZATION_ID_));
        entity.setOnMasterOnly(getSafeValue(entity.isOnMasterOnly(), ON_MASTER_ONLY_));

        return entity;
    }

    /**
     * Validates the form data according to Scheduler business rules.
     * <p>
     * Performs the following validations:
     * </p>
     * <ul>
     *   <li>Validates that cronExpression is not blank - rejects with error code 'not.empty' if blank</li>
     *   <li>Validates that eventData is not blank - rejects with error code 'not.empty' if blank</li>
     *   <li>Validates cron expression syntax using {@link CronSequenceGenerator#isValidExpression(String)} -
     *       rejects with error code 'not.valid' if the expression is invalid</li>
     * </ul>
     * <p>
     * Validation errors are recorded in the provided {@link BindingResult} and can be displayed to the user
     * via Spring's form error handling mechanism.
     * </p>
     *
     * @param br the BindingResult to record validation errors
     * @return this SchedulerForm instance for fluent method chaining
     */
    @Override
    public SchedulerForm validate(BindingResult br) {
        if(isBlank(dto.cronExpression)) { br.rejectValue("dto.cronExpression", "not.empty", defaultErrorMessage); }
        if(isBlank(dto.eventData)) { br.rejectValue("dto.eventData", "not.empty", defaultErrorMessage); }
        if(!CronSequenceGenerator.isValidExpression(dto.cronExpression)) { br.rejectValue("dto.cronExpression", "not" +
                        ".valid",
                defaultErrorMessage); }
        return this;
    }

}
