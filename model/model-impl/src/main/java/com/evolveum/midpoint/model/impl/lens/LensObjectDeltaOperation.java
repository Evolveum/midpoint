/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 * Describes operation executed during the clockwork. Besides the {@link ObjectDeltaOperation} itself, it contains
 * additional data like whether the operation was audited, and (temporarily!) information about the original state of the object.
 *
 * @author semancik
 */
public class LensObjectDeltaOperation<T extends ObjectType>
        extends ObjectDeltaOperation<T>
        implements Serializable {

    private boolean audited;

    /** In what wave was this delta executed? */
    private int wave;

    /**
     * For `MODIFY` and `DELETE` deltas, this is the object state before the operation.
     *
     * Only for projections.
     *
     * Currently not serialized to beans, because we don't need that. (Approvals are invoked before any deltas are executed.)
     */
    private T baseObject;

    public LensObjectDeltaOperation() {
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

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public T getBaseObject() {
        return baseObject;
    }

    void setBaseObject(T baseObject) {
        this.baseObject = baseObject;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "wave", wave, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "audited", audited, indent + 1);
        return sb.toString();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "LensObjectDeltaOperation";
    }

    @NotNull
    public LensObjectDeltaOperationType toLensObjectDeltaOperationBean() throws SchemaException {
        LensObjectDeltaOperationType bean = new LensObjectDeltaOperationType();
        DeltaConversionOptions options = DeltaConversionOptions.createSerializeReferenceNames();
        // Escaping invalid characters in serialized deltas could be questionable; see MID-6262.
        // But because we currently do not use the deltas for other than human readers we can afford
        // to replace invalid characters by descriptive text.
        options.setEscapeInvalidCharacters(true);
        bean.setObjectDeltaOperation(DeltaConvertor.toObjectDeltaOperationType(this, options));
        bean.setAudited(audited);
        bean.setWave(wave);
        return bean;
    }

    static LensObjectDeltaOperation<?> fromLensObjectDeltaOperationType(LensObjectDeltaOperationType jaxb)
            throws SchemaException {
        var odo = DeltaConvertor.createObjectDeltaOperation(jaxb.getObjectDeltaOperation(), PrismContext.get());
        var retval = new LensObjectDeltaOperation<>(odo.getObjectDelta());
        retval.setExecutionResult(odo.getExecutionResult());
        retval.setAudited(jaxb.isAudited());
        retval.setWave(or0(jaxb.getWave())); // default is not very reasonable; we hope we won't parse this cross-versions
        return retval;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public LensObjectDeltaOperation<T> clone() {
        LensObjectDeltaOperation<T> clone = new LensObjectDeltaOperation<>();
        super.copyToClone(clone);
        copyToClone(clone);
        return clone;
    }

    private void copyToClone(LensObjectDeltaOperation<T> clone) {
        clone.audited = this.audited;
        clone.baseObject = CloneUtil.cloneCloneable(this.baseObject);
        clone.wave = wave;
    }
}
