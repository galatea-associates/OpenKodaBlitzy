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

import com.openkoda.model.task.HttpRequestTask;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Thin RestTemplate wrapper providing simplified HTTP client operations for external API calls.
 * <p>
 * This service exposes synchronous POST and GET methods with header customization for making
 * HTTP requests to external REST APIs. It provides a convenient abstraction over Spring's
 * RestTemplate for common use cases involving JSON payloads and custom headers.
 * </p>
 * <p>
 * Note: This service does not implement internal retry or timeout policies. Callers should
 * handle {@code RestClientException} for HTTP errors and implement their own retry logic
 * if needed.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * Map<String, String> headers = Map.of("Authorization", "Bearer token");
 * Map response = restClientService.get("https://api.example.com/data", headers);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see HttpRequestTask
 * @see org.springframework.web.client.RestTemplate
 * @see com.openkoda.service.GenericWebhookService
 * @see com.openkoda.integration.service.SlackService
 */
@Service
public class RestClientService {

    /**
     * RestTemplate instance for executing HTTP operations.
     * <p>
     * Note: This instance is not thread-safe across concurrent requests when state is modified.
     * The default RestTemplate instance provided by Spring uses SimpleClientHttpRequestFactory
     * which is thread-safe for read operations.
     * </p>
     */
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Extracts headers from HttpRequestTask and prepares HttpHeaders for REST requests.
     * <p>
     * This method delegates to the map-based overload {@link #prepareHttpHeaders(Map)}
     * after extracting the headers map from the task. Sets Content-Type to
     * APPLICATION_JSON_UTF8 by default.
     * </p>
     *
     * @param task the HTTP request task containing headers in its headersMap
     * @return HttpHeaders instance with Content-Type set to JSON and custom headers from task
     * @see #prepareHttpHeaders(Map)
     * @see HttpRequestTask#getHeadersMap()
     */
    public static HttpHeaders prepareHttpHeaders(HttpRequestTask task) {
        return prepareHttpHeaders(task.getHeadersMap());
    }

    /**
     * Constructs HttpHeaders with JSON content type and custom headers from map.
     * <p>
     * This method creates a new HttpHeaders instance, sets the Content-Type to
     * APPLICATION_JSON_UTF8, and iterates through the provided map entries to set
     * custom headers. Each entry key-value pair is added as an HTTP header.
     * </p>
     *
     * @param headers map of header names to header values to be added to the request
     * @return HttpHeaders instance configured with JSON content type and all provided custom headers
     */
    public static HttpHeaders prepareHttpHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            httpHeaders.set(e.getKey(), e.getValue());
        }
        return httpHeaders;
    }

    /**
     * Executes a synchronous HTTP POST request with JSON body and custom headers.
     * <p>
     * This method sends a POST request to the specified URL with the provided body
     * serialized as JSON and custom headers. The response body is deserialized into
     * a Map and returned. No automatic retry is performed on failure.
     * </p>
     *
     * @param url the target URL for the POST request
     * @param body the request body as a map to be serialized as JSON
     * @param headers custom HTTP headers to include in the request
     * @return the response body deserialized as a Map
     * @throws org.springframework.web.client.RestClientException if an HTTP error occurs or the request fails
     */
    public Map post(String url, Map<String, String> body, Map<String, String> headers) {
        HttpHeaders httpHeaders = prepareHttpHeaders(headers);
        HttpEntity<Map> httpEntity = new HttpEntity<>(body, httpHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);
        return response.getBody();
    }

    /**
     * Executes a synchronous HTTP GET request convenience method returning a Map.
     * <p>
     * This method delegates to the generic overload {@link #get(String, Map, Class)}
     * with Map.class as the result type, providing a convenient way to retrieve
     * response data as a Map without specifying the type parameter.
     * </p>
     *
     * @param url the target URL for the GET request
     * @param headers custom HTTP headers to include in the request
     * @return the response body deserialized as a Map
     * @throws org.springframework.web.client.RestClientException if an HTTP error occurs or the request fails
     * @see #get(String, Map, Class)
     */
    public Map get(String url, Map<String, String> headers) {
        return get(url, headers, Map.class);
    }

    /**
     * Executes a generic HTTP GET request with type-safe response deserialization.
     * <p>
     * This method provides flexible GET request execution using RestTemplate.exchange()
     * for maximum flexibility. The response body is deserialized into the specified
     * result type, allowing for type-safe response handling. No automatic retry is
     * performed on failure.
     * </p>
     *
     * @param <T> the type of the response body
     * @param url the target URL for the GET request
     * @param headers custom HTTP headers to include in the request
     * @param resultType the class object representing the expected response type
     * @return the response body deserialized as the specified type
     * @throws org.springframework.web.client.RestClientException if an HTTP error occurs or the request fails
     */
    public <T> T get(String url, Map<String, String> headers, Class<T> resultType) {
        HttpHeaders httpHeaders = prepareHttpHeaders(headers);
        HttpEntity<Map> httpEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, resultType);
        return response.getBody();
    }


}
