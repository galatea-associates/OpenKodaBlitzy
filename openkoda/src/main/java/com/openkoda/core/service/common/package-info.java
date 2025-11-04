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

/**
 * Provides common utility services for cross-cutting transaction management concerns.
 * This package contains foundation services used throughout the OpenKoda application.
 * These services are not specific to any business domain but provide shared infrastructure.
 * <p>
 * The primary component in this package is the transaction execution service, which offers
 * programmatic transaction control when declarative Spring {@code @Transactional} annotations
 * are insufficient. This enables fine-grained transaction demarcation for complex workflows
 * that require dynamic transaction boundaries or multiple transaction scopes within a single
 * operation.

 * <p>
 * Key features include:
 * <ul>
 *   <li>Programmatic transaction execution with custom propagation and isolation levels</li>
 *   <li>Integration with Spring's {@code PlatformTransactionManager} infrastructure</li>
 *   <li>Support for nested transactions and savepoint management</li>
 *   <li>Exception handling and rollback control for transactional operations</li>
 * </ul>

 * <p>
 * Use cases for services in this package include:
 * <ul>
 *   <li>Dynamic entity operations requiring transaction control during runtime class generation</li>
 *   <li>Batch processing operations with custom transaction boundaries</li>
 *   <li>Integration scenarios where external system calls must be isolated from local transactions</li>
 *   <li>Flow pipeline execution requiring explicit transaction management</li>
 * </ul>

 *
 * @see com.openkoda.core.service.common.TransactionalExecutorImpl
 * @since 1.7.1
 * @author OpenKoda Team
 */
package com.openkoda.core.service.common;