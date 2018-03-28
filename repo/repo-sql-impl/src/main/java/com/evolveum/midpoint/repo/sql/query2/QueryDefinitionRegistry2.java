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
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.ClassDefinitionParser;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityPointerDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
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
    private static final Map<QName, JpaEntityDefinition> definitions;

    private static QueryDefinitionRegistry2 registry;

    static {
    	try {
			LOGGER.trace("Initializing query definition registry.");
			ClassDefinitionParser classDefinitionParser = new ClassDefinitionParser();

			final Map<QName, JpaEntityDefinition> map = new HashMap<>();
			final Map<Class<?>, JpaEntityDefinition> definitionsByClass = new HashMap<>();

			Collection<RObjectType> types = ClassMapper.getKnownTypes();
			for (RObjectType type : types) {
				Class clazz = type.getClazz();
				if (!RObject.class.isAssignableFrom(clazz)) {
					continue;
				}

				JpaEntityDefinition definition = classDefinitionParser.parseRootClass(clazz);
				if (definition == null) {
					continue;
				}

				ObjectTypes objectType = ClassMapper.getObjectTypeForHQLType(type);
				map.put(objectType.getTypeQName(), definition);
				definitionsByClass.put(definition.getJpaClass(), definition);
			}

			// TODO fix this hack
			JpaEntityDefinition caseDefinition = classDefinitionParser.parseRootClass(RAccessCertificationCase.class);
			definitionsByClass.put(RAccessCertificationCase.class, caseDefinition);
			map.put(AccessCertificationCaseType.COMPLEX_TYPE, caseDefinition);
			JpaEntityDefinition certWorkItemDefinition = classDefinitionParser.parseRootClass(RAccessCertificationWorkItem.class);
			definitionsByClass.put(RAccessCertificationWorkItem.class, certWorkItemDefinition);
			map.put(AccessCertificationWorkItemType.COMPLEX_TYPE, certWorkItemDefinition);
			JpaEntityDefinition caseWorkItemDefinition = classDefinitionParser.parseRootClass(RCaseWorkItem.class);
			definitionsByClass.put(RCaseWorkItem.class, caseWorkItemDefinition);
			map.put(CaseWorkItemType.COMPLEX_TYPE, caseWorkItemDefinition);

			// link parents (maybe not needed at all, we'll see) and referenced entity definitions
			// sort definitions
			for (final JpaEntityDefinition definition : map.values()) {
				Visitor resolutionVisitor = visitable -> {
					if (visitable instanceof JpaEntityDefinition) {
						JpaEntityDefinition entityDef = ((JpaEntityDefinition) visitable);
						Class superclass = entityDef.getJpaClass().getSuperclass();
						if (superclass == null || !RObject.class.isAssignableFrom(superclass)) {
							return;
						}
						JpaEntityDefinition superclassDefinition = definitionsByClass.get(superclass);
						if (superclassDefinition == null) {
							throw new IllegalStateException("No definition for superclass " + superclass + " of " + entityDef);
						}
						entityDef.setSuperclassDefinition(superclassDefinition);
					} else if (visitable instanceof JpaEntityPointerDefinition) {
						JpaEntityPointerDefinition entPtrDef = ((JpaEntityPointerDefinition) visitable);
						if (!entPtrDef.isResolved()) {
							Class referencedEntityJpaClass = entPtrDef.getJpaClass();
							JpaEntityDefinition realEntDef = definitionsByClass.get(referencedEntityJpaClass);
							if (realEntDef == null) {
								throw new IllegalStateException("Couldn't find entity definition for " + referencedEntityJpaClass);
							}
							entPtrDef.setResolvedEntityDefinition(realEntDef);
						}
					}
				};
				definition.accept(resolutionVisitor);

				Visitor sortingVisitor = visitable -> {
					if (visitable instanceof JpaEntityDefinition) {
						JpaEntityDefinition entityDef = ((JpaEntityDefinition) visitable);
						entityDef.sortDefinitions();
					}
				};
				definition.accept(sortingVisitor);
			}

			definitions = Collections.unmodifiableMap(map);
		} catch (Throwable t) {
    		LOGGER.error("Couldn't initialize query definition registry: {}", t.getMessage(), t);
    		throw t;
		}
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
        Collection<JpaEntityDefinition> defCollection = definitions.values();
        for (JpaEntityDefinition definition : defCollection) {
            builder.append(definition.debugDump(indent)).append('\n');
        }

        return builder.toString();
    }

    public JpaEntityDefinition findEntityDefinition(QName typeName) {
        Validate.notNull(typeName, "Type name must not be null.");

        JpaEntityDefinition def = QNameUtil.getKey(definitions, typeName);
        if (def == null) {
            throw new IllegalStateException("Type " + typeName + " couldn't be found in type registry");
        }
        return def;
    }

    // always returns non-null value
    public <T extends Containerable> JpaEntityDefinition findEntityDefinition(Class<T> type) throws QueryException {
        Validate.notNull(type, "Type must not be null.");
        return findEntityDefinition(getQNameForType(type));
    }

    public <T extends Containerable> QName getQNameForType(Class<T> type) throws QueryException {
        if (ObjectType.class.isAssignableFrom(type)) {
            return ObjectTypes.getObjectType((Class) type).getTypeQName();
        }
        if (AccessCertificationCaseType.class.equals(type)) {           // TODO generalize
            return AccessCertificationCaseType.COMPLEX_TYPE;
        } else if (AccessCertificationWorkItemType.class.equals(type)) {           // TODO generalize
			return AccessCertificationWorkItemType.COMPLEX_TYPE;
        } else if (CaseWorkItemType.class.equals(type)) {           // TODO generalize
			return CaseWorkItemType.COMPLEX_TYPE;
		}
        throw new QueryException("Unsupported type " + type);
    }

    /**
     * Returns possible "children" of a given definition.
     * More abstract classes are listed first.
     */
    public List<JpaEntityDefinition> getChildrenOf(JpaEntityDefinition entityDefinition) {
        List<JpaEntityDefinition> retval = new ArrayList<>();
        List<JpaEntityDefinition> children = getDirectChildrenOf(entityDefinition);
        for (JpaEntityDefinition child : children) {
            retval.add(child);
            retval.addAll(getChildrenOf(child));
        }
        return retval;
    }

    private List<JpaEntityDefinition> getDirectChildrenOf(JpaEntityDefinition parentDefinition) {
        Class parentClass = parentDefinition.getJpaClass();
        List<JpaEntityDefinition> retval = new ArrayList<>();
        for (JpaEntityDefinition definition : definitions.values()) {
            if (parentClass.equals(definition.getJpaClass().getSuperclass())) {
                retval.add(definition);
            }
        }
        return retval;
    }
}
