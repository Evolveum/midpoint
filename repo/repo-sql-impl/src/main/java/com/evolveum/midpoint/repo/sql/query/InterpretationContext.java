/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query;

import java.util.Objects;

import jakarta.persistence.EntityManager;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.resolution.ItemPathResolver;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class InterpretationContext {

    private final QueryInterpreter interpreter;
    private final PrismContext prismContext;
    private final RelationRegistry relationRegistry;
    private final EntityManager entityManager;
    private final ExtItemDictionary extItemDictionary;

    private final ItemPathResolver itemPathResolver = new ItemPathResolver(this);

    private final Class<? extends Containerable> type;

    private HibernateQuery hibernateQuery; // de-facto final

    /**
     * Definition of the root entity. Root entity corresponds to the ObjectType class that was requested
     * by the search operation, or the one that was refined from abstract types (ObjectType, AbstractRoleType, ...)
     * in the process of restriction construction.
     */
    private final JpaEntityDefinition rootEntityDefinition;

    public InterpretationContext(QueryInterpreter interpreter, Class<? extends Containerable> type,
            PrismContext prismContext, RelationRegistry relationRegistry,
            ExtItemDictionary extItemDictionary, EntityManager entityManager, SupportedDatabase databaseType)
            throws QueryException {
        this(interpreter, type, prismContext, relationRegistry, extItemDictionary, entityManager);

        this.hibernateQuery = new HibernateQuery(rootEntityDefinition, databaseType);
    }

    public InterpretationContext(QueryInterpreter interpreter, Class<? extends Containerable> type,
            PrismContext prismContext, RelationRegistry relationRegistry,
            ExtItemDictionary extItemDictionary, EntityManager em, HibernateQuery parentQuery)
            throws QueryException {
        this(interpreter, type, prismContext, relationRegistry, extItemDictionary, em);

        this.hibernateQuery = parentQuery.createSubquery(rootEntityDefinition);
    }

    private InterpretationContext(QueryInterpreter interpreter, Class<? extends Containerable> type,
            PrismContext prismContext, RelationRegistry relationRegistry,
            ExtItemDictionary extItemDictionary, EntityManager entityManager)
            throws QueryException {

        Objects.requireNonNull(interpreter, "interpreter");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(prismContext, "prismContext");
        Objects.requireNonNull(relationRegistry, "relationRegistry");
        Objects.requireNonNull(extItemDictionary, "extItemDictionary");
        Objects.requireNonNull(entityManager, "session");

        this.interpreter = interpreter;
        this.type = type;
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
        this.extItemDictionary = extItemDictionary;
        this.entityManager = entityManager;

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();

        // This is preliminary information. It can change (be narrowed) during filter interpretation, e.g. from RObject to RUser.
        // Unfortunately, it's not that easy to postpone HibernateQuery creation, because individual filters may require it -
        // even before some ItemValueRestriction requests the narrowing.

        rootEntityDefinition = registry.findEntityDefinition(type);
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public RelationRegistry getRelationRegistry() {
        return relationRegistry;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public QueryInterpreter getInterpreter() {
        return interpreter;
    }

    public Class<? extends Containerable> getType() {
        return type;
    }

    public HibernateQuery getHibernateQuery() {
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

    public InterpretationContext createSubcontext(Class<? extends Containerable> type) throws QueryException {
        return new InterpretationContext(interpreter, type, prismContext,
                relationRegistry, extItemDictionary, entityManager, hibernateQuery);
    }
}
