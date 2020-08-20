/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.mapping;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Simple class with methods for audit event transformation between repo and Prism world.
 */
public class AuditDeltaSqlTransformer
        extends SqlTransformer<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    public AuditDeltaSqlTransformer(PrismContext prismContext, QAuditDeltaMapping mapping) {
        super(prismContext, mapping);
    }

    public ObjectDeltaOperationType toSchemaObject(MAuditDelta row) throws SchemaException {
        ObjectDeltaOperation<ObjectType> delta = new ObjectDeltaOperation<>();

        ObjectDeltaOperationType odo = new ObjectDeltaOperationType();
        return odo;
        /*
        TODO MID-6319
        DeltaConversionOptions options = DeltaConversionOptions.createSerializeReferenceNames();
        // This can be tricky because it can create human-readable but machine-unprocessable
        // data, see MID-6262. But in current context the results of this method are to be
        // used only in GUI and reports, so we are safe here.
        // THIS ^^ is taken originally from AuditEventRecord#createAuditEventRecordType(boolean)
        options.setEscapeInvalidCharacters(true);
        DeltaConvertor.toObjectDeltaOperationType(delta, odo, options);
        auditRecordType.getDelta().add(odo);

        objectDeltaOperation.;
        record.delta(objectDeltaOperation);
        */
    }
}
