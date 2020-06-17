/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.ClassDefinitionParser;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityPointerDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
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
public final class QueryDefinitionRegistry implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(QueryDefinitionRegistry.class);
    private static final Map<QName, JpaEntityDefinition> DEFINITIONS;

    private static QueryDefinitionRegistry registry;

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
                        Class<?> superclass = entityDef.getJpaClass().getSuperclass();
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
                            Class<?> referencedEntityJpaClass = entPtrDef.getJpaClass();
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

            DEFINITIONS = Collections.unmodifiableMap(map);
        } catch (Throwable t) {
            LOGGER.error("Couldn't initialize query definition registry: {}", t.getMessage(), t);
            throw t;
        }
    }

    private QueryDefinitionRegistry() {
    }

    public static QueryDefinitionRegistry getInstance() {
        if (registry == null) {
            registry = new QueryDefinitionRegistry();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Registry:\n{}", registry.debugDump());
            }
        }

        return registry;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder builder = new StringBuilder();
        DebugUtil.indentDebugDump(builder, indent);
        Collection<JpaEntityDefinition> defCollection = DEFINITIONS.values();
        for (JpaEntityDefinition definition : defCollection) {
            builder.append(definition.debugDump(indent)).append('\n');
        }

        return builder.toString();
    }

    public JpaEntityDefinition findEntityDefinition(QName typeName) {
        Validate.notNull(typeName, "Type name must not be null.");

        JpaEntityDefinition def = QNameUtil.getKey(DEFINITIONS, typeName);
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
        for (JpaEntityDefinition definition : DEFINITIONS.values()) {
            if (parentClass.equals(definition.getJpaClass().getSuperclass())) {
                retval.add(definition);
            }
        }
        return retval;
    }
}
