/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.ClassDefinitionParser;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.query.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class QueryDefinitionRegistry implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(QueryDefinitionRegistry.class);
    private static final Map<QName, EntityDefinition> definitions;

    private static QueryDefinitionRegistry registry;

    static {
        LOGGER.trace("Initializing query definition registry.");
        ClassDefinitionParser classDefinitionParser = new ClassDefinitionParser();

        Map<QName, EntityDefinition> map = new HashMap<QName, EntityDefinition>();
        Collection<RObjectType> types = ClassMapper.getKnownTypes();
        for (RObjectType type : types) {
            Class clazz = type.getClazz();
            if (!RObject.class.isAssignableFrom(clazz)) {
                continue;
            }

            Definition definition = classDefinitionParser.parseObjectTypeClass(clazz);
            if (definition == null) {
                continue;
            }

            ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
            map.put(objectType.getQName(), (EntityDefinition) definition);
        }

        definitions = Collections.unmodifiableMap(map);
    }

    private QueryDefinitionRegistry() {
    }

    public static QueryDefinitionRegistry getInstance() {
        if (registry == null) {
            registry = new QueryDefinitionRegistry();
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

    public <T extends ObjectType, D extends Definition> D findDefinition(Class<T> type, ItemPath path, Class<D> definitionType) {
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(definitionType, "Definition type must not be null.");

        EntityDefinition entityDef = definitions.get(ObjectTypes.getObjectType(type).getQName());
        return entityDef.findDefinition(path, definitionType);
    }
}
