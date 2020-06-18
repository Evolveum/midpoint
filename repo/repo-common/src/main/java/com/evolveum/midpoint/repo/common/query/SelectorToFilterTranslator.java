/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.query;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Translates a selector (ObjectSelectorType) to appropriate ObjectFilter.
 *
 * See also SecurityEnforcerImpl#computeSecurityFilterPhase.
 */
@Experimental
public class SelectorToFilterTranslator {

    @NotNull private final ObjectSelectorType selector;
    @NotNull private final List<ObjectFilter> components = new ArrayList<>();
    @NotNull private final String contextDescription;

    @NotNull final PrismContext prismContext;
    @NotNull final QueryFactory queryFactory;
    @NotNull private final ExpressionFactory expressionFactory;

    @NotNull private final Task task;
    @NotNull private final OperationResult result;

    @NotNull private Class<? extends ObjectType> targetType;
    @NotNull private PrismObjectDefinition<?> targetDefinition;

    private boolean evaluated;

    public SelectorToFilterTranslator(@NotNull ObjectSelectorType selector, @NotNull Class<? extends ObjectType> targetType,
            @NotNull String contextDescription, @NotNull PrismContext prismContext, @NotNull ExpressionFactory expressionFactory,
            @NotNull Task task, @NotNull OperationResult result) {
        this.selector = selector;
        this.contextDescription = contextDescription;

        this.prismContext = prismContext;
        this.queryFactory = prismContext.queryFactory();
        this.expressionFactory = expressionFactory;

        this.task = task;
        this.result = result;

        this.targetType = targetType;
        this.targetDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(targetType);
        if (this.targetDefinition == null) {
            throw new IllegalArgumentException("No object definition for " + targetType);
        }
    }

    @NotNull
    public ObjectFilter createFilter() throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        checkAlreadyEvaluated();

        if (!applyType()) {
            return none();
        }
        applyFilter();
        applyArchetypes();
        applyOrg();
        checkNoSubtype();

        return queryFactory.createAndOptimized(components);
    }

    private void checkAlreadyEvaluated() {
        if (evaluated) {
            throw new IllegalStateException("A filter was already created for this selector: " +
                    selector + " in " + contextDescription);
        } else {
            evaluated = true;
        }
    }

    private boolean applyType() throws SchemaException {
        QName typeName = selector.getType();
        if (typeName != null) {
            QName qualifiedTypeName = prismContext.getSchemaRegistry().qualifyTypeName(typeName);
            PrismObjectDefinition<?> objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(qualifiedTypeName);
            if (objectDef == null) {
                throw new SchemaException("Unknown object type " + typeName + " in " + contextDescription);
            }
            Class<?> objectClass = objectDef.getCompileTimeClass();
            if (targetType.equals(objectClass)) {
                // nothing to do
            } else if (!targetType.isAssignableFrom(objectClass)) {
                return false;
            } else {
                components.add(queryFactory.createType(qualifiedTypeName, null));

                // Remember more specific object type and definition now
                //noinspection unchecked
                targetType = (Class<? extends ObjectType>) objectClass;
                targetDefinition = objectDef;
            }
        }
        return true;
    }

    private void applyFilter() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        SearchFilterType filterBean = selector.getFilter();
        if (filterBean != null) {
            ObjectFilter specFilter = parseAndEvaluateFilter(filterBean);
            if (specFilter != null) {
                ObjectQueryUtil.assertNotRaw(specFilter, "Filter in " + contextDescription + " has undefined items. Maybe a 'type' specification is missing?");
                ObjectQueryUtil.assertPropertyOnly(specFilter, "Filter in " + contextDescription + " object is not property-only filter");
                components.add(specFilter);
            }
        }
    }

    private ObjectFilter parseAndEvaluateFilter(SearchFilterType filterBean) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        ObjectFilter filter = prismContext.getQueryConverter().createObjectFilter(targetDefinition, filterBean);
        if (filter != null) {
            ExpressionVariables variables = new ExpressionVariables(); // TODO
            return ExpressionUtil.evaluateFilterExpressions(filter, variables, MiscSchemaUtil.getExpressionProfile(),
                    expressionFactory, prismContext, "expression in " + contextDescription, task, result);
        } else {
            return null;
        }
    }

    private void applyArchetypes() {
        List<ObjectReferenceType> archetypeRefs = selector.getArchetypeRef();
        if (!archetypeRefs.isEmpty()) {
            List<ObjectFilter> disjuncts = new ArrayList<>();
            for (ObjectReferenceType archetypeRef : archetypeRefs) {
                disjuncts.add(prismContext.queryFor(targetType)
                        .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeRef.getOid())
                        .buildFilter());
            }
            components.add(queryFactory.createOrOptimized(disjuncts));
        }
    }

    private void applyOrg() {
        ObjectReferenceType orgRef = selector.getOrgRef();
        if (orgRef != null) {
            if (orgRef.getOid() != null) {
                components.add(
                        prismContext.queryFor(ObjectType.class)
                                .isChildOf(orgRef.getOid())
                                .buildFilter()
                );
            } else {
                throw new UnsupportedOperationException("orgRef without OID is not supported in " + contextDescription);
            }
        }
    }

    private void checkNoSubtype() {
        if (selector.getSubtype() != null) {
            throw new UnsupportedOperationException("Subtype is not supported in " + contextDescription);
        }
    }

    private NoneFilter none() {
        return queryFactory.createNone();
    }

    public Class<? extends ObjectType> getNarrowedTargetType() {
        if (evaluated) {
            return targetType;
        } else {
            throw new IllegalStateException("Selector was not evaluated yet");
        }
    }
}
