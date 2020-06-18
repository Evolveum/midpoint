/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Wraps everything we need to count or look for objects.
 */
@Experimental
public class CompleteQuery<T extends ObjectType> implements DebugDumpable {

    @NotNull private final Class<T> type;
    private final ObjectQuery query;
    private final Collection<SelectorOptions<GetOperationOptions>> options;

    public CompleteQuery(@NotNull Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
        this.type = type;
        this.query = query;
        this.options = options;
    }

    /**
     * Composes complete queries into single "OR" query. Requires compatible paging and options.
     * (Currently no paging nor options is allowed.)
     */
    @NotNull
    public static CompleteQuery<?> or(List<CompleteQuery<?>> completeQueries, PrismContext prismContext) {
        QueryFactory queryFactory = prismContext.queryFactory();

        Class<? extends ObjectType> commonType = getCommonAncestor(completeQueries);
        List<ObjectFilter> disjuncts = new ArrayList<>();
        for (CompleteQuery<?> completeQuery : completeQueries) {
            if (CollectionUtils.isNotEmpty(completeQuery.options)) {
                throw new UnsupportedOperationException("Query options are not supported here: " + completeQuery.options);
            }
            ObjectQuery query = completeQuery.query;
            if (query != null && query.getPaging() != null) {
                throw new UnsupportedOperationException("Query paging is not supported here: " + query.getPaging());
            }

            ObjectFilter filter = query != null ? query.getFilter() : null;
            Class<?> type = completeQuery.type;
            if (type.equals(commonType)) {
                if (filter != null) {
                    disjuncts.add(filter);
                }
            } else {
                QName typeName = prismContext.getSchemaRegistry().determineTypeForClassRequired(type);
                disjuncts.add(queryFactory.createType(typeName, filter));
            }
        }
        ObjectQuery orQuery = queryFactory.createQuery(queryFactory.createOrOptimized(disjuncts));
        return new CompleteQuery<>(commonType, orQuery, null);
    }

    @NotNull
    private static Class<? extends ObjectType> getCommonAncestor(List<CompleteQuery<?>> completeQueries) {
        if (completeQueries.isEmpty()) {
            return ObjectType.class;
        } else {
            Set<Class<?>> types = completeQueries.stream()
                    .map(CompleteQuery::getType)
                    .collect(Collectors.toSet());
            Class<?> commonAncestor = MiscUtil.determineCommonAncestor(types);
            if (commonAncestor == null || !ObjectType.class.isAssignableFrom(commonAncestor)) {
                throw new IllegalStateException("Common ancestor for complete queries is unknown or not an ObjectType: " +
                        commonAncestor + " for " + completeQueries);
            } else {
                //noinspection unchecked
                return (Class<? extends ObjectType>) commonAncestor;
            }
        }
    }

    public static CompleteQuery<?> inOid(Collection<PrismReferenceValue> references, PrismContext prismContext) {
        Class<? extends ObjectType> commonAncestor = getCommonAncestorForReferences(references, prismContext);
        String[] oids = references.stream().map(PrismReferenceValue::getOid).distinct().toArray(String[]::new);
        ObjectQuery query = prismContext.queryFor(commonAncestor)
                .id(oids)
                .build();
        return new CompleteQuery<>(commonAncestor, query, null);
    }

    @NotNull
    private static Class<? extends ObjectType> getCommonAncestorForReferences(Collection<PrismReferenceValue> references,
            PrismContext prismContext) {
        Class<? extends ObjectType> commonAncestor1;
        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        Set<QName> typeNames = references.stream()
                .map(PrismReferenceValue::getTargetType)
                .collect(Collectors.toSet());
        Set<Class<?>> types = typeNames.stream()
                .map(schemaRegistry::determineClassForTypeRequired)
                .collect(Collectors.toSet());
        Class<?> commonAncestor = MiscUtil.determineCommonAncestor(types);
        if (commonAncestor == null || !ObjectType.class.isAssignableFrom(commonAncestor)) {
            commonAncestor1 = ObjectType.class; // Let's be more relaxed here
        } else {
            //noinspection unchecked
            commonAncestor1 = (Class<? extends ObjectType>) commonAncestor;
        }
        return commonAncestor1;
    }

    public static <T extends ObjectType> CompleteQuery<T> none(Class<T> type, PrismContext prismContext) {
        return new CompleteQuery<>(type,
                prismContext.queryFactory().createQuery(prismContext.queryFactory().createNone()),
                null);
    }

    public @NotNull Class<T> getType() {
        return type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Object type", type, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Query", query, indent);
        DebugUtil.debugDumpWithLabel(sb, "Options", options, indent);
        return sb.toString();
    }
}
