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

package com.openkoda;

import com.openkoda.core.customisation.BasicCustomisationService;
import com.openkoda.core.helper.SpringProfilesHelper;
import com.openkoda.model.component.Form;
import com.openkoda.repository.FormRepository;
import com.openkoda.service.dynamicentity.DynamicEntityDescriptor;
import com.openkoda.service.dynamicentity.DynamicEntityDescriptorFactory;
import com.openkoda.service.dynamicentity.DynamicEntityRegistrationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.openkoda.service.dynamicentity.DynamicEntityRegistrationService.PACKAGE;
import static com.openkoda.service.dynamicentity.DynamicEntityRegistrationService.buildAndLoadDynamicClasses;

/**
 * Primary Spring Boot application entry point for OpenKoda platform that manages application lifecycle,
 * dynamic entity registration, and JPA configuration.
 * <p>
 * This class serves as the main entry point for the OpenKoda application, configured with
 * {@code @SpringBootApplication} for component scanning in the {@code com.openkoda} package.
 * It enables several key Spring features including caching ({@code @EnableCaching}),
 * transaction management ({@code @EnableTransactionManagement}), and JPA repositories
 * ({@code @EnableJpaRepositories}) with support for both standard and dynamically generated
 * entities in the {@code com.openkoda.dynamicentity.generated} package.

 * <p>
 * The class maintains a static {@link ConfigurableApplicationContext} reference for lifecycle
 * control, allowing programmatic restart and shutdown operations. Dynamic entity support is
 * provided through integration with Byte Buddy runtime class generation, where forms defined
 * in the database are compiled into JPA entities and registered with the persistence unit.

 * <p>
 * Key lifecycle methods include:
 * <ul>
 *   <li>{@link #startApp(Class, String[])} - Initializes Spring context with safety checks</li>
 *   <li>{@link #restart(boolean)} - Restarts application with optional dynamic entity reloading</li>
 *   <li>{@link #shutdown()} - Terminates the application process</li>
 * </ul>

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see BasicCustomisationService
 * @see DynamicEntityRegistrationService
 * @see FormRepository
 */
@SpringBootApplication(
        scanBasePackages = "com.openkoda", exclude = { SecurityAutoConfiguration.class })
@EnableCaching//(mode = AdviceMode.ASPECTJ)
@RestController
@Configuration
@EnableJpaRepositories(basePackages = {"com.openkoda","com.openkoda.dynamicentity.generated"})
@EnableTransactionManagement
public class App extends SpringBootServletInitializer {
    
    /**
     * Static Spring application context reference for lifecycle management and bean retrieval.
     * <p>
     * Used by {@link #restart(boolean)} and {@link #shutdown()} methods to control application
     * lifecycle. The context is set during application startup in {@link #startApp(Class, String[], boolean)}
     * and updated during restart operations.

     */
    protected static ConfigurableApplicationContext context;
    
    /**
     * Main application class reference for context restart operations.
     * <p>
     * Preserved across restarts to maintain application class identity. Set in
     * {@link #startApp(Class, String[], boolean)} and used by {@link #restart(boolean)}
     * to relaunch the Spring context with the same application class.

     */
    private static Class mainClass;

    /**
     * Application entry point that delegates to {@link #startApp(Class, String[])}.
     * <p>
     * This is the standard JVM entry point for launching the OpenKoda application.
     * It immediately delegates to the {@code startApp} method with {@link App} class
     * as the application class parameter.

     *
     * @param args command-line arguments passed to the application. Supports {@code --force}
     *             flag for bypassing initialization safety prompts when running in
     *             destructive initialization profiles
     * @see #startApp(Class, String[])
     */
    public static void main(String[] args) {
        startApp(App.class, args);
    }

    /**
     * Initializes and starts the Spring Boot application context with safety checks.
     * <p>
     * This method delegates to {@link #startApp(Class, String[], boolean)} with {@code forced}
     * parameter set to {@code false}, ensuring that initialization safety checks are performed
     * when running in destructive initialization profiles.

     * <p>
     * The startup process includes:
     * <ul>
     *   <li>Setting JAXBContextFactory system property for XML binding compatibility</li>
     *   <li>Running the Spring application context</li>
     *   <li>Initializing {@link BasicCustomisationService} for custom module registration</li>
     *   <li>Performing safety checks for initialization profiles (unless bypassed)</li>
     * </ul>

     *
     * @param appClass the Spring Boot application class to run, typically {@link App} class
     *                 or a subclass for customized configurations
     * @param args command-line arguments including optional {@code --force} flag for
     *             bypassing initialization safety prompts
     * @see #startApp(Class, String[], boolean)
     * @see #initializationSafetyCheck(boolean)
     */
    protected static void startApp(Class appClass, String[] args) {
        startApp(appClass, args, false);
    }
    
    /**
     * Starts application with optional forced initialization bypass.
     * <p>
     * This is the core startup method that initializes the Spring Boot application context.
     * When {@code forced} is {@code false}, it performs initialization safety checks to prevent
     * accidental data loss in initialization profiles. The {@code forced} parameter is typically
     * set to {@code true} when called from {@link OpenkodaApp} after safety checks have already
     * been performed by {@link JDBCApp}.

     * <p>
     * Startup sequence:
     * <ol>
     *   <li>Stores the main application class for restart operations</li>
     *   <li>Checks for {@code --force} flag in arguments (if not forced)</li>
     *   <li>Invokes {@link #initializationSafetyCheck(boolean)} for destructive profile confirmation</li>
     *   <li>Sets {@code jakarta.xml.bind.JAXBContextFactory} system property for JAXB compatibility</li>
     *   <li>Runs Spring application context via {@link SpringApplication#run(Class, String[])}</li>
     *   <li>Retrieves {@link BasicCustomisationService} bean for custom module initialization</li>
     * </ol>

     *
     * @param appClass the Spring Boot application class to run, typically {@link App} class
     *                 or a subclass for customized application configurations
     * @param args command-line arguments passed to Spring Boot, may include {@code --force}
     *             flag to bypass interactive confirmation prompts
     * @param forced if {@code true}, skips user confirmation for initialization profiles.
     *               Used when safety checks have been performed by calling code
     * @see #initializationSafetyCheck(boolean)
     * @see BasicCustomisationService
     */
    protected static void startApp(Class appClass, String[] args, boolean forced) {
        mainClass = appClass;
        if(!forced) {
            boolean isForce = args != null && Arrays.stream(args).anyMatch(a -> "--force".equals(a));
            initializationSafetyCheck(isForce);
        }
        
        System.setProperty("jakarta.xml.bind.JAXBContextFactory", "com.sun.xml.bind.v2.ContextFactory");
        context = SpringApplication.run(appClass, args);
        BasicCustomisationService customisationService = context.getBean(BasicCustomisationService.class);
    }

    /**
     * Prompts user confirmation when running in initialization profile to prevent accidental data loss.
     * <p>
     * This method checks if the application is starting with an initialization profile
     * (such as {@code drop_and_init_database}) using {@link SpringProfilesHelper#isInitializationProfile()}.
     * Initialization profiles perform destructive operations including dropping all database tables
     * and recreating schema from scratch.

     * <p>
     * When an initialization profile is detected:
     * <ul>
     *   <li>Displays a warning message with skull emoji indicating data loss risk</li>
     *   <li>Prompts user to type 'y' to continue or any other key to abort</li>
     *   <li>If {@code isforce} is {@code true}, bypasses interactive prompt and assumes confirmation</li>
     *   <li>Exits application with status 0 if user declines to continue</li>
     * </ul>

     * <p>
     * This safety mechanism prevents accidental execution of destructive initialization profiles
     * in production environments or during development when data preservation is important.

     *
     * @param isforce if {@code true}, bypasses interactive confirmation prompt and assumes user consent.
     *                Typically set to {@code true} when {@code --force} flag is present in command-line arguments
     * @see SpringProfilesHelper#isInitializationProfile()
     */
    protected static void initializationSafetyCheck(boolean isforce) {
        try {
            if (SpringProfilesHelper.isInitializationProfile()) {
                System.out.println("*********************************************************************");
                System.out.println(" Application starts in initialization mode.");
                System.out.println(" " + Character.toString(0x1F480) + " This will irreversibly delete all data in the database.");
                System.out.println(" " + Character.toString(0x1F480) + " Continue? [y to continue]");
                System.out.println("*********************************************************************");
                if(isforce) {
                    System.out.println(" Force mode, assuming yes");
                    return;
                }
                int c = System.in.read();
                if (c != 'y') {
                    System.out.println(" Breaking the initialization");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            System.exit(1);
        }
    }


    /**
     * Terminates the application process with clean shutdown.
     * <p>
     * This method performs an immediate application termination by calling {@link System#exit(int)}
     * with status code 0, indicating successful termination. The JVM will exit and all non-daemon
     * threads will be stopped.

     * <p>
     * This method does not attempt to close the Spring application context gracefully.
     * For graceful shutdown with proper context cleanup, consider using Spring Boot's
     * actuator shutdown endpoint or {@link ConfigurableApplicationContext#close()}.

     */
    public static void shutdown() {
        System.exit(0);
    }
    
    /**
     * Restarts application context without reloading dynamic entity forms.
     * <p>
     * This convenience method delegates to {@link #restart(boolean)} with {@code reloadAllForms}
     * parameter set to {@code false}. The Spring application context is closed and restarted,
     * but dynamic entity definitions are not regenerated from the database.

     * <p>
     * Use this method when configuration changes require context restart but dynamic entity
     * schemas have not changed. For full restart including dynamic entity regeneration,
     * use {@link #restart(boolean)} with {@code true} parameter.

     *
     * @see #restart(boolean)
     */
    public static void restart() {
        restart(false);
    }
    
    /**
     * Restarts Spring application context with optional dynamic entity form reloading.
     * <p>
     * This method performs a full application restart in a separate non-daemon thread to survive
     * the context shutdown process. The restart sequence includes closing the current Spring context,
     * optionally regenerating dynamic entity classes from database form definitions, and launching
     * a new Spring application context with the original command-line arguments.

     * <p>
     * Restart sequence when {@code reloadAllForms} is {@code true}:
     * <ol>
     *   <li>Retrieves all {@link Form} entities from {@link FormRepository}</li>
     *   <li>Closes the current Spring application context</li>
     *   <li>Generates {@link DynamicEntityDescriptor} instances via {@link DynamicEntityRegistrationService}</li>
     *   <li>Compiles and loads dynamic entity classes using Byte Buddy via {@link DynamicEntityRegistrationService#buildAndLoadDynamicClasses(ClassLoader)}</li>
     *   <li>Launches new Spring context with {@link SpringApplicationBuilder} using stored {@code mainClass} and original arguments</li>
     * </ol>

     * <p>
     * Restart sequence when {@code reloadAllForms} is {@code false}:
     * <ol>
     *   <li>Closes the current Spring application context</li>
     *   <li>Recompiles existing dynamic entity classes (no database query)</li>
     *   <li>Launches new Spring context with original configuration</li>
     * </ol>

     * <p>
     * The restart executes in a non-daemon thread ({@code setDaemon(false)}) to ensure the thread
     * survives the context shutdown operation. This is critical because daemon threads are terminated
     * when the JVM detects no remaining non-daemon threads.

     *
     * @param reloadAllForms if {@code true}, queries {@link FormRepository} to retrieve all form
     *                       entities, generates {@link DynamicEntityDescriptor} instances via Byte Buddy,
     *                       recompiles and loads dynamic entity classes before context restart.
     *                       If {@code false}, reuses existing dynamic entity descriptors
     * @see FormRepository
     * @see com.openkoda.service.dynamicentity.DynamicEntityRegistrationService#generateDynamicEntityDescriptors(List, Long)
     * @see DynamicEntityRegistrationService#buildAndLoadDynamicClasses(ClassLoader)
     */
    public static void restart(boolean reloadAllForms) {

        System.out.println("*********************************************************************");
        System.out.println(" Application restart...");
        System.out.println("*********************************************************************");

        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            //review
            List<Form> forms = null;
            if (reloadAllForms) {
                FormRepository fr = context.getBean(FormRepository.class);
                forms = fr.findAll();
            }
            context.close();
            if (reloadAllForms) {
                DynamicEntityRegistrationService.generateDynamicEntityDescriptors(forms, System.currentTimeMillis());
            }

            try {
                buildAndLoadDynamicClasses(App.class.getClassLoader());
            } catch (IOException|URISyntaxException e) {
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }

            context = new SpringApplicationBuilder(mainClass).run(args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Creates PersistenceUnitPostProcessor that registers dynamically generated entity classes with JPA.
     * <p>
     * This method produces a {@link PersistenceUnitPostProcessor} that iterates through all
     * {@link DynamicEntityDescriptor} instances maintained by {@link DynamicEntityDescriptorFactory}
     * and adds their fully-qualified class names to the JPA persistence unit. This enables Hibernate
     * to discover and manage runtime-generated entity classes that are compiled via Byte Buddy and
     * loaded into the {@code com.openkoda.dynamicentity.generated} package.

     * <p>
     * Without this post-processor, JPA would only discover entities via classpath scanning and
     * annotation processing at compile time. Dynamic entities generated at runtime would not be
     * recognized by the {@link jakarta.persistence.EntityManager}.

     * <p>
     * The post-processor is invoked by {@link #entityManagerFactoryBuilder()} and integrated
     * into the {@link EntityManagerFactoryBuilder} configuration. Each dynamic entity class name
     * is constructed by concatenating {@link DynamicEntityRegistrationService#PACKAGE} constant
     * with the suffixed entity class name from the descriptor.

     *
     * @return a {@link PersistenceUnitPostProcessor} that adds managed class names from
     *         {@link DynamicEntityDescriptorFactory} to enable JPA discovery of runtime-generated entities
     * @see DynamicEntityDescriptorFactory#instances()
     * @see DynamicEntityDescriptor#getSuffixedEntityClassName()
     * @see #entityManagerFactoryBuilder()
     */
    public PersistenceUnitPostProcessor persistenceUnitPostProcessor() {
        return pui -> {
            for (DynamicEntityDescriptor a : DynamicEntityDescriptorFactory.instances()) {
                pui.addManagedClassName(PACKAGE + a.getSuffixedEntityClassName());
            }
        };
    }

    /**
     * Configures EntityManagerFactoryBuilder bean with HibernateJpaVendorAdapter and dynamic entity support.
     * <p>
     * This Spring bean method creates and configures an {@link EntityManagerFactoryBuilder} that supports
     * both standard JPA entities and dynamically generated entities. The builder uses Hibernate as the
     * JPA vendor adapter and integrates the {@link #persistenceUnitPostProcessor()} to register runtime-generated
     * entity classes with the persistence unit.

     * <p>
     * Configuration details:
     * <ul>
     *   <li>JPA Vendor: {@link HibernateJpaVendorAdapter} for Hibernate ORM integration</li>
     *   <li>JPA Properties: Empty {@link HashMap} (properties configured via application.properties)</li>
     *   <li>Bootstrap Executor: {@code null} (synchronous entity manager factory creation)</li>
     *   <li>Persistence Unit Post-Processors: Includes {@link #persistenceUnitPostProcessor()} for dynamic entity registration</li>
     * </ul>

     * <p>
     * The bean is named {@code "entityBuilder"} and is used throughout the application for creating
     * entity manager factories that recognize both compile-time and runtime-generated JPA entities.
     * This is essential for the dynamic entity generation feature where form definitions are compiled
     * into JPA entities via Byte Buddy.

     *
     * @return configured {@link EntityManagerFactoryBuilder} with persistence unit post-processor
     *         that enables JPA discovery of runtime-generated dynamic entities
     * @see #persistenceUnitPostProcessor()
     * @see HibernateJpaVendorAdapter
     * @see DynamicEntityRegistrationService
     */
    @Bean("entityBuilder")
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        EntityManagerFactoryBuilder a = new EntityManagerFactoryBuilder(hibernateJpaVendorAdapter, new HashMap<>(), null);
        a.setPersistenceUnitPostProcessors(persistenceUnitPostProcessor());
        return a;
    }

}
