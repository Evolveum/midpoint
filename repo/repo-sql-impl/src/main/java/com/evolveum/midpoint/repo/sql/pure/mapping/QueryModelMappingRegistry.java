package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import com.querydsl.core.types.EntityPath;

/**
 * Holds {@link QueryModelMapping} instances obtainable by various key (e.g. model type Q-name).
 * The registry is oblivious to the actual configuration that is in {@link QueryModelMappingConfig}.
 */
public class QueryModelMappingRegistry {

    private final Map<QName, QueryModelMapping<?, ?, ?>> mappingByQName = new HashMap<>();
    private final Map<Class<?>, QueryModelMapping<?, ?, ?>> mappingByModelType = new HashMap<>();
    private final Map<Class<? extends EntityPath<?>>, QueryModelMapping<?, ?, ?>>
            mappingByQueryType = new HashMap<>();

    public QueryModelMappingRegistry register(
            QName modelTypeQName, QueryModelMapping<?, ?, ?> mapping) {

        // TODO check overrides, throw exception
        mappingByQName.put(modelTypeQName, mapping);
        mappingByModelType.put(mapping.modelType(), mapping);
        mappingByQueryType.put(mapping.queryType(), mapping);

        // TODO check defaultAliasName uniqueness
        return this;
    }

    /**
     * Register mapper not bound to a model type.
     * This can happen for detail tables that have no unique mapping from model type.
     */
    public QueryModelMappingRegistry register(QueryModelMapping<?, ?, ?> mapping) {

        // TODO check overrides, throw exception
        mappingByQueryType.put(mapping.queryType(), mapping);

        // TODO check defaultAliasName uniqueness
        return this;
    }

    public <M, Q extends EntityPath<R>, R>
    QueryModelMapping<M, Q, R> getByModelType(Class<M> modelType) {
        //noinspection unchecked
        return (QueryModelMapping<M, Q, R>) mappingByModelType.get(modelType);
    }

    public <M, Q extends EntityPath<R>, R>
    QueryModelMapping<M, Q, R> getByQueryType(Class<Q> queryType) {
        //noinspection unchecked
        return (QueryModelMapping<M, Q, R>) mappingByQueryType.get(queryType);
    }
}
