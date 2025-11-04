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

package com.openkoda.core.security;

import com.openkoda.model.authentication.ApiKey;
import com.openkoda.model.authentication.LoginAndPassword;
import com.openkoda.service.user.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring lifecycle bootstrap component that initializes BCrypt password encoding and distributes
 * encoder instances to domain classes and services.
 * <p>
 * This class extends {@link BCryptPasswordEncoder} from Spring Security to serve as the primary
 * password encoder for the OpenKoda application. It centralizes password encoder configuration
 * and registration via the {@link PostConstruct} lifecycle callback, avoiding circular Spring
 * dependency issues that would occur with traditional dependency injection.
 * 
 * <p>
 * During initialization, this component distributes password encoder instances to static setters
 * on domain classes and services: {@link LoginAndPassword#setPasswordEncoderOnce},
 * {@link UserService#setPasswordEncoderOnce}, and {@link ApiKey#setPasswordEncoderOnce}.
 * The {@link Component} stereotype registers this as a Spring bean, ensuring single initialization
 * during application startup.
 * 
 * <p>
 * Example usage pattern: after initialization, {@code LoginAndPassword.setPasswordEncoderOnce()}
 * receives a {@link DelegatingPasswordEncoder} with "bcrypt" as the default algorithm, enabling
 * password validation via {@code password.matches(rawPassword)}.
 * 
 *
 * @see LoginAndPassword#setPasswordEncoderOnce
 * @see UserService#setPasswordEncoderOnce
 * @see ApiKey#setPasswordEncoderOnce
 * @see BCryptPasswordEncoder
 * @since 1.7.1
 * @author OpenKoda Team
 */
@Component
public class ApplicationAwarePasswordEncoder extends BCryptPasswordEncoder {

    /**
     * Initializes and distributes BCrypt password encoders to domain classes and services.
     * <p>
     * This {@link PostConstruct} lifecycle callback executes after Spring bean construction
     * and performs the following initialization sequence:
     * 
     * <ol>
     * <li>Creates a {@link HashMap} of {@link PasswordEncoder} instances with "bcrypt" key
     * mapping to this {@link BCryptPasswordEncoder}</li>
     * <li>Constructs a {@link DelegatingPasswordEncoder} with "bcrypt" as default algorithm
     * and the encoders map</li>
     * <li>Sets this {@link BCryptPasswordEncoder} as default for matches() operations to
     * provide backward compatibility for passwords without {bcrypt} prefix</li>
     * <li>Calls {@link LoginAndPassword#setPasswordEncoderOnce} to enable password validation
     * on LoginAndPassword entity instances</li>
     * <li>Calls {@link UserService#setPasswordEncoderOnce} to enable password hashing and
     * verification in UserService operations</li>
     * <li>Calls {@link ApiKey#setPasswordEncoderOnce} to enable API key token hashing</li>
     * </ol>
     * <p>
     * The {@link DelegatingPasswordEncoder} stores encoded passwords in the format
     * {@code {bcrypt}$2a$10$...} with an algorithm prefix, enabling future migration to
     * alternative password hashing algorithms.
     * 
     * <p>
     * Thread-safety: The {@link PostConstruct} annotation guarantees single execution per bean
     * lifecycle. The static setters use a one-time assignment pattern to prevent multiple
     * initialization attempts.
     * 
     *
     * @throws IllegalStateException if called multiple times (enforced by setPasswordEncoderOnce
     * methods in target classes)
     */
    @PostConstruct void init () {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", this);
        DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder(
                "bcrypt", encoders);
        passwordEncoder.setDefaultPasswordEncoderForMatches(this);
        LoginAndPassword.setPasswordEncoderOnce(passwordEncoder);
        UserService.setPasswordEncoderOnce(passwordEncoder);
        ApiKey.setPasswordEncoderOnce(this);
    }

}
