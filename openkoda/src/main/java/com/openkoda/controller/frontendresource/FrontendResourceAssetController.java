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

package com.openkoda.controller.frontendresource;

import com.openkoda.controller.file.AbstractFileController;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.Privilege;
import com.openkoda.model.file.File;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller for serving static frontend assets (CSS, JavaScript, images, fonts).
 * <p>
 * Handles asset requests from FrontendResource configurations. Serves files from filesystem 
 * or database storage with appropriate content-types and caching headers. Supports versioned 
 * assets for browser cache invalidation. Routes requests under /assets with dual access patterns:
 * public access for published assets and privileged access for HTML preview.
 * </p>
 * <p>
 * This controller implements privilege-based access control where assets can be served publicly
 * (for published frontend resources) or with user privilege verification (for preview mode).
 * Content-type detection is performed based on file extension (.css, .js, .png, etc.) and
 * appropriate caching headers are set for immutable assets.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see File
 * @see AbstractFileController
 * @see HasSecurityRules
 */
@Controller
public class FrontendResourceAssetController extends AbstractFileController implements HasSecurityRules {

    /**
     * Serves static asset file with content-type detection and privilege-based access control.
     * <p>
     * This method handles dual routing patterns for frontend resource assets:
     * </p>
     * <ul>
     *   <li><b>Public access</b>: FILE_ASSET + {id}/* - Serves only files marked as public 
     *       (publicFile=true), suitable for published frontend resources</li>
     *   <li><b>Privileged preview</b>: _HTML + FILE_ASSET + {id}/* - Serves any file if the 
     *       user has isUser privilege, used for HTML preview in development/admin contexts</li>
     * </ul>
     * <p>
     * <b>Content-Type Detection:</b> Automatically sets appropriate MIME types based on file 
     * extension (.css → text/css, .js → application/javascript, .png → image/png, etc.).
     * </p>
     * <p>
     * <b>Caching Behavior:</b> Sets Cache-Control: max-age=31536000 for immutable assets to 
     * optimize browser caching. Versioned assets enable cache invalidation through URL changes.
     * </p>
     * <p>
     * <b>Security Logic:</b> If request URI contains _HTML prefix and user has 
     * {@link Privilege#isUser} privilege, unrestricted repository access is used 
     * (unsecure.file.findOne). Otherwise, only public files are served 
     * (findByIdAndPublicFileTrue).
     * </p>
     * <p>
     * Returns HTTP 404 (SC_NOT_FOUND) if the file does not exist or access is denied.
     * </p>
     *
     * @param frontendResourceFileId ID of the FrontendResource file to serve. Used to lookup 
     *                               file in repository by ID with optional public file flag check
     * @param download If true, serves file as attachment with Content-Disposition header forcing 
     *                 download. If false, serves inline for browser rendering. Defaults to false
     * @param request HttpServletRequest used to determine request URI for privilege check. 
     *                If URI contains _HTML prefix and user has isUser privilege, unrestricted 
     *                repository access is used
     * @param response HttpServletResponse for streaming file content and setting headers 
     *                 (Content-Type, Cache-Control, Content-Disposition)
     * @throws IOException If an I/O error occurs during file content streaming to response output stream
     * @throws SQLException If a database access error occurs while retrieving file content from 
     *                      database storage
     */
    @Transactional(readOnly = true)
    @GetMapping(value = {FILE_ASSET + "{frontendResourceFileId:" + NUMBERREGEX + "$}/*",
            _HTML + FILE_ASSET + "{frontendResourceFileId:" + NUMBERREGEX + "$}/*"})
    public void getFrontendResourceAsset(@PathVariable("frontendResourceFileId") Long frontendResourceFileId,
                                         @RequestParam(name = "dl", required = false, defaultValue = "false") boolean download,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws IOException, SQLException {
        debug("[getFrontendResourceAsset] frontendResourceFileId {}", frontendResourceFileId);
        File f = request.getRequestURI().contains(_HTML) && hasGlobalPrivilege(Privilege.isUser) ?
                repositories.unsecure.file.findOne(frontendResourceFileId)
                : repositories.unsecure.file.findByIdAndPublicFileTrue(frontendResourceFileId);
        if (f == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        services.file.getFileContentAndPrepareResponse(f, download, true, response);
    }
}
