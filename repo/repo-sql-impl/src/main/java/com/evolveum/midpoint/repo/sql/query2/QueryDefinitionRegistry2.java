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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.ClassDefinitionParser;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryDefinitionRegistry2 implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(QueryDefinitionRegistry2.class);
    private static final Map<QName, EntityDefinition> definitions;

    private static QueryDefinitionRegistry2 registry;

    static {
        LOGGER.trace("Initializing query definition registry.");
        ClassDefinitionParser classDefinitionParser = new ClassDefinitionParser();

        Map<QName, EntityDefinition> map = new HashMap<QName, EntityDefinition>();
        Map<Class<? extends RObject>, EntityDefinition> definitionsByClass = new HashMap<>();

        Collection<RObjectType> types = ClassMapper.getKnownTypes();
        for (RObjectType type : types) {
            Class clazz = type.getClazz();
            if (!RObject.class.isAssignableFrom(clazz)) {
                continue;
            }

            EntityDefinition definition = classDefinitionParser.parseObjectTypeClass(clazz);
            if (definition == null) {
                continue;
            }

            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
            map.put(objectType.getTypeQName(), definition);
            definitionsByClass.put(definition.getJpaType(), definition);
        }

        // TODO fix this hack
        EntityDefinition caseDefinition = classDefinitionParser.parseObjectTypeClass(AccessCertificationCaseType.COMPLEX_TYPE,
                AccessCertificationCaseType.class, RAccessCertificationCase.class);
        map.put(AccessCertificationCaseType.COMPLEX_TYPE, caseDefinition);

        // link parents (maybe not needed at all, we'll see)
        for (EntityDefinition definition : map.values()) {
            Class superclass = definition.getJpaType().getSuperclass();
            if (!RObject.class.isAssignableFrom(superclass)) {
                continue;
            }
            EntityDefinition superclassDefinition = definitionsByClass.get(superclass);
            if (superclassDefinition == null) {
                throw new IllegalStateException("No definition for superclass " + superclass + " of " + definition);
            }
            definition.setSuperclassDefinition(superclassDefinition);
        }

        definitions = Collections.unmodifiableMap(map);
    }

    private QueryDefinitionRegistry2() {
    }

    public static QueryDefinitionRegistry2 getInstance() {
        if (registry == null) {
            registry = new QueryDefinitionRegistry2();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Registry:\n{}", registry.debugDump());
            }
        }

        return registry;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder builder = new StringBuilder();
        DebugUtil.indentDebugDump(builder, indent);
        Collection<EntityDefinition> defCollection = definitions.values();
        for (Definition definition : defCollection) {
            builder.append(definition.debugDump()).append('\n');
        }

        return builder.toString();
    }

    public EntityDefinition findEntityDefinition(QName typeName) {
        Validate.notNull(typeName, "Type name must not be null.");

        EntityDefinition def = definitions.get(typeName);
        if (def == null) {
            throw new IllegalStateException("Type " + typeName + " couldn't be found in type registry");
        }
        return def;
    }

    // always returns non-null value
    public <T extends Containerable> EntityDefinition findEntityDefinition(Class<T> type) throws QueryException {
        Validate.notNull(type, "Type must not be null.");
        return findEntityDefinition(getQNameForType(type));
    }

    public <T extends Containerable> QName getQNameForType(Class<T> type) throws QueryException {
        if (ObjectType.class.isAssignableFrom(type)) {
            return ObjectTypes.getObjectType((Class) type).getTypeQName();
        }
        if (AccessCertificationCaseType.class.equals(type)) {           // TODO generalize
            return AccessCertificationCaseType.COMPLEX_TYPE;
        }
        throw new QueryException("Unsupported type " + type);
    }

    public <T extends Containerable, D extends Definition> DefinitionSearchResult<D> findDefinition(Class<T> type, ItemPath path, Class<D> definitionType) throws QueryException {
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(definitionType, "Definition type must not be null.");
        EntityDefinition entityDef = findEntityDefinition(type);
        return entityDef.findDefinition(path, definitionType);
    }

    /**
     * Returns possible "children" of a given definition.
     * More abstract classes are listed first.
     */
    public List<EntityDefinition> getChildrenOf(EntityDefinition entityDefinition) {
        List<EntityDefinition> retval = new ArrayList<>();
        List<EntityDefinition> children = getDirectChildrenOf(entityDefinition);
        for (EntityDefinition child : children) {
            retval.add(child);
            retval.addAll(getChildrenOf(child));
        }
        return retval;
    }

    private List<EntityDefinition> getDirectChildrenOf(EntityDefinition parentDefinition) {
        Class parentClass = parentDefinition.getJpaType();
        List<EntityDefinition> retval = new ArrayList<>();
        for (EntityDefinition definition : definitions.values()) {
            if (parentClass.equals(definition.getJpaType().getSuperclass())) {
                retval.add(definition);
            }
        }
        return retval;
    }
}
