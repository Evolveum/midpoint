/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;

import static com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation.COLUMN_RECORD_ID;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RObjectDeltaOperationId.class)
@Table(name = RObjectDeltaOperation.TABLE_NAME, indexes = {
        @Index(name = "iAuditDeltaRecordId", columnList = COLUMN_RECORD_ID)})
public class RObjectDeltaOperation implements OperationResultFull, EntityState {

    private static final long serialVersionUID = -1065600513263271161L;

    public static final String TABLE_NAME = "m_audit_delta";
    public static final String COLUMN_RECORD_ID = "record_id";

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectDeltaOperation that = (RObjectDeltaOperation) o;

        if (getChecksum() != null ? !getChecksum().equals(that.getChecksum()) : that.getChecksum() != null)
            return false;
        if (delta != null ? !delta.equals(that.delta) : that.delta != null) return false;
        if (fullResult != null ? !fullResult.equals(that.fullResult) : that.fullResult != null)
            return false;
        if (status != that.status) return false;
        if (deltaType != null ? !deltaType.equals(that.deltaType) : that.deltaType != null) return false;
        if (deltaOid != null ? !deltaOid.equals(that.deltaOid) : that.deltaOid != null) return false;
        if (objectName != null ? !objectName.equals(that.objectName) : that.objectName != null) return false;
        if (resourceOid != null ? !resourceOid.equals(that.resourceOid) : that.resourceOid != null) return false;
        if (resourceName != null ? !resourceName.equals(that.resourceName) : that.resourceName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = delta != null ? delta.hashCode() : 0;
        result1 = 31 * result1 + (getChecksum() != null ? getChecksum().hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);
        result1 = 31 * result1 + (fullResult != null ? fullResult.hashCode() : 0);
        result1 = 31 * result1 + (deltaOid != null ? deltaOid.hashCode() : 0);
        result1 = 31 * result1 + (deltaType != null ? deltaType.hashCode() : 0);
        result1 = 31 * result1 + (objectName != null ? objectName.hashCode() : 0);
        result1 = 31 * result1 + (resourceOid != null ? resourceOid.hashCode() : 0);
        result1 = 31 * result1 + (resourceName != null ? resourceName.hashCode() : 0);
        return result1;
    }

    public static RObjectDeltaOperation toRepo(RAuditEventRecord record, ObjectDeltaOperation operation,
                                               PrismContext prismContext) throws DtoTranslationException {
        RObjectDeltaOperation auditDelta = new RObjectDeltaOperation();
        auditDelta.setRecord(record);

        try {
            if (operation.getObjectDelta() != null) {
                ObjectDelta delta = operation.getObjectDelta();

                String xmlDelta = DeltaConvertor.toObjectDeltaTypeXml(delta, DeltaConversionOptions.createSerializeReferenceNames());
                byte[] data = RUtil.getByteArrayFromXml(xmlDelta, true);
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

    public static ObjectDeltaOperation fromRepo(RObjectDeltaOperation operation, PrismContext prismContext, boolean useUtf16)
            throws DtoTranslationException {

        ObjectDeltaOperation odo = new ObjectDeltaOperation();
        try {
            if (operation.getDelta() != null) {
                byte[] data = operation.getDelta();
                String xmlDelta = RUtil.getXmlFromByteArray(data, true, useUtf16);

                ObjectDeltaType delta = prismContext.parserFor(xmlDelta).parseRealValue(ObjectDeltaType.class);
                odo.setObjectDelta(DeltaConvertor.createObjectDelta(delta, prismContext));
            }
            if (operation.getFullResult() != null) {
                byte[] data = operation.getFullResult();
                String xmlResult = RUtil.getXmlFromByteArray(data, true, useUtf16);

                OperationResultType resultType = prismContext.parserFor(xmlResult).parseRealValue(OperationResultType.class);
                odo.setExecutionResult(OperationResult.createOperationResult(resultType));
            }
            odo.setObjectName(RPolyString.fromRepo(operation.getObjectName()));
            odo.setResourceOid(operation.getResourceOid());
            odo.setResourceName(RPolyString.fromRepo(operation.getResourceName()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return odo;
    }
}
