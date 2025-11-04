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

/**
 * Marker interface for PageAttr values that require post-execution processing after Flow pipeline completion.
 * <p>
 * This interface enables automatic finalization or cleanup operations for values stored in the Flow pipeline's
 * PageModelMap. When a Flow pipeline completes successfully, the Flow executor automatically invokes the
 * {@link #process()} method on all PageAttr values that implement this interface.
 * <p>
 * Processing occurs only after successful execution of all pipeline steps. If an exception occurs during
 * Flow execution, post-execution processing is skipped to prevent inconsistent state.
 * <p>
 * Example usage:
 * <pre>
 * public class CleanupResource implements PostExecuteProcessablePageAttr {
 *     public void process() { // finalization logic }
 * }
 * </pre>
 *
 * @see Flow
 * @see PageAttr
 * @since 1.7.1
 * @author OpenKoda Team
 */
public interface PostExecuteProcessablePageAttr {
    
    /**
     * Processes this PageAttr value after successful Flow pipeline execution.
     * <p>
     * This method is invoked automatically by {@code Flow.execute()} after all pipeline steps complete
     * successfully. Implementations should perform cleanup, finalization, or other post-processing tasks
     * required by the PageAttr value.
     * 
     * <p>
     * This method is called only on successful execution. If the Flow pipeline throws an exception during
     * execution, this method is not invoked.
     * 
     */
    void process();
}
