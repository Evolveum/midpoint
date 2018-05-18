/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.resolution.ItemPathResolver;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;

/**
 * @author lazyman
 * @author mederly
 */
public class InterpretationContext {

    private static final Trace LOGGER = TraceManager.getTrace(InterpretationContext.class);

    private QueryInterpreter2 interpreter;
    private PrismContext prismContext;
    private Session session;
    private ExtItemDictionary extItemDictionary;

    private ItemPathResolver itemPathResolver = new ItemPathResolver(this);

    private Class<? extends Containerable> type;

    private RootHibernateQuery hibernateQuery;

    /**
     * Definition of the root entity. Root entity corresponds to the ObjectType class that was requested
     * by the search operation, or the one that was refined from abstract types (ObjectType, AbstractRoleType, ...)
     * in the process of restriction construction.
     */
    private JpaEntityDefinition rootEntityDefinition;

    public InterpretationContext(QueryInterpreter2 interpreter, Class<? extends Containerable> type,
                                 PrismContext prismContext, ExtItemDictionary extItemDictionary, Session session) throws QueryException {

        Validate.notNull(interpreter, "interpreter");
        Validate.notNull(type, "type");
        Validate.notNull(prismContext, "prismContext");
        Validate.notNull(extItemDictionary, "extItemDictionary");
        Validate.notNull(session, "session");

        this.interpreter = interpreter;
        this.type = type;
        this.prismContext = prismContext;
        this.extItemDictionary = extItemDictionary;
        this.session = session;

        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();

        // This is a preliminary information. It can change (be narrowed) during filter interpretation, e.g. from RObject to RUser.
        // Unfortunately, it's not that easy to postpone HibernateQuery creation, because individual filters may require it -
        // even before some ItemValueRestriction requests the narrowing.

        rootEntityDefinition = registry.findEntityDefinition(type);

        this.hibernateQuery = new RootHibernateQuery(rootEntityDefinition);
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public Session getSession() {
        return session;
    }

    public QueryInterpreter2 getInterpreter() {
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
