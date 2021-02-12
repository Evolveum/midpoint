/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query;

import java.util.Objects;

import org.hibernate.Session;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query.resolution.ItemPathResolver;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class InterpretationContext {

    private final QueryInterpreter interpreter;
    private final PrismContext prismContext;
    private final RelationRegistry relationRegistry;
    private final Session session;
    private final ExtItemDictionary extItemDictionary;

    private final ItemPathResolver itemPathResolver = new ItemPathResolver(this);

    private final Class<? extends Containerable> type;

    private final RootHibernateQuery hibernateQuery;

    /**
     * Definition of the root entity. Root entity corresponds to the ObjectType class that was requested
     * by the search operation, or the one that was refined from abstract types (ObjectType, AbstractRoleType, ...)
     * in the process of restriction construction.
     */
    private final JpaEntityDefinition rootEntityDefinition;

    public InterpretationContext(QueryInterpreter interpreter, Class<? extends Containerable> type,
            PrismContext prismContext, RelationRegistry relationRegistry,
            ExtItemDictionary extItemDictionary, Session session) throws QueryException {

        Objects.requireNonNull(interpreter, "interpreter");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(prismContext, "prismContext");
        Objects.requireNonNull(relationRegistry, "relationRegistry");
        Objects.requireNonNull(extItemDictionary, "extItemDictionary");
        Objects.requireNonNull(session, "session");

        this.interpreter = interpreter;
        this.type = type;
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
        this.extItemDictionary = extItemDictionary;
        this.session = session;

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();

        // This is a preliminary information. It can change (be narrowed) during filter interpretation, e.g. from RObject to RUser.
        // Unfortunately, it's not that easy to postpone HibernateQuery creation, because individual filters may require it -
        // even before some ItemValueRestriction requests the narrowing.

        rootEntityDefinition = registry.findEntityDefinition(type);

        this.hibernateQuery = new RootHibernateQuery(rootEntityDefinition);
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public RelationRegistry getRelationRegistry() {
        return relationRegistry;
    }

    public Session getSession() {
        return session;
    }

    public QueryInterpreter getInterpreter() {
        return interpreter;
    }

    public Class<? extends Containerable> getType() {
        return type;
    }

    public RootHibernateQuery getHibernateQuery() {
        return hibernateQuery;
    }

    public ItemPathResolver getItemPathResolver() {
        return itemPathResolver;
    }

    public JpaEntityDefinition getRootEntityDefinition() {
        return rootEntityDefinition;
    }

    public boolean isObject() {
        return ObjectType.class.isAssignableFrom(type);
    }

    public String getPrimaryEntityAlias() {
        return hibernateQuery.getPrimaryEntityAlias();
    }

    public ExtItemDictionary getExtItemDictionary() {
        return extItemDictionary;
    }
}
