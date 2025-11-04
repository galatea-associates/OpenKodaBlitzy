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

package com.openkoda.service.export.dto;

/**
 * DTO for scheduled job definitions with cron expression and master-node execution flag.
 * <p>
 * This is a mutable JavaBean POJO for YAML/JSON serialization. It extends ComponentDto for module 
 * and organization scope, and maps the Scheduler domain entity for scheduled job export/import. 
 * The onMasterOnly flag controls cluster master-node-only execution for distributed deployments. 
 * This DTO is used by job scheduler configuration pipelines and is not thread-safe.
 * 
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see ComponentDto for inherited module and organizationId fields
 * @see com.openkoda.model.component.Scheduler
 */
public class SchedulerConversionDto extends ComponentDto {

    /**
     * Cron expression defining the job execution schedule (e.g., '0 0 * * * ?' for daily). Nullable.
     */
    private String cronExpression;
    
    /**
     * Event data payload or parameters passed to the scheduled job on execution. Nullable.
     */
    private String eventData;
    
    /**
     * Flag indicating whether this job should execute only on the cluster master node in distributed deployments. Defaults to false.
     */
    private boolean onMasterOnly;
    
    /**
     * Gets the cron expression defining the job execution schedule.
     *
     * @return the cron expression or null if not set
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression defining the job execution schedule.
     *
     * @param cronExpression the cron expression to set, may be null
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Gets the event data payload or parameters passed to the scheduled job on execution.
     *
     * @return the event data or null if not set
     */
    public String getEventData() {
        return eventData;
    }

    /**
     * Sets the event data payload or parameters passed to the scheduled job on execution.
     *
     * @param eventData the event data to set, may be null
     */
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    /**
     * Checks if this job executes only on the cluster master node.
     *
     * @return true if job executes only on cluster master node, false to allow execution on any node
     */
    public boolean isOnMasterOnly() {
        return onMasterOnly;
    }

    /**
     * Sets whether this job executes only on the cluster master node.
     *
     * @param onMasterOnly true to restrict execution to master node only, false to allow execution on any node
     */
    public void setOnMasterOnly(boolean onMasterOnly) {
        this.onMasterOnly = onMasterOnly;
    }
}
