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

package com.openkoda.core.service.common;

import com.openkoda.core.flow.TransactionalExecutor;
import com.openkoda.core.tracker.LoggingComponentWithRequestId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Spring-managed service that implements programmatic transaction management via the {@link TransactionalExecutor} interface.
 * <p>
 * This service provides methods for executing code within Spring-managed transactions with configurable transaction attributes.
 * It is used when declarative {@code @Transactional} annotation is insufficient, such as when dynamic transaction requirements,
 * conditional transaction boundaries, or programmatic control are needed. The service works with Spring's
 * {@code PlatformTransactionManager} and AOP proxying to ensure proper transaction semantics.

 * <p>
 * This implementation also extends {@link LoggingComponentWithRequestId} to provide request correlation IDs for tracing
 * and debugging across transaction boundaries. The service is a stateless singleton that is safe for concurrent use
 * across multiple threads and requests.

 * <p>
 * Example usage:
 * <pre>{@code
 * transactionalExecutor.executeInTransaction(() -> repository.save(entity));
 * }</pre>

 * <p>
 * <b>Comparison with @Transactional:</b> Use this programmatic approach when transaction boundaries cannot be determined
 * at compile time, when multiple transaction configurations are needed conditionally, or when fine-grained control over
 * transaction propagation is required. For standard use cases, prefer declarative {@code @Transactional} annotation for
 * simplicity and clarity.

 * <p>
 * <b>Important:</b> This service requires Spring context injection to ensure AOP interception. Direct instantiation or
 * self-invocation bypasses transactional semantics and will not create transaction boundaries.

 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see TransactionalExecutor
 * @see LoggingComponentWithRequestId
 * @see org.springframework.transaction.annotation.Transactional
 */

@Service
public class TransactionalExecutorImpl implements TransactionalExecutor, LoggingComponentWithRequestId {

    /**
     * Executes the supplied function within a Spring-managed transaction with rollback-on-any-exception semantics.
     * <p>
     * This method starts a transaction, logs entry with debug tag {@code [executeInTransaction]}, invokes the supplier,
     * returns the result, and commits on success or rolls back on any exception. The transaction attribute
     * {@code rollbackFor = Exception.class} ensures rollback for both checked and unchecked exceptions, which differs
     * from Spring's default behavior of rolling back only on unchecked exceptions.

     * <p>
     * The return value is of type {@code Object}; callers typically cast to the concrete type expected from the supplier.
     * This generic approach allows the method to handle any return type from the transactional operation.

     *
     * @param f the Supplier function to execute within transaction context; must not be null
     * @return the result returned by the supplied function, may be null if supplier returns null
     * @see org.springframework.transaction.annotation.Transactional
     */
    @Transactional(rollbackFor = Exception.class)
    public Object executeInTransaction(Supplier f) {
        debug("[executeInTransaction]");
        return f.get();
    }
}
