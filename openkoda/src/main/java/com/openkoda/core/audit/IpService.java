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

package com.openkoda.core.audit;

import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * Utility service for extracting client IP addresses from HTTP requests with proxy support for audit logging.
 * <p>
 * Extracts client IP addresses from incoming HTTP requests, honoring X-Forwarded-For headers for proxied traffic.
 * Used by audit trail system to record client IPs in Audit entities. Provides IP allowlist validation for access control.
 * Handles both direct connections and reverse proxy scenarios.
 * </p>
 * <p>
 * Implementation note: Contains redundant double-call to getAddressForProxiedRequest in getIpFromRequest method at line 79.
 * </p>
 * <p>
 * Thread-safety: Stateless service, thread-safe.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see PropertyChangeListener
 */
@Service
public class IpService implements LoggingComponentWithRequestId {

   /**
    * This method looks for current client ip. Should work with proxies. Please
    * note that it might not work correctly, as it depends on correct behavior
    * of user browser and proxy servers. If current thread is not bound to
    * client request, returns null.
    *
    * @return Client IP address from current request context, or null if not in request scope.
    */
   public String getCurrentUserIpAddress() {
      debug( "[getCurrentUserIpAddress]" );
      ServletRequestAttributes currentRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if ( null != currentRequestAttributes ) {
         HttpServletRequest currentRequest = currentRequestAttributes.getRequest();
         return getIpFromRequest( currentRequest );
      }
      return null;
   }

   /**
    * Validates whether the request's client IP is in the allowed IPs list.
    * <p>
    * Note: Tokens are not trimmed, CIDR ranges not supported.
    * </p>
    *
    * @param allowedIps Comma-separated list of allowed IP addresses, or blank to allow all.
    * @param request HTTP request containing client IP.
    * @return true if IP is allowed or allowedIps is blank, false otherwise.
    */
   public boolean checkIPAllowed(String allowedIps, HttpServletRequest request) {
      return checkIPAllowed(allowedIps, getIpFromRequest(request));
   }
   
   /**
    * Validates whether the given IP address is in the allowed IPs list.
    *
    * @param allowedIps Comma-separated list of allowed IP addresses, or blank to allow all.
    * @param requestIP Client IP address to validate.
    * @return true if IP is allowed or allowedIps is blank, false otherwise.
    */
   private boolean checkIPAllowed(String allowedIps, String requestIP){
      debug("[checkIPAllowed] allowedIps: {} requestIP: {}", allowedIps, requestIP);
      return StringUtils.isBlank(allowedIps) || Arrays.asList(allowedIps.split(",")).contains(requestIP);
   }
   /**
    * This method returns ip address of a client. Works for connections with
    * proxies.
    * <p>
    * Note: Calls getAddressForProxiedRequest twice (line 79) - optimization opportunity.
    * </p>
    * 
    * @param request HTTP request.
    * @return Client IP address from X-Forwarded-For header if proxied, otherwise from RemoteAddr.
    */
   private String getIpFromRequest(HttpServletRequest request) {
      debug( "[getIpFromRequest] {}" , request );
      return null != getAddressForProxiedRequest( request ) ? getAddressForProxiedRequest( request ) : getAddressForNotProxiedRequest( request );
   }


   /**
    * Extracts client's IP from "X-Forwarded-For" header from the request.
    * Useful for requests behind reverse proxy.
    * <p>
    * Returns leftmost token from comma-separated list per standard proxy behavior.
    * </p>
    *
    * @param request HTTP request with potential X-Forwarded-For header.
    * @return Leftmost IP from X-Forwarded-For header, or null if header absent/empty.
    */
   private String getAddressForProxiedRequest(HttpServletRequest request) {
      debug( "[getAddressForProxiedRequest] {}" , request );
      String forwardedForIp = request.getHeader("X-Forwarded-For");
      return  forwardedForIp != null && !forwardedForIp.equals("") && !forwardedForIp.equals("-") ? StringUtils.substringBefore(forwardedForIp, ",") : null;
   }

   /**
    * Extracts client's IP from the request.
    * For application behind reverse proxy it's usually localhost, see {@link #getAddressForProxiedRequest}
    *
    * @param request HTTP request.
    * @return Client IP from request.getRemoteAddr(), typically localhost if behind reverse proxy.
    */
   private String getAddressForNotProxiedRequest(HttpServletRequest request) {
      debug( "[getAddressForNotProxiedRequest] {}" , request );
      return request.getRemoteAddr();
   }
}
