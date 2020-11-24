package com.evolveum.midpoint.repo.sqlbase.mapping;

import com.querydsl.core.Tuple;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Contract for SQL transformers translating from query beans or tuples to model types.
 *
 * @param <S> schema type
 * @param <R> type of the transformed data, a row bean
 */
public interface SqlTransformer<S, Q extends FlexibleRelationalPathBase<R>, R> {

    /**
     * Transforms row of R to M type - typically a model/schema object.
     * If pre-generated bean is used as row it does not include extension (dynamic) columns,
     * which is OK if extension columns are used only for query and their information
     * is still contained in the object somehow else (e.g. full object LOB).
     * <p>
     * Alternative would be dynamically generated list of select expressions and transforming
     * row to M object directly from {@link com.querydsl.core.Tuple}.
     */
    S toSchemaObject(R row) throws SchemaException;

    S toSchemaObjectSafe(Tuple row, Q entityPath);
    public static class SqlTransformationException extends RuntimeException {
        public SqlTransformationException(Throwable cause) {
            super(cause);
        }
    }
}
