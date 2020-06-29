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

    public QueryModelMappingRegistry register(
            QName modelTypeQName, QueryModelMapping<?, ?> mapping) {
        mappingByQName.put(modelTypeQName, mapping);
        return this;
    }
}
