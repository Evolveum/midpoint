/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class LensObjectDeltaOperation<T extends ObjectType> extends ObjectDeltaOperation<T> implements Serializable {

    private boolean audited = false;

    public LensObjectDeltaOperation() {
        super();
    }

    public LensObjectDeltaOperation(ObjectDelta<T> objectDelta) {
        super(objectDelta);
    }

    public boolean isAudited() {
        return audited;
    }

    public void setAudited(boolean audited) {
        this.audited = audited;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "audited", audited, indent + 1);
        return sb.toString();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "LensObjectDeltaOperation";
    }

    @NotNull LensObjectDeltaOperationType toLensObjectDeltaOperationType() throws SchemaException {
        LensObjectDeltaOperationType retval = new LensObjectDeltaOperationType();
        DeltaConversionOptions options = DeltaConversionOptions.createSerializeReferenceNames();
        // Escaping invalid characters in serialized deltas could be questionable; see MID-6262.
        // But because we currently do not use the deltas for other than human readers we can afford
        // to replace invalid characters by descriptive text.
        options.setEscapeInvalidCharacters(true);
        retval.setObjectDeltaOperation(DeltaConvertor.toObjectDeltaOperationType(this, options));
        retval.setAudited(audited);
        return retval;
    }

    public static LensObjectDeltaOperation fromLensObjectDeltaOperationType(LensObjectDeltaOperationType jaxb, PrismContext prismContext) throws SchemaException {

        ObjectDeltaOperation odo = DeltaConvertor.createObjectDeltaOperation(jaxb.getObjectDeltaOperation(), prismContext);
        LensObjectDeltaOperation retval = new LensObjectDeltaOperation();
        retval.setObjectDelta(odo.getObjectDelta());
        retval.setExecutionResult(odo.getExecutionResult());
        retval.setAudited(jaxb.isAudited());
        return retval;
    }

    public LensObjectDeltaOperation<T> clone() {
        LensObjectDeltaOperation<T> clone = new LensObjectDeltaOperation<>();
        super.copyToClone(clone);
        copyToClone(clone);
        return clone;
    }

    protected void copyToClone(LensObjectDeltaOperation<T> clone) {
        clone.audited = this.audited;
    }

}
