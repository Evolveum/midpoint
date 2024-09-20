/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.reporting;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnIdOperationRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Objects;

public class ConnIdOperation {

    @NotNull private final String identifier;
    @NotNull private final ProvisioningOperation operation;
    @NotNull private final ObjectReferenceType resourceRef;
    private final ResourceObjectClassDefinition objectClassDef;
    private String uid;
    private final long startTimestamp;
    private long endTimestamp;
    private long lastSuspendTimestamp;
    private long lastResumeTimestamp;
    private long netRunningTime;
    private OperationResultStatus status;
    private String message;

    private ConnIdOperation(
            @NotNull String identifier,
            @NotNull ProvisioningOperation operation,
            @NotNull ObjectReferenceType resourceRef,
            ResourceObjectClassDefinition objectClassDef,
            String uid,
            long startTimestamp) {
        this.identifier = identifier;
        this.operation = operation;
        this.resourceRef = resourceRef;
        this.objectClassDef = objectClassDef;
        this.uid = uid;
        this.startTimestamp = startTimestamp != 0 ? startTimestamp : System.currentTimeMillis();
    }

    public static String getIdentifier(@Nullable ConnIdOperation operation) {
        return operation != null ? operation.getIdentifier() : null;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull ProvisioningOperation getOperation() {
        return operation;
    }

    public @NotNull ObjectReferenceType getResourceRef() {
        return resourceRef;
    }

    public @NotNull String getResourceOid() {
        return Objects.requireNonNull(
                resourceRef.getOid(), "no resource OID");
    }

    public @Nullable String getResourceName() {
        return PolyString.getOrig(
                resourceRef.getTargetName());
    }

    public ResourceObjectClassDefinition getObjectClassDef() {
        return objectClassDef;
    }

    public QName getObjectClassDefName() {
        return objectClassDef != null ? objectClassDef.getTypeName() : null;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public OperationResultStatus getStatus() {
        return status;
    }

    public void setStatus(OperationResultStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ConnIdOperation{" +
                "identifier='" + identifier + '\'' +
                ", operation=" + operation +
                ", resourceRef=" + resourceRef +
                ", objectClassDef=" + objectClassDef +
                ", uid='" + uid + '\'' +
                ", startTimestamp=" + startTimestamp +
                ", endTimestamp=" + endTimestamp +
                ", result=" + status +
                ", message='" + message + '\'' +
                '}';
    }

    public double getDuration() {
        return endTimestamp - startTimestamp;
    }

    public long getNetRunningTime() {
        return netRunningTime;
    }

    public boolean wasSuspended() {
        return lastSuspendTimestamp != 0;
    }

    /** Returns time spent from last start/resume to the current moment. */
    public long onSuspend() {
        lastSuspendTimestamp = System.currentTimeMillis();

        long duration = lastSuspendTimestamp - getLastExecutionSegmentStart();
        netRunningTime += duration;
        return duration;
    }

    public void onResume() {
        lastResumeTimestamp = System.currentTimeMillis();
    }

    public void onEnd() {
        endTimestamp = System.currentTimeMillis();
        netRunningTime += endTimestamp - getLastExecutionSegmentStart();
    }

    private long getLastExecutionSegmentStart() {
        return lastResumeTimestamp != 0 ? lastResumeTimestamp : startTimestamp;
    }

    public OperationResultStatusType getStatusBean() {
        return OperationResultStatus.createStatusType(status);
    }

    public ConnIdOperationRecordType toOperationRecordBean() {
        return new ConnIdOperationRecordType()
                .identifier(identifier)
                .resourceRef(resourceRef)
                .objectClass(getObjectClassDefName())
                .operation(String.valueOf(operation))
                .status(getStatusBean())
                .message(message)
                // TODO size
                .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(startTimestamp))
                .endTimestamp(XmlTypeConverter.createXMLGregorianCalendar(endTimestamp))
                .duration((double) getNetRunningTime()); // TODO
    }

    public boolean isNotFatalError() {
        return status != OperationResultStatus.FATAL_ERROR;
    }

    public static final class ConnIdOperationBuilder {
        private String identifier;
        private ProvisioningOperation operation;
        private ObjectReferenceType resourceRef;
        private ResourceObjectClassDefinition objectClassDef;
        private String uid;
        private long startTimestamp;
        private long endTimestamp;
        private OperationResultStatus status;
        private String message;

        private ConnIdOperationBuilder() {
        }

        public static ConnIdOperationBuilder aConnIdOperation() {
            return new ConnIdOperationBuilder();
        }

        public ConnIdOperationBuilder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public ConnIdOperationBuilder withOperation(ProvisioningOperation operation) {
            this.operation = operation;
            return this;
        }

        public ConnIdOperationBuilder withResourceRef(ObjectReferenceType value) {
            this.resourceRef = value;
            return this;
        }

        public ConnIdOperationBuilder withObjectClassDef(ResourceObjectClassDefinition objectClassDef) {
            this.objectClassDef = objectClassDef;
            return this;
        }

        public ConnIdOperationBuilder withUid(String uid) {
            this.uid = uid;
            return this;
        }

        public ConnIdOperationBuilder withStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
            return this;
        }

        public ConnIdOperationBuilder withEndTimestamp(long endTimestamp) {
            this.endTimestamp = endTimestamp;
            return this;
        }

        public ConnIdOperationBuilder withStatus(OperationResultStatus status) {
            this.status = status;
            return this;
        }

        public ConnIdOperationBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        public ConnIdOperation build() {
            ConnIdOperation connIdOperation = new ConnIdOperation(identifier, operation, resourceRef, objectClassDef, uid, startTimestamp);
            connIdOperation.status = this.status;
            connIdOperation.message = this.message;
            connIdOperation.endTimestamp = this.endTimestamp;
            return connIdOperation;
        }
    }
}
