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

package com.openkoda.model.component.event;

/**
 * Lightweight POJO representing a consumer method signature for event listener registration.
 * <p>
 * Parses and serializes consumer descriptors for event listener registration, mapping between
 * {@link com.openkoda.form.EventListenerForm} and {@link EventListenerEntry}. A consumer descriptor
 * consists of a fully qualified class name, method name, parameter type, and the number of static
 * parameters (0-4) provided to the consumer method.
 * 
 * <p>
 * The consumer string format is comma-separated: {@code className,methodName,parameterType,numberOfStaticParams}.
 * Parsing is performed via {@link String#split(String)} without trimming or CSV-escaping. Invalid input
 * may result in silent field initialization failures or {@link NumberFormatException} propagation.
 * 
 * <p>
 * Thread-safety: This is a mutable POJO and is not thread-safe. External synchronization is required
 * for concurrent access.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see EventListenerEntry
 * @see com.openkoda.form.EventListenerForm
 */
public class Consumer {

    /**
     * Fully qualified class name of the consumer (event handler).
     */
    private String className;
    
    /**
     * Method name to be invoked when the event is dispatched.
     */
    private String methodName;
    
    /**
     * Fully qualified class name of the method parameter type.
     */
    private String parameterType;
    
    /**
     * Number of static parameters (0-4) provided to the consumer method.
     */
    private int numberOfStaticParams;
    
    /**
     * Consumer category used for grouping and filtering. Read-only (no setter).
     */
    private String category;

    /**
     * Parses a comma-separated consumer descriptor string and initializes the consumer fields.
     * <p>
     * The expected format is: {@code className,methodName,parameterType,numberOfStaticParams}.
     * Parsing uses {@link String#split(String)} with comma delimiter and validates exactly 4 tokens.
     * If the input has fewer or more than 4 tokens, fields remain null or default-initialized (silent failure).
     * No trimming or CSV-escaping is performed; callers must validate input.
     * 
     *
     * @param consumerString comma-separated 4-token format (className,methodName,parameterType,numberOfStaticParams)
     * @throws NumberFormatException if the numberOfStaticParams token is not a valid integer
     */
    public Consumer(String consumerString) {
        String[] consumerSplit = consumerString.split(",");
        if(consumerSplit.length == 4) {
            this.className = consumerSplit[0];
            this.methodName = consumerSplit[1];
            this.parameterType = consumerSplit[2];
            this.numberOfStaticParams= Integer.valueOf(consumerSplit[3]);
        }
    }

    /**
     * Composes a canonical comma-separated consumer string for deterministic storage and serialization.
     * <p>
     * Returns a consumer descriptor in the format: {@code className,methodName,parameterType,numberOfStaticParams}.
     * This method is used by {@link EventListenerEntry#getConsumerString()} for persistence.
     * 
     *
     * @param className fully qualified consumer class name
     * @param methodName consumer method name
     * @param parameterType fully qualified parameter class name
     * @param numberOfStaticParams number of static parameters (0-4)
     * @return comma-separated consumer descriptor string
     * @see EventListenerEntry#getConsumerString()
     */
    public static String canonicalMethodName(String className, String methodName, String parameterType, int numberOfStaticParams) {
        return className + "," + methodName + "," + parameterType + "," + numberOfStaticParams;
    }

    /**
     * Returns the fully qualified class name of the consumer (event handler).
     *
     * @return the consumer class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the fully qualified class name of the consumer (event handler).
     *
     * @param className the consumer class name to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns the method name to be invoked when the event is dispatched.
     *
     * @return the consumer method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the method name to be invoked when the event is dispatched.
     *
     * @param methodName the consumer method name to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Returns the fully qualified class name of the method parameter type.
     *
     * @return the parameter type class name
     */
    public String getParameterType() {
        return parameterType;
    }

    /**
     * Sets the fully qualified class name of the method parameter type.
     *
     * @param parameterType the parameter type class name to set
     */
    public void setParameterType(String parameterType) {
        this.parameterType = parameterType;
    }

    /**
     * Returns the number of static parameters (0-4) provided to the consumer method.
     *
     * @return the number of static parameters
     */
    public int getNumberOfStaticParams() {
        return numberOfStaticParams;
    }

    /**
     * Sets the number of static parameters (0-4) provided to the consumer method.
     *
     * @param numberOfStaticParams the number of static parameters to set
     */
    public void setNumberOfStaticParams(int numberOfStaticParams) {
        this.numberOfStaticParams = numberOfStaticParams;
    }

    /**
     * Returns the consumer category used for grouping and filtering.
     * <p>
     * This field is read-only and has no corresponding setter method.
     * 
     *
     * @return the consumer category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns a debug representation of this consumer in bracketed format.
     * <p>
     * Format: {@code Consumer[className,methodName,parameterType,numberOfStaticParams]}
     * 
     *
     * @return a string representation of this consumer
     */
    @Override
    public String toString() {
        return "Consumer[" + className + ',' + methodName + ',' + parameterType + ',' + numberOfStaticParams + ']';
    }
}
