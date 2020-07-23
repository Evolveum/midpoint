/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation.COLUMN_RECORD_ID;
import static com.evolveum.midpoint.schema.util.SystemConfigurationAuditUtil.isEscapingInvalidCharacters;

import java.sql.ResultSet;
import java.util.Objects;
import javax.persistence.*;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.InsertQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.data.common.OperationResultFull;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RChangeType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RObjectDeltaOperationId.class)
@Table(name = RObjectDeltaOperation.TABLE_NAME, indexes = {
        @Index(name = "iAuditDeltaRecordId", columnList = COLUMN_RECORD_ID) })
public class RObjectDeltaOperation implements OperationResultFull, EntityState {

    private static final long serialVersionUID = -1065600513263271161L;

    public static final String TABLE_NAME = "m_audit_delta";
    public static final String COLUMN_RECORD_ID = "record_id";

    private static final String CHECKSUM_COLUMN_NAME = "checksum";
    private static final String DELTA_COLUMN_NAME = "delta";
    private static final String DELTA_OID_COLUMN_NAME = "deltaOid";
    private static final String DELTA_TYPE_COLUMN_NAME = "deltaType";
    private static final String FULL_RESULT_COLUMN_NAME = "fullResult";
    private static final String OBJECT_NAME_NORM_COLUMN_NAME = "objectName_norm";
    private static final String OBJECT_NAME_ORIG_COLUMN_NAME = "objectName_orig";
    private static final String RESOURCE_NAME_NORM_COLUMN_NAME = "resourceName_norm";
    private static final String RESOURCE_NAME_ORIG_COLUMN_NAME = "resourceName_orig";
    private static final String RESOURCE_OID_COLUMN_NAME = "resourceOid";
    private static final String STATUS_COLUMN_NAME = "status";

    private Boolean trans;

    private RAuditEventRecord record;
    private Long recordId;

    //delta
    private byte[] delta;
    private String checksum;
    private String deltaOid;
    private RChangeType deltaType;

    //operation result
    private ROperationResultStatus status;
    private byte[] fullResult;

    // additional info from ObjectDeltaOperationType
    private RPolyString objectName;
    private String resourceOid;
    private RPolyString resourceName;

    @ForeignKey(name = "none")
    @MapsId("record")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = COLUMN_RECORD_ID, referencedColumnName = "id")
    })
    public RAuditEventRecord getRecord() {
        return record;
    }

    @Id
    @Column(name = COLUMN_RECORD_ID)
    public Long getRecordId() {
        if (recordId == null && record != null) {
            recordId = record.getId();
        }
        return recordId;
    }

    /**
     * This method is used for content comparing when querying database (we don't want to compare clob values).
     *
     * @return md5 hash of {@link RObjectDeltaOperation#delta} and {@link RObjectDeltaOperation#fullResult}
     */
    @Id
    @Column(length = 32, name = "checksum")
    public String getChecksum() {
        if (checksum == null) {
            recomputeChecksum();
        }

        return checksum;
    }

    @Lob
    public byte[] getDelta() {
        return delta;
    }

    @Enumerated(EnumType.ORDINAL)
    public RChangeType getDeltaType() {
        return deltaType;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getDeltaOid() {
        return deltaOid;
    }

    @Lob
    public byte[] getFullResult() {
        return fullResult;
    }

    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getStatus() {
        return status;
    }

    @Embedded
    public RPolyString getObjectName() {
        return objectName;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getResourceOid() {
        return resourceOid;
    }

    @Embedded
    public RPolyString getResourceName() {
        return resourceName;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public void setRecord(RAuditEventRecord record) {
        if (record.getId() != 0) {
            this.recordId = record.getId();
        }
        this.record = record;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public void setChecksum(String checksum) {
        //checksum is always computed from delta and result, this setter is only to satisfy hibernate
    }

    public void setDelta(byte[] delta) {
        this.delta = delta;

        recomputeChecksum();
    }

    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setFullResult(byte[] fullResult) {
        this.fullResult = fullResult;

        recomputeChecksum();
    }

    public void setDeltaType(RChangeType deltaType) {
        this.deltaType = deltaType;
    }

    public void setDeltaOid(String deltaOid) {
        this.deltaOid = deltaOid;
    }

    public void setObjectName(RPolyString objectName) {
        this.objectName = objectName;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public void setResourceName(RPolyString resourceName) {
        this.resourceName = resourceName;
    }

    @Transient
    private void recomputeChecksum() {
        checksum = RUtil.computeChecksum(delta, fullResult);
    }

    public static RObjectDeltaOperation toRepo(RAuditEventRecord record, ObjectDeltaOperation operation,
            PrismContext prismContext, SystemConfigurationAuditType auditConfiguration) throws DtoTranslationException {
        RObjectDeltaOperation auditDelta = new RObjectDeltaOperation();
        auditDelta.setRecord(record);

        try {
            if (operation.getObjectDelta() != null) {
                ObjectDelta delta = operation.getObjectDelta();

                DeltaConversionOptions options = DeltaConversionOptions.createSerializeReferenceNames();
                options.setEscapeInvalidCharacters(isEscapingInvalidCharacters(auditConfiguration));
                String xmlDelta = DeltaConvertor.toObjectDeltaTypeXml(delta, options);
                byte[] data = RUtil.getBytesFromSerializedForm(xmlDelta, true);
                auditDelta.setDelta(data);

                auditDelta.setDeltaOid(delta.getOid());
                auditDelta.setDeltaType(RUtil.getRepoEnumValue(delta.getChangeType(), RChangeType.class));
            }

            if (operation.getExecutionResult() != null) {
                RUtil.copyResultFromJAXB(SchemaConstantsGenerated.C_OPERATION_RESULT,
                        operation.getExecutionResult().createOperationResultType(),
                        auditDelta, prismContext);
            }

            auditDelta.setObjectName(RPolyString.toRepo(operation.getObjectName()));
            auditDelta.setResourceOid(operation.getResourceOid());
            auditDelta.setResourceName(RPolyString.toRepo(operation.getResourceName()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return auditDelta;
    }

    public static SingleSqlQuery toRepo(Long recordId, ObjectDeltaOperation operation,
            PrismContext prismContext, SystemConfigurationAuditType auditConfiguration) throws DtoTranslationException {

        InsertQueryBuilder queryBuilder = new InsertQueryBuilder(TABLE_NAME);
        queryBuilder.addParameter(COLUMN_RECORD_ID, recordId, true);
        byte[] deltaData = null;
        byte[] fullResultData = null;
        try {
            if (operation.getObjectDelta() != null) {
                ObjectDelta delta = operation.getObjectDelta();

                DeltaConversionOptions options = DeltaConversionOptions.createSerializeReferenceNames();
                options.setEscapeInvalidCharacters(isEscapingInvalidCharacters(auditConfiguration));
                String xmlDelta = DeltaConvertor.toObjectDeltaTypeXml(delta, options);
                deltaData = RUtil.getBytesFromSerializedForm(xmlDelta, true);
                queryBuilder.addParameter(DELTA_COLUMN_NAME, deltaData);
                queryBuilder.addParameter(DELTA_OID_COLUMN_NAME, delta.getOid());
                queryBuilder.addParameter(DELTA_TYPE_COLUMN_NAME, RUtil.getRepoEnumValue(delta.getChangeType(), RChangeType.class));
            } else {
                queryBuilder.addNullParameter(DELTA_COLUMN_NAME);
                queryBuilder.addNullParameter(DELTA_OID_COLUMN_NAME);
                queryBuilder.addNullParameter(DELTA_TYPE_COLUMN_NAME);
            }

            if (operation.getExecutionResult() != null) {
                OperationResultType jaxb = operation.getExecutionResult().createOperationResultType();
                if (jaxb == null) {
                    queryBuilder.addNullParameter(STATUS_COLUMN_NAME);
                    queryBuilder.addNullParameter(FULL_RESULT_COLUMN_NAME);
                } else {
                    queryBuilder.addParameter(STATUS_COLUMN_NAME, RUtil.getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
                    try {
                        String full = prismContext.xmlSerializer()
                                .options(SerializationOptions.createEscapeInvalidCharacters())
                                .serializeRealValue(jaxb, SchemaConstantsGenerated.C_OPERATION_RESULT);
                        fullResultData = RUtil.getBytesFromSerializedForm(full, true);
                        queryBuilder.addParameter(FULL_RESULT_COLUMN_NAME, fullResultData);
                    } catch (Exception ex) {
                        throw new DtoTranslationException(ex.getMessage(), ex);
                    }
                }
            } else {
                queryBuilder.addNullParameter(STATUS_COLUMN_NAME);
                queryBuilder.addNullParameter(FULL_RESULT_COLUMN_NAME);
            }
            if (operation.getObjectName() != null) {
                queryBuilder.addParameter(OBJECT_NAME_ORIG_COLUMN_NAME, operation.getObjectName().getOrig());
                queryBuilder.addParameter(OBJECT_NAME_NORM_COLUMN_NAME, operation.getObjectName().getNorm());
            } else {
                queryBuilder.addNullParameter(OBJECT_NAME_ORIG_COLUMN_NAME);
                queryBuilder.addNullParameter(OBJECT_NAME_NORM_COLUMN_NAME);
            }
            queryBuilder.addParameter(RESOURCE_OID_COLUMN_NAME, operation.getResourceOid());
            if (operation.getResourceName() != null) {
                queryBuilder.addParameter(RESOURCE_NAME_ORIG_COLUMN_NAME, operation.getResourceName().getOrig());
                queryBuilder.addParameter(RESOURCE_NAME_NORM_COLUMN_NAME, operation.getResourceName().getNorm());
            } else {
                queryBuilder.addNullParameter(RESOURCE_NAME_ORIG_COLUMN_NAME);
                queryBuilder.addNullParameter(RESOURCE_NAME_NORM_COLUMN_NAME);
            }
            queryBuilder.addParameter(CHECKSUM_COLUMN_NAME, RUtil.computeChecksum(deltaData, fullResultData), true);
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return queryBuilder.build();
    }

    @NotNull
    public static ObjectDeltaOperation fromRepo(RObjectDeltaOperation operation, PrismContext prismContext, boolean useUtf16)
            throws DtoTranslationException {

        ObjectDeltaOperation odo = new ObjectDeltaOperation();
        try {
            if (operation.getDelta() != null) {
                byte[] data = operation.getDelta();
                String serializedDelta = RUtil.getSerializedFormFromBytes(data, useUtf16);

                ObjectDeltaType delta = prismContext.parserFor(serializedDelta)
                        .parseRealValue(ObjectDeltaType.class);
                odo.setObjectDelta(DeltaConvertor.createObjectDelta(delta, prismContext));
            }
            if (operation.getFullResult() != null) {
                byte[] data = operation.getFullResult();
                String serializedResult = RUtil.getSerializedFormFromBytes(data, useUtf16);

                OperationResultType resultType = prismContext.parserFor(serializedResult)
                        .parseRealValue(OperationResultType.class);
                odo.setExecutionResult(OperationResult.createOperationResult(resultType));
            }
            odo.setObjectName(RPolyString.fromRepo(operation.getObjectName(), prismContext));
            odo.setResourceOid(operation.getResourceOid());
            odo.setResourceName(RPolyString.fromRepo(operation.getResourceName(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return odo;
    }

    @NotNull
    public static ObjectDeltaOperation fromRepo(ResultSet resultSet, PrismContext prismContext, boolean useUtf16)
            throws DtoTranslationException {

        ObjectDeltaOperation odo = new ObjectDeltaOperation();
        try {
            if (resultSet.getBytes(DELTA_COLUMN_NAME) != null) {
                byte[] data = resultSet.getBytes(DELTA_COLUMN_NAME);
                String serializedDelta = RUtil.getSerializedFormFromBytes(data, useUtf16);

                ObjectDeltaType delta = prismContext.parserFor(serializedDelta)
                        .parseRealValue(ObjectDeltaType.class);
                odo.setObjectDelta(DeltaConvertor.createObjectDelta(delta, prismContext));
            }
            if (resultSet.getBytes(FULL_RESULT_COLUMN_NAME) != null) {
                byte[] data = resultSet.getBytes(FULL_RESULT_COLUMN_NAME);
                String serializedResult = RUtil.getSerializedFormFromBytes(data, useUtf16);

                OperationResultType resultType = prismContext.parserFor(serializedResult)
                        .parseRealValue(OperationResultType.class);
                odo.setExecutionResult(OperationResult.createOperationResult(resultType));
            }
            if (resultSet.getString(OBJECT_NAME_ORIG_COLUMN_NAME) != null
                    || resultSet.getString(OBJECT_NAME_NORM_COLUMN_NAME) != null) {
                odo.setObjectName(new PolyString(
                        resultSet.getString(OBJECT_NAME_ORIG_COLUMN_NAME),
                        resultSet.getString(OBJECT_NAME_NORM_COLUMN_NAME)));
            }
            odo.setResourceOid(resultSet.getString(RESOURCE_OID_COLUMN_NAME));
            if (resultSet.getString(RESOURCE_NAME_ORIG_COLUMN_NAME) != null
                    || resultSet.getString(RESOURCE_NAME_NORM_COLUMN_NAME) != null) {
                odo.setResourceName(new PolyString(
                        resultSet.getString(RESOURCE_NAME_ORIG_COLUMN_NAME),
                        resultSet.getString(RESOURCE_NAME_NORM_COLUMN_NAME)));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return odo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RObjectDeltaOperation)) { return false; }

        RObjectDeltaOperation that = (RObjectDeltaOperation) o;
        return Objects.equals(recordId, that.recordId)
                && Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, checksum);
    }
}
