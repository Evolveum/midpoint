/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Pavol Mederly
 */
public class IterativeTaskInformation {

    public static final int LAST_FAILURES_KEPT = 30;

    /*
     * Thread safety: Just like EnvironmentalPerformanceInformation, instances of this class may be accessed from
     * more than one thread at once. Updates are invoked in the context of the thread executing the task.
     * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
     */

    protected final IterativeTaskInformationType startValue;

    protected String lastSuccessObjectName;
    protected String lastSuccessObjectDisplayName;
    protected QName lastSuccessObjectType;
    protected String lastSuccessObjectOid;
    protected Date lastSuccessEndTimestamp;
    protected long lastSuccessDuration;
    protected long totalSuccessDuration;
    protected int totalSuccessCount;

    protected String lastFailureObjectName;
    protected String lastFailureObjectDisplayName;
    protected QName lastFailureObjectType;
    protected String lastFailureObjectOid;
    protected Date lastFailureEndTimestamp;
    protected long lastFailureDuration;
    protected long totalFailureDuration;
    protected int totalFailureCount;
    protected Throwable lastFailureException;
    protected String lastFailureExceptionMessage;

    protected String currentObjectName;
    protected String currentObjectDisplayName;
    protected QName currentObjectType;
    protected String currentObjectOid;
    protected Date currentObjectStartTimestamp;

    protected CircularFifoBuffer lastFailures = new CircularFifoBuffer(LAST_FAILURES_KEPT);

    public IterativeTaskInformation() {
        this(null);
    }

    public IterativeTaskInformation(IterativeTaskInformationType value) {
        startValue = value;
    }

    public IterativeTaskInformationType getStartValue() {
        return startValue;
    }

    public synchronized IterativeTaskInformationType getDeltaValue() {
        IterativeTaskInformationType rv = toIterativeTaskInformationType();
        return rv;
    }

    public synchronized IterativeTaskInformationType getAggregatedValue() {
        IterativeTaskInformationType delta = toIterativeTaskInformationType();
        IterativeTaskInformationType rv = aggregate(startValue, delta);
        return rv;
    }

    private IterativeTaskInformationType aggregate(IterativeTaskInformationType startValue, IterativeTaskInformationType delta) {
        if (startValue == null) {
            return delta;
        }
        IterativeTaskInformationType rv = new IterativeTaskInformationType();
        addTo(rv, startValue, true);
        addTo(rv, delta, true);
        return rv;
    }

    private IterativeTaskInformationType toIterativeTaskInformationType() {
        IterativeTaskInformationType rv = new IterativeTaskInformationType();
        toJaxb(rv);
        return rv;
    }

    public synchronized void recordOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid, long started, Throwable exception) {
        if (exception != null) {
            lastFailureObjectName = objectName;
            lastFailureObjectDisplayName = objectDisplayName;
            lastFailureObjectType = objectType;
            lastFailureObjectOid = objectOid;
            lastFailureEndTimestamp = new Date();
            lastFailureDuration = lastFailureEndTimestamp.getTime() - started;
            lastFailureException = exception;
            lastFailureExceptionMessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            totalFailureDuration += lastFailureDuration;
            totalFailureCount++;

            String name = objectName != null ? objectName
                    : objectOid != null ? objectOid : "(unnamed)";
            lastFailures.add(name + ": " + exception.getMessage());
        } else {
            lastSuccessObjectName = objectName;
            lastSuccessObjectDisplayName = objectDisplayName;
            lastSuccessObjectType = objectType;
            lastSuccessObjectOid = objectOid;
            lastSuccessEndTimestamp = new Date();
            lastSuccessDuration = lastSuccessEndTimestamp.getTime() - started;
            totalSuccessDuration += lastSuccessDuration;
            totalSuccessCount++;
        }
        currentObjectName = null;
        currentObjectDisplayName = null;
        currentObjectType = null;
        currentObjectOid = null;
        currentObjectStartTimestamp = null;
    }

    public synchronized void recordOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid) {
        currentObjectName = objectName;
        currentObjectDisplayName = objectDisplayName;
        currentObjectType = objectType;
        currentObjectOid = objectOid;
        currentObjectStartTimestamp = new Date();
    }

    private void toJaxb(IterativeTaskInformationType rv) {
        rv.setLastSuccessObjectName(lastSuccessObjectName);
        rv.setLastSuccessObjectDisplayName(lastSuccessObjectDisplayName);
        rv.setLastSuccessObjectType(lastSuccessObjectType);
        rv.setLastSuccessObjectOid(lastSuccessObjectOid);
        rv.setLastSuccessEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(lastSuccessEndTimestamp));
        rv.setLastSuccessDuration(lastSuccessDuration);
        rv.setTotalSuccessDuration(totalSuccessDuration);
        rv.setTotalSuccessCount(totalSuccessCount);

        rv.setLastFailureObjectName(lastFailureObjectName);
        rv.setLastFailureObjectDisplayName(lastFailureObjectDisplayName);
        rv.setLastFailureObjectType(lastFailureObjectType);
        rv.setLastFailureObjectOid(lastFailureObjectOid);
        rv.setLastFailureEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(lastFailureEndTimestamp));
        rv.setLastFailureDuration(lastFailureDuration);
        rv.setLastFailureExceptionMessage(lastFailureExceptionMessage);
        rv.setTotalFailureDuration(totalFailureDuration);
        rv.setTotalFailureCount(totalFailureCount);

        rv.setCurrentObjectName(currentObjectName);
        rv.setCurrentObjectDisplayName(currentObjectDisplayName);
        rv.setCurrentObjectType(currentObjectType);
        rv.setCurrentObjectOid(currentObjectOid);
        rv.setCurrentObjectStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar(currentObjectStartTimestamp));
    }

    // sum != null, delta != null
    // overrideCurrent should be TRUE if the delta is chronologically later (i.e. if delta is meant as an update to sum)
    // if it is simply an aggregation of various (parallel) sources, overrideCurrent should be FALSE
    public static void addTo(IterativeTaskInformationType sum, IterativeTaskInformationType delta, boolean overrideCurrent) {
        if (sum.getLastSuccessEndTimestamp() == null || (delta.getLastSuccessEndTimestamp() != null &&
                delta.getLastSuccessEndTimestamp().compare(sum.getLastSuccessEndTimestamp()) == DatatypeConstants.GREATER)) {
            sum.setLastSuccessObjectName(delta.getLastSuccessObjectName());
            sum.setLastSuccessObjectDisplayName(delta.getLastSuccessObjectDisplayName());
            sum.setLastSuccessObjectType(delta.getLastSuccessObjectType());
            sum.setLastSuccessObjectOid(delta.getLastSuccessObjectOid());
            sum.setLastSuccessEndTimestamp(delta.getLastSuccessEndTimestamp());
            sum.setLastSuccessDuration(delta.getLastSuccessDuration());
        }
        sum.setTotalSuccessDuration(sum.getTotalSuccessDuration() + delta.getTotalSuccessDuration());
        sum.setTotalSuccessCount(sum.getTotalSuccessCount() + delta.getTotalSuccessCount());

        if (sum.getLastFailureEndTimestamp() == null || (delta.getLastFailureEndTimestamp() != null &&
                delta.getLastFailureEndTimestamp().compare(sum.getLastFailureEndTimestamp()) == DatatypeConstants.GREATER)) {
            sum.setLastFailureObjectName(delta.getLastFailureObjectName());
            sum.setLastFailureObjectDisplayName(delta.getLastFailureObjectDisplayName());
            sum.setLastFailureObjectType(delta.getLastFailureObjectType());
            sum.setLastFailureObjectOid(delta.getLastFailureObjectOid());
            sum.setLastFailureEndTimestamp(delta.getLastFailureEndTimestamp());
            sum.setLastFailureDuration(delta.getLastFailureDuration());
            sum.setLastFailureExceptionMessage(delta.getLastFailureExceptionMessage());
        }
        sum.setTotalFailureDuration(sum.getTotalFailureDuration() + delta.getTotalFailureDuration());
        sum.setTotalFailureCount(sum.getTotalFailureCount() + delta.getTotalFailureCount());

        if (overrideCurrent || sum.getCurrentObjectStartTimestamp() == null || (delta.getCurrentObjectStartTimestamp() != null &&
                delta.getCurrentObjectStartTimestamp().compare(sum.getCurrentObjectStartTimestamp()) == DatatypeConstants.GREATER)) {
            sum.setCurrentObjectName(delta.getCurrentObjectName());
            sum.setCurrentObjectDisplayName(delta.getCurrentObjectDisplayName());
            sum.setCurrentObjectType(delta.getCurrentObjectType());
            sum.setCurrentObjectOid(delta.getCurrentObjectOid());
            sum.setCurrentObjectStartTimestamp(delta.getCurrentObjectStartTimestamp());
        }
    }

    public List<String> getLastFailures() {
        return new ArrayList<>(lastFailures);
    }
}
