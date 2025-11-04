package com.openkoda.core.service.email;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.openkoda.model.EmailConfig;
import com.openkoda.repository.EmailConfigRepository;

import jakarta.inject.Inject;
import jakarta.mail.internet.MimeMessage;

/**
 * Spring {@code @Service} wrapper around {@link JavaMailSenderImpl} that overrides getters and {@link #getJavaMailProperties()} 
 * to prefer persisted {@link EmailConfig} entity values from database at send time.
 * <p>
 * This implementation merges DB-stored SMTP configuration (host, credentials, port, protocol) with application.properties 
 * defaults using {@link StringUtils#defaultIfBlank(CharSequence, CharSequence)} fallback pattern. When an {@link EmailConfig} 
 * entity is present in the database, its values take precedence over configured property defaults. If database values are absent 
 * or blank, the implementation falls back to parent {@link JavaMailSenderImpl} configuration from {@code spring.mail.*} properties.

 * <p>
 * <b>Per-Organization Email Configuration:</b> This class supports per-organization email configuration via {@link EmailConfig} 
 * entities stored in the database, enabling different SMTP settings for multi-tenant deployments. The {@link #doSend(MimeMessage[], Object[])} 
 * override loads the first available {@link EmailConfig} from the repository before sending, making those settings available to 
 * subsequent getter invocations.

 * <p>
 * <b>Concurrency Warning:</b> The transient {@link #emailConfig} field is populated in {@link #doSend(MimeMessage[], Object[])} 
 * and is NOT thread-safe. Concurrent mail sends on the same bean instance can experience race conditions where one thread's 
 * {@link EmailConfig} may be observed by another thread's getter invocations. For production use with concurrent sends, consider 
 * using a {@link ThreadLocal} or prototype-scoped bean.

 * <p>
 * <b>Known Issues:</b>
 * <ul>
 * <li>Line 72: {@code getJavaMailProperties()} incorrectly sets {@code spring.mail.smtp.ssl.enable} property using 
 * {@code emailConfig.getSmtpAuth().toString()} instead of {@code emailConfig.getSsl().toString()}</li>
 * <li>Line 76: Property key has typo {@code "spring.mail.properties.mail.smtp.starttls.enabl"} (missing final 'e' in 'enable')</li>
 * </ul>

 * 
 * @author mboronski
 * @since 1.7.1
 * @see EmailConfig
 * @see EmailConfigRepository
 * @see JavaMailSenderImpl
 * @see JavaMailSender
 */
@Service
public class EmailConfigJavaMailSender extends JavaMailSenderImpl implements JavaMailSender {

    /**
     * Repository for fetching database-stored {@link EmailConfig} entities containing per-organization SMTP configuration.
     * Injected via Jakarta {@code @Inject} annotation.
     */
    @Inject private EmailConfigRepository emailConfigRepository;
    
    /**
     * Transient field populated in {@link #doSend(MimeMessage[], Object[])} with the first {@link EmailConfig} entity from database.
     * <p>
     * <b>Thread-Safety:</b> This field is NOT thread-safe. Concurrent invocations of {@code doSend()} can cause race conditions 
     * where getters observe configuration from concurrent send operations. Value is set per-send and persists until next send.

     */
    private EmailConfig emailConfig;
    
    /**
     * Returns SMTP mail server host, preferring database-stored value from {@link EmailConfig} if present.
     * <p>
     * Fallback behavior: Returns {@link EmailConfig#getHost()} if {@link #emailConfig} is non-null and host value is non-blank, 
     * otherwise returns {@link JavaMailSenderImpl#getHost()} from {@code spring.mail.host} property.

     * 
     * @return SMTP host from database {@link EmailConfig} or application properties fallback
     * @see EmailConfig#getHost()
     * @see JavaMailSenderImpl#getHost()
     */
    @Override
    public String getHost() {
        return StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getHost() : null, super.getHost());
    }
    
    /**
     * Returns SMTP authentication password, preferring database-stored value from {@link EmailConfig} if present.
     * <p>
     * Fallback behavior: Returns {@link EmailConfig#getPassword()} if {@link #emailConfig} is non-null and password is non-blank, 
     * otherwise returns {@link JavaMailSenderImpl#getPassword()} from {@code spring.mail.password} property.

     * 
     * @return SMTP password from database {@link EmailConfig} or application properties fallback
     * @see EmailConfig#getPassword()
     * @see JavaMailSenderImpl#getPassword()
     */
    @Override
    public String getPassword() {
        return StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getPassword() : null, super.getPassword());
    }
    
    /**
     * Returns SMTP mail server port, preferring database-stored value from {@link EmailConfig} if present.
     * <p>
     * Fallback behavior: Returns {@link EmailConfig#getPort()} if {@link #emailConfig} is non-null and port is non-null, 
     * otherwise returns {@link JavaMailSenderImpl#getPort()} from {@code spring.mail.port} property (typically 25, 465, or 587).

     * 
     * @return SMTP port number from database {@link EmailConfig} or application properties fallback
     * @see EmailConfig#getPort()
     * @see JavaMailSenderImpl#getPort()
     */
    @Override
    public int getPort() {
        if(emailConfig != null && emailConfig.getPort() != null) {
            return emailConfig.getPort();
        }
        
        return super.getPort();
    }
    
    /**
     * Returns mail transport protocol, mapping {@link EmailConfig#getSsl()} boolean to protocol string or using explicit protocol value.
     * <p>
     * Protocol resolution logic:
     * <ol>
     * <li>If {@link #emailConfig} is non-null and {@link EmailConfig#getSsl()} is non-null: returns {@code "smtps"} if SSL is enabled, 
     * otherwise {@code "smtp"}</li>
     * <li>Else if {@link EmailConfig#getProtocol()} is non-blank: returns that protocol string</li>
     * <li>Else returns {@link JavaMailSenderImpl#getProtocol()} from {@code spring.mail.protocol} property</li>
     * </ol>

     * 
     * @return mail protocol string ({@code "smtp"}, {@code "smtps"}, etc.) from database {@link EmailConfig} or application properties fallback
     * @see EmailConfig#getSsl()
     * @see EmailConfig#getProtocol()
     * @see JavaMailSenderImpl#getProtocol()
     */
    @Override
    public String getProtocol() {
        if(emailConfig != null && emailConfig.getSsl() != null) {
            return Boolean.TRUE.equals(emailConfig.getSsl()) ? "smtps" : "smtp";
        }
        
        return StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getProtocol() : null, super.getProtocol());
    }
    
    /**
     * Returns SMTP authentication username, preferring database-stored value from {@link EmailConfig} if present.
     * <p>
     * Fallback behavior: Returns {@link EmailConfig#getUsername()} if {@link #emailConfig} is non-null and username is non-blank, 
     * otherwise returns {@link JavaMailSenderImpl#getUsername()} from {@code spring.mail.username} property.

     * 
     * @return SMTP username from database {@link EmailConfig} or application properties fallback
     * @see EmailConfig#getUsername()
     * @see JavaMailSenderImpl#getUsername()
     */
    @Override
    public String getUsername() {
        return StringUtils.defaultIfBlank(emailConfig != null ? emailConfig.getUsername() : null, super.getUsername());
    }
    
    /**
     * Returns JavaMail {@link Properties} with database-stored SMTP property overrides merged into parent properties.
     * <p>
     * This override copies {@link JavaMailSenderImpl#getJavaMailProperties()} from application configuration, then conditionally 
     * sets SMTP property keys from {@link EmailConfig} if {@link #emailConfig} is non-null:
     * <ul>
     * <li>{@code spring.mail.properties.mail.smtp.auth} - set from {@link EmailConfig#getSmtpAuth()} if non-null</li>
     * <li>{@code spring.mail.smtp.ssl.enable} - intended to be set from {@link EmailConfig#getSsl()} if non-null</li>
     * <li>{@code spring.mail.properties.mail.smtp.starttls.enabl} - set from {@link EmailConfig#getStarttls()} if non-null</li>
     * </ul>

     * <p>
     * <b>Known Bugs:</b>
     * <ul>
     * <li><b>SSL Property Bug (Line 72):</b> The {@code spring.mail.smtp.ssl.enable} property is incorrectly set using 
     * {@code emailConfig.getSmtpAuth().toString()} instead of {@code emailConfig.getSsl().toString()}. This causes the SSL 
     * enable property to receive the SMTP auth boolean value, potentially breaking SSL connections.</li>
     * <li><b>STARTTLS Property Key Typo (Line 76):</b> Property key {@code "spring.mail.properties.mail.smtp.starttls.enabl"} 
     * is missing the final 'e' character. The correct key should be {@code "spring.mail.properties.mail.smtp.starttls.enable"}. 
     * This typo prevents the STARTTLS setting from being recognized by JavaMail.</li>
     * </ul>

     * 
     * @return merged {@link Properties} with parent configuration plus database-stored SMTP overrides (subject to bugs noted above)
     * @see EmailConfig#getSmtpAuth()
     * @see EmailConfig#getSsl()
     * @see EmailConfig#getStarttls()
     * @see JavaMailSenderImpl#getJavaMailProperties()
     */
    @Override
    public Properties getJavaMailProperties() {
        Properties mailProps = new Properties(super.getJavaMailProperties());
        if(emailConfig != null) {
            if(emailConfig.getSmtpAuth() != null) {
                mailProps.setProperty("spring.mail.properties.mail.smtp.auth", emailConfig.getSmtpAuth().toString());
            }
            
            if(emailConfig.getSsl() != null) {
                mailProps.setProperty("spring.mail.smtp.ssl.enable", emailConfig.getSmtpAuth().toString());
            }
            
            if(emailConfig.getStarttls() != null) {
                mailProps.setProperty("spring.mail.properties.mail.smtp.starttls.enabl", emailConfig.getStarttls().toString());
            }
        }

        return mailProps;
    }
    
    /**
     * Loads first {@link EmailConfig} entity from database into transient {@link #emailConfig} field, then delegates to parent 
     * {@link JavaMailSenderImpl#doSend(MimeMessage[], Object[])} for actual message transmission.
     * <p>
     * This override populates {@link #emailConfig} before sending, enabling subsequent getter method invocations 
     * (from parent send logic) to resolve database-stored SMTP configuration via {@link #getHost()}, {@link #getPort()}, 
     * {@link #getUsername()}, {@link #getPassword()}, {@link #getProtocol()}, and {@link #getJavaMailProperties()}.

     * <p>
     * <b>Configuration Resolution:</b> Retrieves the first {@link EmailConfig} from {@link EmailConfigRepository#findAll()} 
     * (or {@code null} if repository is empty). For multi-tenant scenarios with multiple {@link EmailConfig} entities, 
     * this implementation always uses the first entity returned by the repository query.

     * <p>
     * <b>Concurrency Warning:</b> The {@link #emailConfig} field assignment is NOT thread-safe. If multiple threads invoke 
     * this method concurrently on the same bean instance, race conditions can occur where one thread's configuration overwrites 
     * another's, causing emails to be sent with incorrect SMTP settings. Consider using prototype scope or {@link ThreadLocal} 
     * for concurrent email sending scenarios.

     * 
     * @param mimeMessages array of {@link MimeMessage} instances to send
     * @param originalMessages array of original message objects (for error reporting)
     * @throws MailException if message sending fails (propagated from parent {@link JavaMailSenderImpl#doSend(MimeMessage[], Object[])})
     * @see EmailConfigRepository#findAll()
     * @see JavaMailSenderImpl#doSend(MimeMessage[], Object[])
     */
    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
        emailConfig = emailConfigRepository.findAll().stream().findFirst().orElse(null);
        super.doSend(mimeMessages, originalMessages);
    }
}