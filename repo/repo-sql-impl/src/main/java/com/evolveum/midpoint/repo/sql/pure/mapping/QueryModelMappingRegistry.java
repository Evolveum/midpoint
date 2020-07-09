package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * Holds {@link QueryModelMapping} instances obtainable by various key (e.g. model type Q-name).
 * The registry is oblivious to the actual configuration that is in {@link QueryModelMappingConfig}.
 */
public class QueryModelMappingRegistry {

    private final Map<QName, QueryModelMapping<?, ?>> mappingByQName = new HashMap<>();
    private final Map<Class<?>, QueryModelMapping<?, ?>> mappingByModelType = new HashMap<>();

    public QueryModelMappingRegistry register(
            QName modelTypeQName, QueryModelMapping<?, ?> mapping) {

        // TODO check overrides, throw exception
        mappingByQName.put(modelTypeQName, mapping);
        mappingByModelType.put(mapping.modelType(), mapping);

        // TODO check defaultAliasName uniqueness
        return this;
    }

    public <M> QueryModelMapping<M, ?> getByModelType(Class<M> modelType) {
        //noinspection unchecked
        return (QueryModelMapping<M, ?>) mappingByModelType.get(modelType);
    }
}
