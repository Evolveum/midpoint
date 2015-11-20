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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.restriction.TypeRestriction;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;

import javax.xml.namespace.QName;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyman
 */
public class InterpretationContext {

    private QueryInterpreter2 interpreter;
    private PrismContext prismContext;
    private Session session;

    private Class<? extends ObjectType> type;

    private HibernateQuery hibernateQuery;

    // path from the root filter to the current one (necessary when finding correct Restriction)
    private List<ObjectFilter> currentFilterPath = new ArrayList<>();

    public InterpretationContext(QueryInterpreter2 interpreter, Class<? extends ObjectType> type,
                                 PrismContext prismContext, Session session) {
        this.interpreter = interpreter;
        this.type = type;
        this.prismContext = prismContext;
        this.session = session;

        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();

        EntityDefinition primaryEntityDef = registry.findDefinition(type, null, EntityDefinition.class);
        this.hibernateQuery = new HibernateQuery(primaryEntityDef);
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

    public HibernateQuery getHibernateQuery() {
        return hibernateQuery;
    }

    public static class ProperDefinitionSearchResult<T extends Definition> {
        EntityDefinition rootEntityDefinition;
        T itemDefinition;

        public ProperDefinitionSearchResult(EntityDefinition rootEntityDefinition, T itemDefinition) {
            this.rootEntityDefinition = rootEntityDefinition;
            this.itemDefinition = itemDefinition;
        }

        public EntityDefinition getRootEntityDefinition() {
            return rootEntityDefinition;
        }

        public T getItemDefinition() {
            return itemDefinition;
        }
    }

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
            EntityDefinition entityDefinition = registry.findDefinition(getType(), null, EntityDefinition.class);
            T pathDefinition = entityDefinition.findDefinition(path, clazz);
            if (pathDefinition != null) {
                return new ProperDefinitionSearchResult<>(entityDefinition, pathDefinition);
            }
        }
        return null;
    }

    protected EntityDefinition findProperEntityDefinition(ItemPath path) {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        if (!ObjectType.class.equals(getType())) {
            return registry.findDefinition(getType(), null, EntityDefinition.class);
        }

        EntityDefinition entity = null;
        // we should try to find property in descendant classes
        for (Class type : findOtherPossibleParents()) {
            entity = registry.findDefinition(type, null, EntityDefinition.class);
            Definition def = entity.findDefinition(path, Definition.class);
            if (def != null) {
                break;
            }
        }
        LOGGER.trace("Found proper entity definition for path {}, {}", path, entity.toString());
        return entity;
    }

    private Set<Class<? extends ObjectType>> findOtherPossibleParents() {
        TypeRestriction typeRestriction = findTypeRestrictionParent(this);
        ObjectTypes typeClass;
        if (typeRestriction != null) {
            TypeFilter filter = typeRestriction.getFilter();
            typeClass = ObjectTypes.getObjectTypeFromTypeQName(filter.getType());
        } else {
            typeClass = ObjectTypes.getObjectType(getType());
        }

        Set<Class<? extends ObjectType>> classes = new HashSet<>();
        classes.add(typeClass.getClassDefinition());

        switch (typeClass) {
            case OBJECT:
                classes.addAll(ObjectTypes.getAllObjectTypes());
                break;
            case FOCUS_TYPE:
                classes.add(UserType.class);
            case ABSTRACT_ROLE:
                classes.add(RoleType.class);
                classes.add(OrgType.class);
        }

        LOGGER.trace("Found possible parents {} for entity definitions.", Arrays.toString(classes.toArray()));
        return classes;
    }

    public void pushFilter(ObjectFilter filter) {
        currentFilterPath.add(filter);
    }

    public void popFilter() {
        currentFilterPath.remove(currentFilterPath.size() - 1);
    }

    public String getCurrentHqlPropertyPath() {
        // TODO
    }

    private void getCurrentItemPath() {

    }
}
