/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Objects;

public record IndexedRef(@NotNull String path,
                         @NotNull String targetOid,
                         @Nullable QName targetType,
                         @NotNull QName relation) {
    /**
     * Returns null when the reference has no OID: a target given only by a filter is not indexed.
     */
    public static @Nullable IndexedRef of(@NotNull String path, @Nullable ObjectReferenceType ref) {
        if (ref == null || ref.getOid() == null) {
            return null;
        }
        QName relation = ref.getRelation() != null
                ? ref.getRelation()
                : PrismContext.get().getDefaultRelation();
        return new IndexedRef(path, ref.getOid(), targetType(ref), relation);

    }

    private static QName targetType(ObjectReferenceType ref) {
        if (ref.getType() != null) {
            return ref.getType();
        }
        PrismReferenceDefinition definition = ref.asReferenceValue().getDefinition();
        return definition != null ? definition.getTargetTypeName() : null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IndexedRef other
                && path.equals(other.path)
                && targetOid.equals(other.targetOid)
                && relation.equals(other.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, targetOid, relation);
    }
}
