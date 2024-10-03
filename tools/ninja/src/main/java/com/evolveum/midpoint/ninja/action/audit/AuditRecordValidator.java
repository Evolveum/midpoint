package com.evolveum.midpoint.ninja.action.audit;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceValueType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class AuditRecordValidator {


    private static final ItemName F_OID = new ItemName(PrismConstants.NS_TYPES, PrismConstants.ATTRIBUTE_OID_LOCAL_NAME);
    private static final ItemPath DELTA_RESOURCE_OID = ItemPath.create(
            AuditEventRecordType.F_DELTA, ObjectDeltaOperationType.F_RESOURCE_OID);
    private static final ItemPath DELTA_OBJECT_DELTA_OID = ItemPath.create(
            AuditEventRecordType.F_DELTA, ObjectDeltaOperationType.F_OBJECT_DELTA, F_OID);

    private static final ItemPath REFERENCE_VALUE_OID = ItemPath.create(
            AuditEventRecordType.F_REFERENCE, AuditEventRecordReferenceType.F_VALUE, AuditEventRecordReferenceValueType.F_OID);


    private final ObjectValidator validator;

    public AuditRecordValidator(ObjectValidator validator) {
        this.validator = validator;
    }

    public ValidationResult validate(AuditEventRecordType record) {
        var recordPcv = record.asPrismContainerValue();
        var result = validator.validate(recordPcv);

        checkAdditionalOid(result, recordPcv, AuditEventRecordType.F_RESOURCE_OID);
        checkAdditionalOid(result, recordPcv, AuditEventRecordType.F_TASK_OID);
        for (var delta : record.getDelta()) {
            checkDelta(result, delta);
        }
        for (var reference : record.getReference()) {
            checkAuditReference(result, reference);
        }
        return result;
    }



    private void checkDelta(ValidationResult result, ObjectDeltaOperationType delta) {
        // delta/resourceOid
        validator.checkOid(result, DELTA_RESOURCE_OID, delta.getResourceOid());
        checkObjectDelta(result, delta.getObjectDelta());

    }

    private void checkObjectDelta(ValidationResult result, ObjectDeltaType delta) {
        if (delta == null) {
            return;
        }
        // delta/objectDelta/oid
        validator.checkOid(result, DELTA_OBJECT_DELTA_OID, delta.getOid());
        // should we deep dive into object delta?
    }

    private void checkAuditReference(ValidationResult result, AuditEventRecordReferenceType reference) {
        if (reference == null) {
            return;
        }
        for (var ref : reference.getValue()) {
            validator.checkOid(result, REFERENCE_VALUE_OID ,ref.getOid());
        }
    }
    private void checkAdditionalOid(ValidationResult result, PrismContainerValue auditPcv, ItemName name) {
        var maybe = auditPcv.findItem(name);
        if (maybe instanceof PrismProperty) {
            var prop = (PrismProperty<String>) maybe;
            for (var val : prop.getValues()) {
                validator.checkOid(result, val, val.getRealValue());
            }
        }
    }

}
