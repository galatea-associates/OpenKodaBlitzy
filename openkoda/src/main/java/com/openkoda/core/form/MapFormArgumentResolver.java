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

import com.openkoda.controller.HtmlCRUDControllerConfigurationMap;
import com.openkoda.core.customisation.FrontendMapping;
import com.openkoda.core.customisation.FrontendMappingMap;
import com.openkoda.core.helper.ReadableCode;
import com.openkoda.core.helper.UrlHelper;
import com.openkoda.core.multitenancy.TenantResolver;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.common.SearchableOrganizationRelatedEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestParameterPropertyValues;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

/**
 * Spring MVC argument resolver for {@link AbstractOrganizationRelatedEntityForm} parameters.
 * <p>
 * This resolver automatically creates and binds form instances in controller methods.
 * It resolves form instances using {@link CRUDControllerConfiguration} lookup via
 * {@link UrlHelper} or {@link FrontendMappingMap}, calls {@code createNewForm} or
 * {@code createNewEntity}, then binds HTTP request parameters to the form using
 * {@link WebDataBinderFactory} and attaches {@link BindingResult} to the
 * {@link ModelAndViewContainer}.
 * <p>
 * The resolver implements a two-phase resolution strategy:
 * <ol>
 * <li>URL-based lookup: Extracts mapping key from request URL via {@code getMappingKeyOrNull},
 * retrieves {@link CRUDControllerConfiguration} from {@code htmlCRUDControllerConfigurationMap},
 * and creates form using {@code createNewForm()}</li>
 * <li>Parameter-based lookup: Reads {@code frontendMappingDefinition} parameter,
 * retrieves {@link FrontendMapping} from {@code frontendMappingMap}, builds dynamic form
 * configuration with {@link ReflectionBasedEntityForm}, extracts tenant context via
 * {@link TenantResolver}, and creates organization-scoped form</li>
 * </ol>
 * <p>
 * Example usage:
 * <pre>
 * {@code @RequestMapping("/settings/edit")}
 * public String edit(AbstractOrganizationRelatedEntityForm form) { ... }
 * </pre>
 *
 * @see AbstractOrganizationRelatedEntityForm
 * @see CRUDControllerConfiguration
 * @see MapEntityForm
 * @see HandlerMethodArgumentResolver
 * @since 1.7.1
 * @author OpenKoda Team
 */
public class MapFormArgumentResolver implements HandlerMethodArgumentResolver, HasSecurityRules, ReadableCode {

    /**
     * Map of URL-based CRUD controller configurations for form resolution.
     * Used in first phase of resolution strategy to look up configurations by mapping key.
     */
    private final HtmlCRUDControllerConfigurationMap htmlCRUDControllerConfigurationMap;
    
    /**
     * Map of frontend mapping definitions for parameter-based form resolution.
     * Used in second phase when {@code frontendMappingDefinition} parameter is present.
     */
    private final FrontendMappingMap frontendMappingMap;
    
    /**
     * Helper for extracting mapping keys from request URLs.
     * Enables URL-based configuration lookup in the first resolution phase.
     */
    private final UrlHelper urlHelper;

    /**
     * Creates a new MapFormArgumentResolver with required dependencies.
     *
     * @param htmlCRUDControllerConfigurationMap map of URL-based CRUD configurations
     * @param frontendMappingMap map of frontend mapping definitions
     * @param urlHelper helper for URL mapping key extraction
     */
    public MapFormArgumentResolver(HtmlCRUDControllerConfigurationMap htmlCRUDControllerConfigurationMap, FrontendMappingMap frontendMappingMap, UrlHelper urlHelper) {
        this.htmlCRUDControllerConfigurationMap = htmlCRUDControllerConfigurationMap;
        this.frontendMappingMap = frontendMappingMap;
        this.urlHelper = urlHelper;
    }

    /**
     * Determines if this resolver supports the given method parameter.
     * <p>
     * Returns {@code true} only when the parameter type is exactly
     * {@link AbstractOrganizationRelatedEntityForm}, enabling automatic form
     * resolution for organization-related entity forms in controller methods.
     * 
     *
     * @param parameter the method parameter to check
     * @return {@code true} if parameter type equals AbstractOrganizationRelatedEntityForm, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AbstractOrganizationRelatedEntityForm.class.equals(parameter.getParameterType());
    }

    /**
     * Resolves and binds an {@link AbstractOrganizationRelatedEntityForm} instance from the web request.
     * <p>
     * Implements a two-phase resolution strategy:
     * 
     * <ol>
     * <li><b>URL-based lookup</b>: Extracts mapping key via {@code urlHelper.getMappingKeyOrNull()},
     * retrieves {@link CRUDControllerConfiguration} from {@code htmlCRUDControllerConfigurationMap},
     * creates form using {@code conf.createNewForm()}, and binds request parameters</li>
     * <li><b>Parameter-based lookup</b>: Reads {@code frontendMappingDefinition} request parameter,
     * retrieves {@link FrontendMapping} from {@code frontendMappingMap}, builds configuration with
     * {@code CRUDControllerConfiguration.getBuilder()} using {@link ReflectionBasedEntityForm},
     * extracts tenant organization ID via {@link TenantResolver}, creates entity and form using
     * {@code conf.createNewEntity()} and {@code conf.createNewForm()}, and binds request parameters</li>
     * </ol>
     * <p>
     * Binding uses {@link ServletRequestParameterPropertyValues} to extract request parameters,
     * {@link WebDataBinderFactory} to create a binder, applies binding with {@code binder.bind()},
     * and stores {@link BindingResult} in the {@link ModelAndViewContainer}.
     * 
     *
     * @param parameter the method parameter to resolve
     * @param mavContainer the model and view container for storing binding results
     * @param webRequest the current web request containing parameters
     * @param binderFactory factory for creating web data binders
     * @return the resolved and bound form instance, or {@code null} if no configuration found
     * @throws Exception if form binding or resolution fails
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String mappingKey = urlHelper.getMappingKeyOrNull((HttpServletRequest) webRequest.getNativeRequest());

        AbstractOrganizationRelatedEntityForm form = null;
        if(mappingKey != null) {
            CRUDControllerConfiguration conf = htmlCRUDControllerConfigurationMap.get(mappingKey);
            if (conf != null) {
                form = conf.createNewForm();
                bindForm(mavContainer, webRequest, binderFactory, form);
                return form;
            }
        }
        String mappingDefinitionName = webRequest.getParameter("frontendMappingDefinition");
        if (StringUtils.isNotBlank(mappingDefinitionName)) {
            FrontendMapping frontendMapping = frontendMappingMap.get(mappingDefinitionName);
            if (frontendMapping != null) {
                CRUDControllerConfiguration conf = CRUDControllerConfiguration.getBuilder("form", frontendMapping.definition(), frontendMapping.repository(), ReflectionBasedEntityForm.class);
                Long orgId = TenantResolver.getTenantedResource().organizationId;
                SearchableOrganizationRelatedEntity entity = conf.createNewEntity(orgId);
                form = conf.createNewForm(orgId, entity);
                bindForm(mavContainer, webRequest, binderFactory, form);
                return form;
            }
        }
        return null;

//        throw new RuntimeException("Cannot resolve AbstractOrganizationRelatedEntityForm for url " + webRequest.getContextPath());
    }

    /**
     * Binds HTTP request parameters to the given form and stores binding results.
     * <p>
     * Uses {@link ServletRequestParameterPropertyValues} to extract request parameters,
     * creates a {@link WebDataBinder} via {@code binderFactory.createBinder()}, applies
     * parameter binding with {@code binder.bind()}, retrieves the {@link BindingResult},
     * and adds it to the {@link ModelAndViewContainer} for validation error handling.
     * 
     *
     * @param mavContainer the model and view container for storing binding results
     * @param webRequest the current web request containing form parameters
     * @param binderFactory factory for creating web data binders
     * @param form the form instance to bind parameters to
     * @throws Exception if parameter binding fails
     */
    private static void bindForm(ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory, AbstractOrganizationRelatedEntityForm form) throws Exception {
        ServletWebRequest swr = (ServletWebRequest) webRequest;
        PropertyValues pv = new ServletRequestParameterPropertyValues(swr.getRequest());
        WebDataBinder pn = binderFactory.createBinder(webRequest, form, "form");
        pn.bind(pv);

        BindingResult br = pn.getBindingResult();
        form.setBindingResult(br);
        Map<String, Object> bindingResultModel = br.getModel();
        mavContainer.removeAttributes(bindingResultModel);
        mavContainer.addAllAttributes(bindingResultModel);
    }
}