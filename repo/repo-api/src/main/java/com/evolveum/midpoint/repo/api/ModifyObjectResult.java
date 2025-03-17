/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.perf.OperationRecord;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 *  Contains information about object modification result; primarily needed by repository caching algorithms.
 *  Because it is bound to the current (SQL) implementation of the repository, avoid using this information
 *  for any other purposes.
 *
 *  Note that objectAfter might be null if the object XML representation was not changed.
 *  It is currently the case for lookup tables (when rows are modified) and certification campaigns (when cases are modified).
 *  In all other cases these are non-null.
 *
 *  As split objects are there, it is also not always complete. TODO decide what to do with it.
 */
@Experimental
public class ModifyObjectResult<T extends ObjectType> implements RepositoryOperationResult {

    private final PrismObject<T> objectAfter;
    private final Collection<? extends ItemDelta<?, ?>> modifications;
    private final boolean overwrite;

    /**
     * Performance record for the current operation.
     * Very experimental. Probably should be present also for other repository operation result objects.
     */
    private OperationRecord performanceRecord;

    public ModifyObjectResult(Collection<? extends ItemDelta<?, ?>> modifications) {
        this(null, modifications, false);
    }

    public ModifyObjectResult(PrismObject<T> objectAfter,
            Collection<? extends ItemDelta<?, ?>> modifications) {
        this(objectAfter, modifications, false);
    }

    public ModifyObjectResult(PrismObject<T> objectAfter, Collection<? extends ItemDelta<?, ?>> modifications, boolean overwrite) {
        this.objectAfter = objectAfter;
        this.modifications = modifications;
        this.overwrite = overwrite;
    }

    public PrismObject<T> getObjectAfter() {
        return objectAfter;
    }

    public Collection<? extends ItemDelta<?, ?>> getModifications() {
        return modifications;
    }

    public void setPerformanceRecord(OperationRecord performanceRecord) {
        this.performanceRecord = performanceRecord;
    }

    public int getRetries() {
        return performanceRecord != null ? performanceRecord.getAttempts() - 1 : 0;
    }

    public long getWastedTime() {
        return performanceRecord != null ? performanceRecord.getWastedTime() : 0;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    @Override
    public ChangeType getChangeType() {
        return ChangeType.MODIFY;
    }

    @Override
    public @Nullable QName getShadowObjectClassName() {
        T stateAfter = asObjectable(objectAfter);
        if (!(stateAfter instanceof ShadowType shadow)) {
            return null;
        }

        if (modifications.stream().anyMatch(delta -> delta.getPath().equivalent(ShadowType.F_OBJECT_CLASS))) {
            return null; // To be safe, let's consider this situation as "unknown"
        }

        // Finally, we assume that object class is present in objectAfter if it's not null. TODO check this
        return shadow.getObjectClass();
    }

    @Override
    public String toString() {
        return "ModifyObjectResult{" +
                "objectAfter=" + objectAfter +
                ", modifications=" + modifications +
                ", performanceRecord=" + performanceRecord +
                '}';
    }
}
