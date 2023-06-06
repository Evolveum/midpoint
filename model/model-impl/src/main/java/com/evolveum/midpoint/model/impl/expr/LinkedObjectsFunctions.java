/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import static java.util.Collections.emptySet;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.common.LinkManager;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.query.LinkedSelectorToFilterTranslator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;

/**
 * Functions related to "linked objects" functionality.
 */
@Component
@Experimental
public class LinkedObjectsFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(LinkedObjectsFunctions.class);

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private MidpointFunctionsImpl midpointFunctions;
    @Autowired private LinkManager linkManager;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    <T extends AssignmentHolderType> T findLinkedSource(Class<T> type) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return MiscUtil.extractSingleton(findLinkedSources(type), () -> new IllegalStateException("More than one assignee found"));
    }

    <T extends AssignmentHolderType> T findLinkedSource(String linkType) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return MiscUtil.extractSingleton(findLinkedSources(linkType), () -> new IllegalStateException("More than one assignee found"));
    }

    <T extends AssignmentHolderType> List<T> findLinkedSources(Class<T> type) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ObjectReferenceType focusObjectReference = midpointFunctions.getFocusObjectReference();
        if (focusObjectReference == null) {
            return List.of(); // There cannot be any sources yet, because the focus is not in repository.
        }
        ObjectQuery query = prismContext.queryFor(type)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .ref(focusObjectReference.asReferenceValue())
                .build();
        return midpointFunctions.searchObjects(type, query, null);
    }

    <T extends AssignmentHolderType> List<T> findLinkedSources(String linkType) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {

        Task currentTask = midpointFunctions.getCurrentTask();
        OperationResult currentResult = midpointFunctions.getCurrentResult();
        LensFocusContext<?> focusContext = (LensFocusContext<?>) midpointFunctions.getFocusContext();
        if (focusContext == null) {
            throw new IllegalStateException("No focus context");
        }
        LinkTypeDefinitionType definition = focusContext.getSourceLinkTypeDefinition(linkType, linkManager, currentResult);
        if (definition == null) {
            throw new IllegalStateException("No definition for source link type " + linkType + " for " + focusContext);
        }

        ObjectReferenceType focusObjectReference = midpointFunctions.getFocusObjectReference();
        if (focusObjectReference == null) {
            return List.of();
        }
        PrismReferenceValue focusReference = focusObjectReference.asReferenceValue();
        LinkedSelectorToFilterTranslator translator = new LinkedSelectorToFilterTranslator(definition.getSelector(), focusReference,
                "finding linked sources for " + focusContext, prismContext, expressionFactory,
                currentTask, currentResult);
        ObjectQuery query = prismContext.queryFactory().createQuery(translator.createFilter());

        //noinspection unchecked
        return (List<T>) midpointFunctions.searchObjects(translator.getNarrowedTargetType(), query, null);
    }

    // Should be used after assignment evaluation!
    <T extends AssignmentHolderType> T findLinkedTarget(Class<T> type, String archetypeOid) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        return MiscUtil.extractSingleton(findLinkedTargets(type, archetypeOid),
                () -> new IllegalStateException("More than one assigned object found"));
    }

    // Should be used after assignment evaluation!
    <T extends AssignmentHolderType> T findLinkedTarget(String linkTypeName) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        return MiscUtil.extractSingleton(findLinkedTargets(linkTypeName),
                () -> new IllegalStateException("More than one assigned object found"));
    }

    // Should be used after assignment evaluation!
    @Experimental
    @NotNull
    <T extends AssignmentHolderType> List<T> findLinkedTargets(Class<T> type, String archetypeOid)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        Set<PrismReferenceValue> membership = getMembership();
        List<PrismReferenceValue> assignedWithMemberRelation = membership.stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()) && objectTypeMatches(ref, type))
                .collect(Collectors.toList());
        // TODO deduplicate w.r.t. member/manager
        // TODO optimize matching
        List<T> objects = new ArrayList<>(assignedWithMemberRelation.size());
        for (PrismReferenceValue reference : assignedWithMemberRelation) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue(reference);
            T object = midpointFunctions.resolveReferenceInternal(ort, true);
            if (objectMatches(object, type, archetypeOid)) {
                objects.add(object);
            }
        }
        return objects;
    }

    // Should be used after assignment evaluation!
    @Experimental
    @NotNull
    <T extends AssignmentHolderType> List<T> findLinkedTargets(String linkTypeName)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult currentResult = midpointFunctions.getCurrentResult();
        LensFocusContext<?> focusContext = (LensFocusContext<?>) midpointFunctions.getFocusContext();
        if (focusContext == null) {
            throw new IllegalStateException("No focus context");
        }
        LinkTypeDefinitionType definition = focusContext.getTargetLinkTypeDefinition(linkTypeName, linkManager, currentResult);
        if (definition == null) {
            throw new IllegalStateException("No definition for target link type " + linkTypeName + " for " + focusContext);
        }

        Class<?> expectedClasses = getExpectedClass(definition.getSelector());

        Set<PrismReferenceValue> membership = getMembership();
        List<PrismReferenceValue> assignedWithMatchingRelation = membership.stream()
                .filter(ref -> relationMatches(ref, definition.getSelector()) && objectTypeMatches(ref, expectedClasses))
                .collect(Collectors.toList());
        // TODO deduplicate w.r.t. member/manager
        // TODO optimize matching
        List<T> objects = new ArrayList<>(assignedWithMatchingRelation.size());
        for (PrismReferenceValue reference : assignedWithMatchingRelation) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue(reference);
            T object = midpointFunctions.resolveReferenceInternal(ort, true);
            if (objectMatches(object, definition.getSelector())) {
                objects.add(object);
            }
        }
        return objects;
    }

    private Class<?> getExpectedClass(LinkedObjectSelectorType selector) {
        if (selector == null || selector.getType() == null) {
            return null;
        } else {
            return prismContext.getSchemaRegistry().determineClassForTypeRequired(selector.getType());
        }
    }

    @NotNull
    private Set<PrismReferenceValue> getMembership() {
        if (Boolean.FALSE.equals(midpointFunctions.isEvaluateNew())) {
            return getMembershipOld();
        } else {
            return getMembershipNew();
        }
    }

    @NotNull
    private Set<PrismReferenceValue> getMembershipNew() {
        LensContext<?> lensContext = (LensContext<?>) ModelExpressionThreadLocalHolder.getLensContextRequired();
        Set<PrismReferenceValue> assigned = new HashSet<>();
        for (EvaluatedAssignmentImpl<?> evaluatedAssignment : lensContext.getNonNegativeEvaluatedAssignments()) {
            assigned.addAll(evaluatedAssignment.getMembershipRefVals());
        }
        return assigned;
    }

    @NotNull
    private Set<PrismReferenceValue> getMembershipOld() {
        LensContext<?> lensContext = (LensContext<?>) ModelExpressionThreadLocalHolder.getLensContextRequired();
        LensFocusContext<?> focusContext = lensContext.getFocusContextRequired();
        Objectable objectOld = asObjectable(focusContext.getObjectOld());
        if (objectOld instanceof AssignmentHolderType) {
            return new HashSet<>(objectReferenceListToPrismReferenceValues(((AssignmentHolderType) objectOld).getRoleMembershipRef()));
        } else {
            return emptySet();
        }
    }

    private boolean objectTypeMatches(PrismReferenceValue ref, Class<?> expectedClass) {
        if (expectedClass == null) {
            return true;
        } else {
            Class<?> refTargetClass = prismContext.getSchemaRegistry().determineClassForTypeRequired(
                    Objects.requireNonNull(
                            ref.getTargetType(), "no target type"));
            return expectedClass.isAssignableFrom(refTargetClass);
        }
    }

    private boolean relationMatches(PrismReferenceValue ref, LinkedObjectSelectorType selector) {
        return selector == null ||
                selector.getRelation().isEmpty() ||
                prismContext.relationMatches(selector.getRelation(), ref.getRelation());
    }

    private <T extends AssignmentHolderType> boolean objectMatches(
            AssignmentHolderType targetObject, Class<T> type, String archetypeOid) {
        return (type == null || type.isAssignableFrom(targetObject.getClass()))
                && (archetypeOid == null || hasArchetypeRef(targetObject, archetypeOid));
    }

    private boolean objectMatches(AssignmentHolderType targetObject, ObjectSelectorType selector)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return selector == null ||
                repositoryService.selectorMatches(
                        selector, targetObject.asPrismObject(), null, LOGGER, "");
    }

    @Experimental // todo clean up!
    public <T extends AssignmentHolderType> T createLinkedSource(String linkTypeName) throws SchemaException, ConfigurationException {
        OperationResult currentResult = midpointFunctions.getCurrentResult();
        LensFocusContext<?> focusContext = (LensFocusContext<?>) midpointFunctions.getFocusContext();
        if (focusContext == null) {
            throw new IllegalStateException("No focus context");
        }
        LinkTypeDefinitionType definition = focusContext.getSourceLinkTypeDefinition(linkTypeName, linkManager, currentResult);
        if (definition == null) {
            throw new IllegalStateException("No definition for source link type " + linkTypeName + " for " + focusContext);
        }
        LinkedObjectSelectorType selector = definition.getSelector();
        if (selector == null) {
            throw new IllegalStateException("Couldn't create new linked source object without a selector");
        }
        if (selector.getType() == null) {
            throw new IllegalStateException("Couldn't create new linked source object without explicit type in the selector");
        }
        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        Class<?> objectClass = schemaRegistry.getCompileTimeClassForObjectType(selector.getType());
        if (objectClass == null) {
            throw new IllegalStateException("No object class for type " + selector.getType());
        }
        if (Modifier.isAbstract(objectClass.getModifiers())) {
            throw new IllegalStateException("Class " + objectClass + " cannot be instantiated because it is abstract");
        }
        T newObject;
        try {
            //noinspection unchecked
            newObject = (T) objectClass.getConstructor(PrismContext.class).newInstance(prismContext);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new SystemException("Couldn't instantiate " + objectClass);
        }
        newObject.beginAssignment()
                .targetRef(focusContext.getOid(), schemaRegistry.determineTypeForClassRequired(focusContext.getObjectTypeClass()));
        ObjectReferenceType archetypeRef = MiscUtil.extractSingleton(selector.getArchetypeRef(),
                () -> new IllegalStateException("Couldn't instantiate object from selector with multiple archetypes"));
        if (archetypeRef != null) {
            newObject.beginAssignment()
                    .targetRef(archetypeRef.clone());
        }
        // TODO other things from the selector
        return newObject;
    }
}
