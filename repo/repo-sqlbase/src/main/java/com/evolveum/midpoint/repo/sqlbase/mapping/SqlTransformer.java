package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.Collection;

import com.querydsl.core.Tuple;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Contract for SQL transformers translating from query beans or tuples to model types.
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> type of the transformed data, a row bean
 */
public interface SqlTransformer<S, Q extends FlexibleRelationalPathBase<R>, R> {

    /**
     * Transforms row of {@link R} type to schema type {@link S}.
     * If pre-generated bean is used as row it does not include extension (dynamic) columns,
     * which is OK if extension columns are used only for query and their information
     * is still contained in the object somehow else (e.g. full object LOB).
     * <p>
     * Alternative would be dynamically generated list of select expressions and transforming
     * row to M object directly from {@link com.querydsl.core.Tuple}.
     */
    S toSchemaObject(R row) throws SchemaException;

    /**
     * Transforms row Tuple containing attributes of {@link R} to schema type {@link S}.
     * Entity path can be used to access tuple elements.
     * This allows loading also dynamically defined columns (like extensions).
     */
    S toSchemaObject(Tuple row, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException;

    default S toSchemaObjectSafe(Tuple tuple, Q entityPath, Collection<SelectorOptions<GetOperationOptions>> options) {
        try {
            return toSchemaObject(tuple, entityPath, options);
        } catch (SchemaException e) {
            throw new SqlTransformationException(e);
        }
    }

    class SqlTransformationException extends RuntimeException {
        public SqlTransformationException(Throwable cause) {
            super(cause);
        }
    }
}
