package com.evolveum.midpoint.repo.sql.pure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Base class for SQL transformers translating from query beans or tuples to model types.
 */
public abstract class SqlTransformer<M, R> {

    protected final PrismContext prismContext;

    protected SqlTransformer(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    /**
     * Transforms row of R to M type - typically a model/schema object.
     * If pre-generated bean is used as row it does not include extension (dynamic) columns,
     * which is OK if extension columns are used only for query and their information
     * is still contained in the object somehow else (e.g. full object LOB).
     * <p>
     * Alternative would be dynamically generated list of select expressions and transforming
     * row to M object directly from {@link com.querydsl.core.Tuple}.
     */
    public abstract M toSchemaObject(R row);

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link RObjectType} and, optionally, description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    @Nullable
    protected ObjectReferenceType objectReferenceType(
            @Nullable String oid, RObjectType repoObjectType, String description) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(prismContext.getSchemaRegistry().determineTypeForClass(
                        repoObjectType.getJaxbClass()))
                .description(description);
    }

    /**
     * Returns {@link RObjectType} from ordinal Integer or specified default value.
     */
    protected @NotNull RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId, @NotNull RObjectType defaultValue) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : defaultValue;
    }

    /**
     * Returns nullable {@link RObjectType} from ordinal Integer.
     * If null is returned it will not fail immediately unlike {@link RObjectType#fromOrdinal(int)}.
     * This is practical for eager argument resolution for
     * {@link #objectReferenceType(String, RObjectType, String)}.
     * Null may still be OK if OID is null as well - which means no reference.
     */
    protected @Nullable RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : null;
    }
}
