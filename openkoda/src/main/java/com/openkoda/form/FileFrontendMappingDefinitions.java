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

import com.openkoda.core.form.FrontendMappingDefinition;
import com.openkoda.model.PrivilegeBase;

import static com.openkoda.core.form.FrontendMappingDefinition.createFrontendMappingDefinition;
import static com.openkoda.core.security.HasSecurityRules.*;
import static com.openkoda.model.Privilege.readOrgData;
import static com.openkoda.model.PrivilegeNames._readOrgData;

/**
 * Centralized file form mapping definitions and security constants.
 * <p>
 * Provides pre-built file form {@link FrontendMappingDefinition} with field builders for filename, 
 * content type, and public file checkbox. Includes security expression constants using organization 
 * privileges for read and write access control. Used by FileForm implementations and file management 
 * controllers to enforce consistent file entity mapping and privilege-based security checks.
 * </p>
 * <p>
 * The {@link #fileForm} definition creates a form with text fields for filename and content type, 
 * plus a checkbox for public file status. Security expressions combine attribute editing checks 
 * with organization-scoped privilege predicates to control form access.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see FrontendMappingDefinition
 * @see PrivilegeBase
 * @see com.openkoda.model.Privilege
 */
public interface FileFrontendMappingDefinitions {

    /**
     * Form identifier constant for file entity forms.
     * <p>
     * Used as the form name when creating the {@link #fileForm} FrontendMappingDefinition.
     * </p>
     */
    String FILE_FORM = "FileForm";
    
    /**
     * Field name constant for file name property.
     * <p>
     * Maps to the filename attribute in file entity forms.
     * </p>
     */
    String FILENAME_ = "filename";
    
    /**
     * Field name constant for content type property.
     * <p>
     * Maps to the contentType attribute (MIME type) in file entity forms.
     * </p>
     */
    String CONTENT_TYPE_ = "contentType";
    
    /**
     * Field name constant for file content property.
     * <p>
     * Maps to the content attribute in file entity forms.
     * </p>
     */
    String CONTENT_ = "content";
    
    /**
     * Field name constant for public file flag.
     * <p>
     * Maps to the publicFile boolean attribute controlling file visibility in forms.
     * </p>
     */
    String PUBLIC_FILE_ = "publicFile";

    /**
     * Read privilege required for file form access.
     * <p>
     * Set to {@code readOrgData} privilege, allowing users with organization data read 
     * permissions to view file forms.
     * </p>
     *
     * @see com.openkoda.model.Privilege#readOrgData
     */
    PrivilegeBase readPrivilege = readOrgData;
    
    /**
     * Write privilege required for file form modifications.
     * <p>
     * Set to {@code readOrgData} privilege, allowing users with organization data read 
     * permissions to modify file forms.
     * </p>
     *
     * @see com.openkoda.model.Privilege#readOrgData
     */
    PrivilegeBase writePrivilege = readOrgData;

    /**
     * String privilege name for read access security expressions.
     * <p>
     * Resolves to the string representation of {@code readOrgData} privilege name, 
     * used in SpEL security expressions for runtime privilege evaluation.
     * </p>
     *
     * @see com.openkoda.model.PrivilegeNames#_readOrgData
     */
    String _readPrivilege = _readOrgData;
    
    /**
     * String privilege name for write access security expressions.
     * <p>
     * Resolves to the string representation of {@code readOrgData} privilege name, 
     * used in SpEL security expressions for runtime privilege evaluation.
     * </p>
     *
     * @see com.openkoda.model.PrivilegeNames#_readOrgData
     */
    String _writePrivilege = _readOrgData;

    /**
     * SpEL security expression template for read access authorization.
     * <p>
     * Combines attribute editing check with organization privilege predicate to determine 
     * read access. Expression evaluates to true if user can edit attributes OR possesses 
     * the organization-scoped read privilege. Used in controller security annotations and 
     * form visibility rules.
     * </p>
     * <p>
     * Expression format: {@code "[[" + CHECK_CAN_EDIT_ATTRIBUTES + " or " + 
     * HAS_ORG_PRIVILEGE + "(" + readPrivilege + ")" + "]]"}
     * </p>
     *
     * @see com.openkoda.core.security.HasSecurityRules#CHECK_CAN_EDIT_ATTRIBUTES
     * @see com.openkoda.core.security.HasSecurityRules#HAS_ORG_PRIVILEGE_OPEN
     */
    String CHECK_CAN_READ = BB_OPEN + CHECK_CAN_EDIT_ATTRIBUTES + OR + HAS_ORG_PRIVILEGE_OPEN + _readPrivilege + HAS_ORG_PRIVILEGE_CLOSE + BB_CLOSE;
    
    /**
     * SpEL security expression template for write access authorization.
     * <p>
     * Combines attribute editing check with organization privilege predicate to determine 
     * write access. Expression evaluates to true if user can edit attributes OR possesses 
     * the organization-scoped write privilege. Used in controller security annotations and 
     * form modification rules.
     * </p>
     * <p>
     * Expression format: {@code "[[" + CHECK_CAN_EDIT_ATTRIBUTES + " or " + 
     * HAS_ORG_PRIVILEGE + "(" + writePrivilege + ")" + "]]"}
     * </p>
     *
     * @see com.openkoda.core.security.HasSecurityRules#CHECK_CAN_EDIT_ATTRIBUTES
     * @see com.openkoda.core.security.HasSecurityRules#HAS_ORG_PRIVILEGE_OPEN
     */
    String CHECK_CAN_WRITE = BB_OPEN + CHECK_CAN_EDIT_ATTRIBUTES + OR + HAS_ORG_PRIVILEGE_OPEN + _writePrivilege + HAS_ORG_PRIVILEGE_CLOSE + BB_CLOSE;

    /**
     * Pre-built FrontendMappingDefinition for file entity forms.
     * <p>
     * Creates a form with three fields: filename text input, publicFile checkbox, and 
     * contentType text input. Form access is controlled by {@link #readPrivilege} and 
     * {@link #writePrivilege}, both set to {@code readOrgData} organization privilege.
     * </p>
     * <p>
     * Field definitions:
     * </p>
     * <ul>
     *   <li>{@code text(FILENAME_)} - Text field for file name entry</li>
     *   <li>{@code checkbox(PUBLIC_FILE_)} - Checkbox controlling public visibility</li>
     *   <li>{@code text(CONTENT_TYPE_)} - Text field for MIME type specification</li>
     * </ul>
     * <p>
     * Usage example:
     * </p>
     * <pre>{@code
     * FrontendMappingDefinition form = FileFrontendMappingDefinitions.fileForm;
     * form.createNewOrEditForm(organizationId, fileEntity);
     * }</pre>
     *
     * @see FrontendMappingDefinition#createFrontendMappingDefinition
     * @see #FILE_FORM
     * @see #readPrivilege
     * @see #writePrivilege
     */
    FrontendMappingDefinition fileForm = createFrontendMappingDefinition(FILE_FORM, readPrivilege, writePrivilege,
            a -> a  .text(FILENAME_)
                    .checkbox(PUBLIC_FILE_)
                    .text(CONTENT_TYPE_)
    );
}