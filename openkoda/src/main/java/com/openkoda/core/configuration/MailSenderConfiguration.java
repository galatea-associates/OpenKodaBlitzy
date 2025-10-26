package com.openkoda.core.configuration;

import java.util.Map;
import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.openkoda.core.service.email.EmailConfigJavaMailSender;

/**
 * Spring configuration class for JavaMail integration and SMTP mail sender setup.
 * <p>
 * This configuration is activated conditionally based on the presence of the {@code spring.mail.host}
 * property. It provides a production-ready JavaMail sender implementation configured with SMTP
 * credentials, connection settings, and additional mail properties from Spring Boot's auto-configuration.
 * </p>
 * <p>
 * The class is annotated with {@code @Configuration(proxyBeanMethods=false)} for optimized bean creation
 * without CGLIB proxies, improving startup performance. It binds {@link MailProperties} from Spring Boot's
 * mail auto-configuration and constructs a {@link JavaMailSenderImpl} bean with SMTP host, port, username,
 * password, protocol, default encoding, and additional properties from {@code spring.mail.properties.*}.
 * </p>
 * <p>
 * Configuration is driven by application properties:
 * <pre>
 * spring.mail.host=smtp.example.com
 * spring.mail.port=587
 * spring.mail.username=user@example.com
 * spring.mail.password=secret
 * spring.mail.properties.mail.smtp.auth=true
 * </pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see MailProperties
 * @see JavaMailSenderImpl
 * @see EmailConfigJavaMailSender
 * @see ConditionalOnProperty
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@EnableConfigurationProperties(MailProperties.class)
public class MailSenderConfiguration {

    /**
     * Creates the primary JavaMailSender bean for sending emails via SMTP.
     * <p>
     * This method constructs an {@link EmailConfigJavaMailSender} instance, which extends
     * {@link JavaMailSenderImpl} with enhanced configuration support. It maps Spring Boot's
     * {@link MailProperties} (including host, port, username, password, protocol, and default encoding)
     * to the JavaMailSender implementation. Additional SMTP properties from {@code spring.mail.properties.*}
     * are converted from a map to {@link Properties} format via the {@link #asProperties(Map)} helper
     * and applied to the mail sender.
     * </p>
     * <p>
     * The returned bean serves as the primary mail sender used across the application for email operations,
     * including notifications, password resets, and administrative communications.
     * </p>
     *
     * @param properties the Spring Boot mail configuration properties bound from application configuration
     * @return configured {@link JavaMailSenderImpl} instance wrapped as {@link EmailConfigJavaMailSender}
     *         ready for email transmission
     * @see MailProperties
     * @see EmailConfigJavaMailSender
     * @see JavaMailSenderImpl
     */
    @Bean
    JavaMailSenderImpl mailSender(MailProperties properties) {
        EmailConfigJavaMailSender sender = new EmailConfigJavaMailSender();
        applyProperties(properties, sender);
        return sender;
    }

    /**
     * Applies Spring Boot mail properties to the JavaMailSender implementation.
     * <p>
     * This helper method transfers configuration from {@link MailProperties} to the
     * {@link JavaMailSenderImpl} instance. It configures the SMTP host (required), port
     * (optional, defaults to protocol standard), username and password for authentication,
     * mail protocol (typically "smtp" or "smtps"), and default character encoding for email content.
     * </p>
     * <p>
     * Additional JavaMail-specific properties from {@code spring.mail.properties.*} (such as
     * {@code mail.smtp.auth}, {@code mail.smtp.starttls.enable}, {@code mail.smtp.timeout})
     * are converted to {@link Properties} format via {@link #asProperties(Map)} and applied
     * to enable advanced SMTP features like authentication, TLS, and connection timeouts.
     * </p>
     *
     * @param properties the mail configuration properties from Spring Boot auto-configuration
     * @param sender the JavaMailSender instance to configure with SMTP settings
     */
    private void applyProperties(MailProperties properties, JavaMailSenderImpl sender) {
        sender.setHost(properties.getHost());
        if (properties.getPort() != null) {
            sender.setPort(properties.getPort());
        }
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setProtocol(properties.getProtocol());
        if (properties.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(properties.getDefaultEncoding().name());
        }
        if (!properties.getProperties().isEmpty()) {
            sender.setJavaMailProperties(asProperties(properties.getProperties()));
        }
    }

    /**
     * Converts Spring Boot mail properties map to java.util.Properties format required by JavaMail API.
     * <p>
     * This utility method transforms the {@code spring.mail.properties.*} configuration map
     * (with String keys and String values) into a {@link Properties} object compatible with
     * the JavaMail API's {@link JavaMailSenderImpl#setJavaMailProperties(Properties)} method.
     * The conversion iterates over map entries and transfers them via {@link Properties#putAll(Map)}.
     * </p>
     * <p>
     * Example properties transferred include:
     * <ul>
     *   <li>{@code mail.smtp.auth} - Enable SMTP authentication</li>
     *   <li>{@code mail.smtp.starttls.enable} - Enable TLS encryption</li>
     *   <li>{@code mail.smtp.timeout} - Connection timeout in milliseconds</li>
     *   <li>{@code mail.smtp.connectiontimeout} - Initial connection timeout</li>
     * </ul>
     * </p>
     *
     * @param source the map of mail properties from Spring Boot configuration (typically from
     *               {@code spring.mail.properties.*} keys)
     * @return a {@link Properties} object containing all entries from the source map, ready for
     *         JavaMail API consumption
     * @see Properties
     * @see JavaMailSenderImpl#setJavaMailProperties(Properties)
     */
    private Properties asProperties(Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }

}
