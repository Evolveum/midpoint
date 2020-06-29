package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.sql.JDBCType;

import com.querydsl.core.types.Path;

/**
 * Mapper between model attribute type and query attribute type (subclass of {@link Path}).
 */
public class QueryModelAttributeMapper<M, Q> {

    private final Class<M> modelType;
    private final Class<Q> queryType;
    private final JDBCType jdbcType;

    public QueryModelAttributeMapper(Class<M> modelType, Class<Q> queryType, JDBCType jdbcType) {
        this.modelType = modelType;
        this.queryType = queryType;
        this.jdbcType = jdbcType;
    }
}
