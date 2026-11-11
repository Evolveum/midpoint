package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Objects;

/**
 * One indexed reference of a policy rule: where in the rule it sits ({@link #path()},
 * e.g. {@code exclusion/targetRef} or {@code markRef}) and which object it points to.
 *
 * Equality follows the primary key of the ref tables (path, target OID, relation),
 * so a set of these never produces duplicate rows. The target type is carried along
 * but is not part of equality - the database key does not contain it either.
 */
public record IndexedRef(
        @NotNull String path,          // -> pathId (m_uri)
        @NotNull String targetOid,     // -> targetOid
        @Nullable QName targetType,    // -> targetType (MObjectType)
        @NotNull QName relation) {     // -> relationId (m_uri), never null

    /** Returns null when the reference has no OID (target given only by filter is not indexed). */
    public static @Nullable IndexedRef of(@NotNull String path, @Nullable ObjectReferenceType ref) {
        if (ref == null || ref.getOid() == null) {
            return null;
        }
        QName relation = ref.getRelation() != null
                ? ref.getRelation()
                : PrismContext.get().getDefaultRelation();
        return new IndexedRef(path, ref.getOid(), ref.getType(), relation);
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
