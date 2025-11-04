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

package com.openkoda.core.service;

import com.openkoda.core.exception.NotFoundException;
import com.openkoda.core.exception.ServerErrorException;
import com.openkoda.core.exception.UnauthorizedException;
import com.openkoda.core.flow.ValidationException;
import com.openkoda.core.form.AbstractEntityForm;
import com.openkoda.core.form.AbstractForm;
import com.openkoda.core.form.Form;
import com.openkoda.core.form.FrontendMappingFieldDefinition;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import com.openkoda.model.common.ComponentEntity;
import com.openkoda.model.common.LongIdEntity;
import com.openkoda.service.export.ComponentExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.function.Function;

import static com.openkoda.controller.common.URLConstants.CAPTCHA_VERIFIED;

/**
 * Form validation orchestration service coordinating field and form validators.
 * <p>
 * This service provides comprehensive validation workflows that integrate Spring's BindingResult
 * with custom field and form validators. It enforces security policies by protecting write-restricted
 * fields during entity population, verifies reCAPTCHA tokens via request attributes, and triggers
 * component export cleanup for updated ComponentEntity records.

 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Orchestrates field validators from FrontendMappingDefinition</li>
 * <li>Maps validation errors into BindingResult with field-specific rejection codes</li>
 * <li>Enforces reCAPTCHA verification via CAPTCHA_VERIFIED request attribute</li>
 * <li>Protects write-restricted fields by checking canWriteField privileges</li>
 * <li>Triggers ComponentExportService cleanup for updated ComponentEntity records</li>
 * <li>Provides assertNotNull helpers with HTTP status-based exception throwing</li>
 * </ul>

 * <p>
 * Usage example:
 * <pre>
 * validationService.validateAndPopulateToEntity(form, bindingResult, entity);
 * </pre>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @version 1.7.1
 * @since 1.7.1
 * @see AbstractForm
 * @see AbstractEntityForm
 * @see FrontendMappingFieldDefinition
 * @see BindingResult
 * @see ValidationException
 * @see ComponentExportService
 * @see RequestAttributes
 */

@Service
public class ValidationService implements LoggingComponentWithRequestId {

    /**
     * Component export service handling component export file cleanup on entity updates.
     * <p>
     * Autowired service triggered during validateAndPopulateToEntity operations to remove
     * exported files when ComponentEntity records are updated.

     */
    @Autowired
    ComponentExportService componentExportService;
    
    /**
     * Primary validation and population workflow with BindingResult support.
     * <p>
     * Validates the form using field and form validators, then populates the entity if validation succeeds.
     * On validation failure, prepares field read/write privileges and checks that no write-restricted fields
     * were modified. For ComponentEntity instances with non-null ID, triggers component export file cleanup
     * before entity population.

     * <p>
     * Workflow:
     * <ol>
     * <li>Calls validate() with BindingResult to execute all validators</li>
     * <li>On ValidationException, prepares field privileges and verifies write permissions</li>
     * <li>Iterates FieldErrors checking canWriteField() to prevent restricted field modification</li>
     * <li>Throws ValidationException with "Contact administrator" message if restricted write attempted</li>
     * <li>Triggers ComponentExportService cleanup for ComponentEntity with non-null ID</li>
     * <li>Delegates to form.populateToEntity(entity) to copy form data to entity</li>
     * </ol>

     * <p>
     * Warning: Write-restricted field protection only enforced during validation errors. Successful
     * validation with manipulated fields may bypass this check.

     *
     * @param form the AbstractEntityForm containing form data and validation rules
     * @param br BindingResult accumulating field-specific validation errors
     * @param entity the target LongIdEntity to populate with validated form data
     * @param <F> form type extending AbstractEntityForm
     * @param <E> entity type extending LongIdEntity
     * @param <D> DTO type used by the form
     * @return the populated entity with form data
     * @throws ValidationException if validation fails or restricted field write attempted
     */
    final public <F extends AbstractEntityForm<D, E>, E extends LongIdEntity, D> E validateAndPopulateToEntity(F form, BindingResult br, E entity) {
        try {
            validate(form, br);
        } catch (ValidationException ve) {
            form.prepareFieldsReadWritePrivileges(entity);
            if (br.hasErrors()) {
                for (FieldError fe : br.getFieldErrors()) {
                    String name = form.extractFieldName(fe.getField());
                    FrontendMappingFieldDefinition fd = form.getFrontendMappingDefinition().findField(name);
                    if (!form.canWriteField(fd)) {
                        throw new ValidationException("Contact the administrator");
                    }
                }
            }
            throw ve;
        }
       if(entity instanceof ComponentEntity && entity.getId() != null){
           componentExportService.removeExportedFilesIfRequired((ComponentEntity) entity);
       }
       return form.populateToEntity(entity);
    }

    /**
     * Core validation orchestration with BindingResult support.
     * <p>
     * Executes field validators, form validators, custom form validation, and reCAPTCHA verification.
     * Accumulates all validation errors into the provided BindingResult using field-specific rejection codes.
     * Throws ValidationException if any validation errors are detected or reCAPTCHA verification fails.

     * <p>
     * Validation sequence:
     * <ol>
     * <li>Iterates form.frontendMappingDefinition.fieldValidators applying each Tuple2&lt;FieldDefinition, Validator&gt;</li>
     * <li>Calls form.validateField() accumulating errors into BindingResult</li>
     * <li>Iterates form.frontendMappingDefinition.formValidators applying each Function&lt;Form, Map&lt;String, String&gt;&gt;</li>
     * <li>Rejects BindingResult fields using findField().getName(isMapDto()) for proper field name resolution</li>
     * <li>Calls form.validate() for custom form-level validation logic</li>
     * <li>Checks requiresReCaptcha() and isCaptchaVerified() throwing ValidationException if CAPTCHA required but not verified</li>
     * <li>Throws ValidationException if BindingResult.hasErrors() returns true</li>
     * </ol>

     * <p>
     * Note: CAPTCHA verification requires CAPTCHA_VERIFIED request attribute set by controller interceptor.

     *
     * @param form the AbstractForm containing validation rules and data
     * @param br BindingResult to accumulate field-specific validation errors
     * @param <T> form type extending AbstractForm
     * @return the validated form if all validations pass
     * @throws ValidationException if validation fails or reCAPTCHA verification required but not verified
     */
    final public <T extends AbstractForm> T validate(T form, BindingResult br) {
        debug("[validate]");
        for (Tuple2<FrontendMappingFieldDefinition, Function<Object, String>> fieldValidator : form.frontendMappingDefinition.fieldValidators) {
             form.validateField(fieldValidator.getT1(), fieldValidator.getT2(), br);
        }

        for (Function<? extends Form, Map<String, String>> formValidator : form.frontendMappingDefinition.formValidators) {
            Map<String, String> rejections = ((Function<T, Map<String, String>>) formValidator).apply(form);
            if (rejections == null) {
                continue;
            }
            for (Map.Entry<String, String> e : rejections.entrySet()) {
                String field = form.frontendMappingDefinition.findField(e.getKey()).getName(form.isMapDto());
                br.rejectValue(field, e.getValue());
            }
        }

        form.validate(br);
        if(form.requiresReCaptcha() && !isCaptchaVerified()){
            throw new ValidationException("Validation error");
        }
        if (br.hasErrors()) {
            throw new ValidationException("Validation error");
        }
        return form;
    }

    /**
     * Checks if reCAPTCHA verification has been completed for the current request.
     * <p>
     * Retrieves the CAPTCHA_VERIFIED attribute from RequestAttributes (scope 0 - request scope).
     * Returns false if no request context is available, which can occur in background threads
     * or asynchronous processing contexts. This method is used to enforce reCAPTCHA verification
     * on form submissions that require captcha protection.

     * <p>
     * Note: The CAPTCHA_VERIFIED attribute must be set by a controller interceptor or filter
     * after successful reCAPTCHA validation with the external reCAPTCHA service.

     *
     * @return true if CAPTCHA verification attribute is present and set to true, false otherwise
     */
    public boolean isCaptchaVerified(){
        if(RequestContextHolder.getRequestAttributes() != null){
            RequestAttributes attr = RequestContextHolder.getRequestAttributes();
            Object obj = attr.getAttribute(CAPTCHA_VERIFIED, 0);
            return (Boolean) obj;
        }else{
            return false;
        }
    }

    /**
     * Generic null-check helper that throws status-specific exceptions.
     * <p>
     * Validates that the provided object is not null, throwing an appropriate exception
     * based on the specified HTTP status code. This is useful for service layer validation
     * where specific HTTP status codes need to be returned to clients.

     * <p>
     * Exception mapping:
     * <ul>
     * <li>HttpStatus.NOT_FOUND → NotFoundException</li>
     * <li>HttpStatus.UNAUTHORIZED → UnauthorizedException</li>
     * <li>Other statuses → ServerErrorException</li>
     * </ul>

     *
     * @param obj the object to check for null
     * @param statusOnFail the HTTP status code to determine which exception to throw
     * @param <T> the type of the object being validated
     * @return the non-null object if validation passes
     * @throws NotFoundException if obj is null and statusOnFail is HttpStatus.NOT_FOUND
     * @throws UnauthorizedException if obj is null and statusOnFail is HttpStatus.UNAUTHORIZED
     * @throws ServerErrorException if obj is null and statusOnFail is any other status
     */
    public <T> T assertNotNull(T obj, HttpStatus statusOnFail) {
        if (obj == null) {
            switch (statusOnFail) {
                case NOT_FOUND -> throw new NotFoundException();
                case UNAUTHORIZED -> throw new UnauthorizedException();
                default -> throw new ServerErrorException();
            }
        }
        return obj;
    }
    
    /**
     * Convenience overload of assertNotNull that defaults to HttpStatus.NOT_FOUND.
     * <p>
     * This is the most common use case where a missing resource should result in a 404 Not Found response.
     * Delegates to assertNotNull(obj, HttpStatus.NOT_FOUND).

     *
     * @param obj the object to check for null
     * @param <T> the type of the object being validated
     * @return the non-null object if validation passes
     * @throws NotFoundException if obj is null
     */
    public <T> T assertNotNull(T obj) {
        return assertNotNull(obj, HttpStatus.NOT_FOUND);
    }

    /**
     * Validation and population workflow without BindingResult, returning boolean success indicator.
     * <p>
     * Validates the form using the boolean-returning validate(form) method, then populates the entity
     * only if validation succeeds. On ValidationException, prepares field read/write privileges before
     * rethrowing the exception to allow proper error handling by the caller.

     * <p>
     * Workflow:
     * <ol>
     * <li>Calls validate(form) boolean version to execute validators</li>
     * <li>On ValidationException, catches and prepares field privileges before rethrowing</li>
     * <li>If validation passes (isValid = true), calls form.populateToEntity(entity)</li>
     * <li>Returns boolean indicating validation success</li>
     * </ol>

     * <p>
     * This variant is useful when BindingResult is not needed and simple success/failure indication suffices.

     *
     * @param form the AbstractEntityForm containing form data and validation rules
     * @param entity the target LongIdEntity to populate with validated form data
     * @param <F> form type extending AbstractEntityForm
     * @param <E> entity type extending LongIdEntity
     * @param <D> DTO type used by the form
     * @return true if validation passed and entity was populated, false if validation failed
     * @throws ValidationException if validation fails (after preparing field privileges)
     */
    final public <F extends AbstractEntityForm<D, E>, E extends LongIdEntity, D> boolean validateAndPopulateToEntity(F form, E entity) {
        boolean isValid;
        try {
            isValid = validate(form);
        } catch (ValidationException ve) {
            form.prepareFieldsReadWritePrivileges(entity);
            throw ve;
        }
        if(isValid) {
            form.populateToEntity(entity);
        }
        return isValid;
    }

    /**
     * Boolean validation workflow without BindingResult support.
     * <p>
     * Executes field validators and form validators, returning false on first validation failure
     * (short-circuit behavior). Does not accumulate errors or throw exceptions - simply returns
     * success/failure indicator. This is useful for scenarios where detailed error information
     * is not required.

     * <p>
     * Validation sequence:
     * <ol>
     * <li>Iterates fieldValidators calling form.validateField() returning boolean</li>
     * <li>Returns false immediately on first failure (short-circuit)</li>
     * <li>Iterates formValidators checking for non-null rejection maps</li>
     * <li>Returns false if any validator returns rejections</li>
     * <li>Returns true if all validators pass</li>
     * </ol>

     * <p>
     * Note: This method skips the form.validate() extension method to only consider
     * validators defined in FrontendMappingDefinition.

     *
     * @param form the AbstractForm containing validation rules and data
     * @param <T> form type extending AbstractForm
     * @return true if all validations pass, false if any validation fails
     */
    final public <T extends AbstractForm> boolean validate(T form) {
        debug("[validate]");
        for (Tuple2<FrontendMappingFieldDefinition, Function<Object, String>> fieldValidator : form.frontendMappingDefinition.fieldValidators) {
            if(!form.validateField(fieldValidator.getT1(), fieldValidator.getT2())){
                return false;
            }
        }

        for (Function<? extends Form, Map<String, String>> formValidator : form.frontendMappingDefinition.formValidators) {
            Map<String, String> rejections = ((Function<T, Map<String, String>>) formValidator).apply(form);
            if(rejections != null){
                return false;
            }

        }
        return true;
        //form.validate(br); -- skip validate method for the form extension for now (only validators in FrontendMappingDefinition are considered)
    }
}
