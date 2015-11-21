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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.restriction.TypeRestriction;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 */
public class InterpretationContext {

    private static final Trace LOGGER = TraceManager.getTrace(InterpretationContext.class);

    private QueryInterpreter2 interpreter;
    private PrismContext prismContext;
    private Session session;

    private Class<? extends ObjectType> type;

    private RootHibernateQuery hibernateQuery;

    // path from the root filter to the current one (necessary when finding correct Restriction)
    private List<ObjectFilter> currentFilterPath = new ArrayList<>();

    public InterpretationContext(QueryInterpreter2 interpreter, Class<? extends ObjectType> type,
                                 PrismContext prismContext, Session session) throws QueryException {

        Validate.notNull(interpreter, "interpreter");
        Validate.notNull(type, "type");
        Validate.notNull(prismContext, "prismContext");
        Validate.notNull(session, "session");

        this.interpreter = interpreter;
        this.type = type;
        this.prismContext = prismContext;
        this.session = session;

        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();

        // This is a preliminary information. It can change (be narrowed) during filter interpretation, e.g. from RObject to RUser.
        // Unfortunately, it's not that easy to postpone HibernateQuery creation, because individual filters may require it -
        // even before some ItemRestriction requests the narrowing.

        EntityDefinition primaryEntityDef = registry.findDefinition(type, null, EntityDefinition.class);
        if (primaryEntityDef == null) {
            throw new QueryException("Primary entity definition couldn't be found for type " + type);
        }

        this.hibernateQuery = new RootHibernateQuery(primaryEntityDef);
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

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public RootHibernateQuery getHibernateQuery() {
        return hibernateQuery;
    }

    /**
     * Finds the proper definition for (possibly abstract) type.
     * If the type is abstract, tries types which could be used in the query (types that have the item definition).
     *
     * It should try more abstract types first, in order to allow for later narrowing. (This would not work in case
     * of double narrowing to an item that has the same name in two different subclasses - but currently we don't know
     * of such.)
     *
     * @param path Path to be found
     * @param clazz Kind of definition to be looked for
     * @return Entity type definition + item definition
     */
    public <T extends Definition> ProperDefinitionSearchResult<T> findProperDefinition(ItemPath path, Class<T> clazz) {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        Class<? extends ObjectType> objectClass = getType();
        if (!Modifier.isAbstract(objectClass.getModifiers())) {
            EntityDefinition entityDefinition = registry.findDefinition(getType(), null, EntityDefinition.class);
            T pathDefinition = entityDefinition.findDefinition(path, clazz);
            return new ProperDefinitionSearchResult<>(entityDefinition, pathDefinition);
        }

        //we should try to find property in descendant classes
        for (Class type : findOtherPossibleParents()) {
            EntityDefinition entityDefinition = registry.findDefinition(type, null, EntityDefinition.class);
            T pathDefinition = entityDefinition.findDefinition(path, clazz);
            if (pathDefinition != null) {
                return new ProperDefinitionSearchResult<>(entityDefinition, pathDefinition);
            }
        }
        return null;
    }

    /**
     * Similar to the above, but returns only the entity name.
     * @param path
     * @return
     */
    protected EntityDefinition findProperEntityDefinition(ItemPath path) {
        ProperDefinitionSearchResult<Definition> result = findProperDefinition(path, Definition.class);
        if (result != null) {
            return result.getRootEntityDefinition();
        } else {
            return null;
        }
    }

    private List<Class<? extends ObjectType>> findOtherPossibleParents() {
        TypeFilter typeFilter = findTypeFilterParent();
        ObjectTypes typeClass;
        if (typeFilter != null) {
            typeClass = ObjectTypes.getObjectTypeFromTypeQName(typeFilter.getType());
        } else {
            typeClass = ObjectTypes.getObjectType(getType());
        }

        List<Class<? extends ObjectType>> classes = new ArrayList<>();
        classes.add(typeClass.getClassDefinition());        // abstract one has to go first

        if (typeClass == ObjectTypes.OBJECT) {
            classes.addAll(ObjectTypes.getAllObjectTypes());
        } else if (typeClass == ObjectTypes.FOCUS_TYPE) {
            classes.add(UserType.class);
            classes.add(AbstractRoleType.class);
            classes.add(RoleType.class);
            classes.add(OrgType.class);
        } else if (typeClass == ObjectTypes.ABSTRACT_ROLE) {
            classes.add(RoleType.class);
            classes.add(OrgType.class);
        }

        LOGGER.trace("Found possible parents {} for entity definitions.", classes);
        return classes;
    }

    private TypeFilter findTypeFilterParent() {
        for (int i = currentFilterPath.size()-1; i >= 0; i--) {
            ObjectFilter filter = currentFilterPath.get(i);
            if (filter instanceof TypeFilter) {
                return (TypeFilter) filter;
            }
        }
        return null;
    }

    public void pushFilter(ObjectFilter filter) {
        currentFilterPath.add(filter);
    }

    public void popFilter() {
        currentFilterPath.remove(currentFilterPath.size() - 1);
    }

    public String getCurrentHqlPropertyPath() {
        // preliminary implementation: returns root alias (should be changed when ForValue is implemented)
        return hibernateQuery.getPrimaryEntityAlias();
    }

}
