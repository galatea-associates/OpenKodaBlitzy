/*
MIT License

Copyright (c) 2014-2022, Codedose CDX Sp. z o.o. Sp. K. <stratoflow.com>

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

package com.openkoda.core.flow;

import java.util.function.Supplier;

/**
 * Transactional boundary interface for wrapping Flow pipeline execution in Spring transactional context.
 * <p>
 * Provides transaction demarcation for Flow steps, ensuring atomicity of operations and automatic
 * rollback on exceptions. Implementations delegate to Spring's transaction infrastructure to
 * provide consistent transactional behavior across Flow pipeline executions.
 * <p>
 * This interface serves as an abstraction over Spring's transaction management, allowing Flow
 * pipelines to execute within managed transactions without direct coupling to Spring's
 * transaction APIs. The interface uses raw types for flexibility with Flow's internal generic handling.
 * <p>
 * Example usage within Flow pipeline:
 * <pre>{@code
 * TransactionalExecutor executor = ...;
 * Object result = executor.executeInTransaction(() -> performDatabaseOperations());
 * }</pre>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see Flow
 * @see org.springframework.transaction.annotation.Transactional
 */
public interface TransactionalExecutor {
    
    /**
     * Executes the supplied function within a transactional context.
     * <p>
     * Wraps the supplier execution in a Spring-managed transaction, providing automatic commit
     * on successful completion and rollback on any exception. Transaction semantics such as
     * propagation, isolation level, and timeout are determined by the concrete implementation.
     * 
     * <p>
     * Implementations typically delegate to Spring's TransactionTemplate or use @Transactional
     * proxy mechanisms to provide transaction boundaries. The raw Supplier type allows flexibility
     * in handling Flow's internal generic types without type parameter constraints.
     * 
     *
     * @param f the supplier function to execute within transaction boundaries, must not be null
     * @return the result returned by the supplier function, may be null depending on supplier implementation
     * @throws RuntimeException if transaction fails or supplier throws an exception, triggering automatic rollback
     */
    Object executeInTransaction(Supplier f);
}
