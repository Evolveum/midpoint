/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.value.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.NullableType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.NullabilityProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

/**
 * Extensions for CEL compiler and runtime implementing functions executed on MidPoint objects.
 * E.g. shadow.primaryIdentifier(), resource.configurationProperty()
 *
 * @author Radovan Semancik
 */
public class CelObjectExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelObjectExtensions.class);

    private final MidpointFunctions midpointExpressionFunctions;

    public CelObjectExtensions(MidpointFunctions midpointExpressionFunctions) {
        this.midpointExpressionFunctions = midpointExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

            // This is supposed to handle prismobject[qname] as well, as cel-java seem to handle both
            // ContainerValueCelValue.CEL_TYPE and ObjectCelValue.CEL_TYPE as DYN.
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "index_map",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-container-index_map-qname",
                                    "Resolves a structure using a QName",
                                    SimpleType.ANY,
                                    ContainerValueCelValue.CEL_TYPE,
                                    QNameCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("prism-container-index_map-qname",
                            ContainerValueCelValue.class, QNameCelValue.class,
                            CelObjectExtensions::prismIndexMap)),

                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "isEffectivelyEnabled",
                                CelOverloadDecl.newMemberOverload(
                                        "prism-object-isEffectivelyEnabled",
                                        "Returns true if the object is effectively enabled.",
                                        SimpleType.BOOL,
                                        ObjectCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("prism-object-isEffectivelyEnabled",
                                ObjectCelValue.class,
                                CelObjectExtensions::isEffectivelyEnabled,
                                NullabilityProperties.NULLABLE_FALSE)),

            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "findItem",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-object-finditem-string",
                                    "Returns an item to which the specified item path refers.",
                                    SimpleType.ANY,
                                    ObjectCelValue.CEL_TYPE,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("prism-object-finditem-string",
                            ObjectCelValue.class, String.class,
                            CelObjectExtensions::prismFind,
                            NullabilityProperties.NULLABLE_NULL)),

            // object.type()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "type",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-object-type",
                                    "Returns object type in QName form.",
                                    QNameCelValue.CEL_TYPE,
                                    ObjectCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("prism-object-type",
                            ObjectCelValue.class,
                            CelObjectExtensions::objectType,
                            NullabilityProperties.NULLABLE_NULL)),

            // resource.connectorConfiguration(propertyName)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "connectorConfiguration",
                            CelOverloadDecl.newMemberOverload(
                                    "mp-resource-connectorConfiguration",
                                    "Returns list of values of a resource connector configuration property specified by the argument.",
                                    ListType.create(SimpleType.DYN),
                                    ObjectCelValue.CEL_TYPE,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-resource-connectorConfiguration",
                            Object.class, Object.class,
                            this::connectorConfiguration,
                            NullabilityProperties.NULLABLE_EMPTY_LIST)),

            // shadow.primaryIdentifiers()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "primaryIdentifiers",
                            CelOverloadDecl.newMemberOverload(
                                    "mp-shadow-primaryIdentifiers",
                                    "Returns list of values of shadow primary identifier.",
                                    ListType.create(SimpleType.DYN),
                                    ObjectCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("mp-shadow-primaryIdentifiers",
                            Object.class,
                            this::primaryIdentifiers,
                            NullabilityProperties.NULLABLE_EMPTY_LIST)),

            // shadow.secondaryIdentifiers()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "secondaryIdentifiers",
                            CelOverloadDecl.newMemberOverload(
                                    "mp-shadow-secondaryIdentifiers",
                                    "Returns list of values of shadow secondary identifier.",
                                    ListType.create(SimpleType.DYN),
                                    ObjectCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("mp-shadow-secondaryIdentifiers",
                            Object.class,
                            this::secondaryIdentifiers,
                            NullabilityProperties.NULLABLE_EMPTY_LIST)),

            // ASSIGNMENT FUNCTIONS

            // assignment.hasRelation(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "hasRelation",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-hasrelation",
                                    "Returns true if targetRef of the assignment has specified relation.",
                                    SimpleType.BOOL,
                                    AssignmentValueCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("assignment-hasrelation",
                            Object.class, Object.class,
                            CelObjectExtensions::assignmentHasRelation,
                            NullabilityProperties.NULLABLE_FALSE)),

            // assignment.hasDefaultRelation()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "hasDefaultRelation",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-hasdefaultrelation",
                                    "Returns true is the assignment has default relation.",
                                    SimpleType.BOOL,
                                    AssignmentValueCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("assignment-hasdefaultrelation",
                            Object.class,
                            // Note! hasDefaultRelation has different implementation than other has*Relation() functions.
                            CelObjectExtensions::assignmentHasDefaultRelation,
                            NullabilityProperties.NULLABLE_FALSE)),

            // assignment.has*Relation()
            createHasRelationFunction("Approver", SchemaConstants.ORG_APPROVER),
            createHasRelationFunction("Consent", SchemaConstants.ORG_CONSENT),
            createHasRelationFunction("Deputy", SchemaConstants.ORG_DEPUTY),
            createHasRelationFunction("Meta", SchemaConstants.ORG_META),
            createHasRelationFunction("Owner", SchemaConstants.ORG_OWNER),
            createHasRelationFunction("Related", SchemaConstants.ORG_RELATED),


            // assignment.isTarget(any)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "isTarget",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-istarget",
                                    "Returns true if targetRef of the assignment is of specified type.",
                                    SimpleType.BOOL,
                                    AssignmentValueCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("assignment-istarget",
                            Object.class, Object.class,
                            CelObjectExtensions::assignmentIsTarget,
                            NullabilityProperties.NULLABLE_FALSE)),

            // assignment.isTarget(any, archetypeOid)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "isTarget",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-istarget-archetype",
                                    "Returns true if targetRef of the assignment is of specified type.",
                                    SimpleType.BOOL,
                                    AssignmentValueCelValue.CEL_TYPE, SimpleType.ANY, SimpleType.STRING)),
                    CelFunctionBinding.from("assignment-istarget-archetype",
                            ImmutableList.of(Object.class, Object.class, String.class),
                            this::assignmentIsTargetArchetype,
                            NullabilityProperties.NULLABLE_FALSE)),

            // assignment.isTarget*()
            createIsTargetFunction("Role", RoleType.COMPLEX_TYPE),
            createIsTargetArchetypeFunction("Role", RoleType.COMPLEX_TYPE),
            createIsTargetFunction("Org", OrgType.COMPLEX_TYPE),
            createIsTargetArchetypeFunction("Org", OrgType.COMPLEX_TYPE),
            createIsTargetFunction("Service", ServiceType.COMPLEX_TYPE),
            createIsTargetArchetypeFunction("Service", ServiceType.COMPLEX_TYPE),
            createIsTargetFunction("Policy", PolicyType.COMPLEX_TYPE),
            createIsTargetArchetypeFunction("Policy", PolicyType.COMPLEX_TYPE),
            createIsTargetFunction("User", UserType.COMPLEX_TYPE),
            createIsTargetArchetypeFunction("User", UserType.COMPLEX_TYPE),

            // assignment.target()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "target",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-target",
                                    "Returns object that is the targetRef refers to.",
                                    NullableType.create(ObjectCelValue.CEL_TYPE),
                                    AssignmentValueCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("assignment-target",
                            Object.class,
                            this::assignmentTarget,
                            NullabilityProperties.NULLABLE_NULL)),

            // assignment.targetName()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "targetName",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-targetname",
                                    "Returns name of object that the targetRef refers to.",
                                    NullableType.create(PolyStringCelValue.CEL_TYPE),
                                    AssignmentValueCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("assignment-targetname",
                            Object.class,
                            this::assignmentTargetName,
                            NullabilityProperties.NULLABLE_NULL)),

            // assignment.targetOid()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "targetOid",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-targetoid",
                                    "Returns OID of targetRef of the assignment.",
                                    NullableType.create(SimpleType.STRING),
                                    AssignmentValueCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("assignment-targetoid",
                            Object.class,
                            CelObjectExtensions::assignmentTargetOid,
                            NullabilityProperties.NULLABLE_NULL)),

            // assignment.targetRelation()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "targetRelation",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-targetrelation",
                                    "Returns relation of targetRef of the assignment.",
                                    NullableType.create(QNameCelValue.CEL_TYPE),
                                    AssignmentValueCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("assignment-targetrelation",
                            Object.class,
                            CelObjectExtensions::assignmentTargetRelation,
                            NullabilityProperties.NULLABLE_NULL)),

            // assignment.targetType()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "targetType",
                            CelOverloadDecl.newMemberOverload(
                                    "assignment-targettype",
                                    "Returns type specified in the targetRef of the assignment.",
                                    NullableType.create(QNameCelValue.CEL_TYPE),
                                    AssignmentValueCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("assignment-targettype",
                            Object.class,
                            CelObjectExtensions::assignmentTargetType,
                            NullabilityProperties.NULLABLE_NULL)),

            // DELTA FUNCTIONS

            // objectDelta.estimateAddedValuesFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "estimateAddedValuesFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-estimateAddedValuesFor",
                                    "Returns estimation of added values for specified item.",
                                    ListType.create(SimpleType.ANY),
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-estimateAddedValuesFor",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::estimateAddedValuesFor,
                            NullabilityProperties.NULLABLE_NULL)),

            // objectDelta.estimateDeletedValuesFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "estimateDeletedValuesFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-estimateDeletedValuesFor",
                                    "Returns estimation of deleted values for specified item.",
                                    ListType.create(SimpleType.ANY),
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-estimateDeletedValuesFor",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::estimateDeletedValuesFor,
                            NullabilityProperties.NULLABLE_NULL)),

            // objectDelta.estimateModifiedValuesFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "estimateModifiedValuesFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-estimateModifiedValuesFor",
                                    "Returns estimation of modified values for specified item.",
                                    ListType.create(SimpleType.ANY),
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-estimateModifiedValuesFor",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::estimateModifiedValuesFor,
                            NullabilityProperties.NULLABLE_NULL)),

            // objectDelta.estimateChangedValuesFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "estimateChangedValuesFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-estimateChangedValuesFor",
                                    "Returns estimation of changed values for specified item.",
                                    ListType.create(SimpleType.ANY),
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-estimateChangedValuesFor",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::estimateChangedValuesFor,
                            NullabilityProperties.NULLABLE_NULL)),

            // objectDelta.estimateNewValuesFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "estimateNewValuesFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-estimateNewValuesFor",
                                    "Returns estimation of a list of new values that would result in delta application.",
                                    ListType.create(SimpleType.ANY),
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-estimateNewValuesFor",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::estimateNewValuesFor,
                            NullabilityProperties.NULLABLE_NULL)),

            // objectDelta.findItemDelta(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "findItemDelta",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-findItemDelta",
                                    "Returns item delta for specified item.",
                                    ItemDeltaCelValue.CEL_TYPE,
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-findItemDelta",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::findItemDelta,
                            NullabilityProperties.NULLABLE_NULL)),

            // objectDelta.hasDeltaFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "hasDeltaFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-hasdeltafor",
                                    "Returns true if the delta has delta for specified item.",
                                    SimpleType.BOOL,
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-hasdeltafor",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::hasDeltaFor,
                            NullabilityProperties.NULLABLE_FALSE)),

            // objectDeltaOperation.hasDeltaFor(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "hasDeltaFor",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdeltaoperation-hasdeltafor",
                                    "Returns true if the delta has delta for specified item.",
                                    SimpleType.BOOL,
                                    ObjectDeltaOperationCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdeltaoperation-hasdeltafor",
                            ObjectDeltaOperationCelValue.class, Object.class,
                            CelObjectExtensions::hasDeltaFor,
                            NullabilityProperties.NULLABLE_FALSE)),

            // objectDelta.isItemChanged(path)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "isItemChanged",
                            CelOverloadDecl.newMemberOverload(
                                    "objectdelta-isItemChanged",
                                    "Returns true if the delta is changing specified item.",
                                    SimpleType.BOOL,
                                    ObjectDeltaCelValue.CEL_TYPE, SimpleType.ANY)),
                    CelFunctionBinding.from("objectdelta-isItemChanged",
                            ObjectDeltaCelValue.class, Object.class,
                            CelObjectExtensions::isItemChanged,
                            NullabilityProperties.NULLABLE_FALSE))
        );

    }

    private static Object objectType(ObjectCelValue<?> object) {
        PrismObjectDefinition<?> definition = object.getObject().getDefinition();
        if (definition == null) {
            return NullValue.NULL_VALUE;
        }
        return QNameCelValue.create(definition.getTypeName());
    }

    private static Object estimateAddedValuesFor(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        try {
            return postProcessPrismValues(objectDeltaCelValue.getJavaValue().estimateAddedValuesFor(toPath(path)));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    private static Object estimateDeletedValuesFor(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        try {
            return postProcessPrismValues(objectDeltaCelValue.getJavaValue().estimateDeletedValuesFor(toPath(path)));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    private static Object estimateModifiedValuesFor(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        try {
            return postProcessPrismValues(objectDeltaCelValue.getJavaValue().estimateModifiedValuesFor(toPath(path)));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    private static Object estimateChangedValuesFor(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        try {
            return postProcessPrismValues(objectDeltaCelValue.getJavaValue().estimateChangedValuesFor(toPath(path)));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    private static Object estimateNewValuesFor(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        try {
            return postProcessPrismValues(objectDeltaCelValue.getJavaValue().estimateNewValuesFor(toPath(path)));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    private static Object postProcessPrismValues(Collection<PrismValue> values) {
        if (values == null) {
            return NullValue.NULL_VALUE;
        }
        return values.stream().map(v -> CelTypeMapper.toCelValue(v.getRealValue())).toList();
    }


    private static Object findItemDelta(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        ItemDelta<PrismValue, ItemDefinition<?>> itemDelta = objectDeltaCelValue.getJavaValue().findItemDelta(toPath(path));
        if (itemDelta == null) {
            return NullValue.NULL_VALUE;
        }
        return ItemDeltaCelValue.create(itemDelta);
    }

    private static Object hasDeltaFor(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        return hasDeltaFor(objectDeltaCelValue.getJavaValue(), path);
    }

    private static Object hasDeltaFor(ObjectDeltaOperationCelValue objectDeltaOperationCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        return hasDeltaFor(objectDeltaOperationCelValue.getObjectDelta(), path);
    }

    private static Object hasDeltaFor(ObjectDelta<?> objectDelta, Object path) {
        if (objectDelta == null) {
            return NullValue.NULL_VALUE;
        }
        if (objectDelta.isDelete()) {
            return true;
        }
        return objectDelta.hasItemOrSubitemDelta(toPath(path));
    }

    private static Object isItemChanged(ObjectDeltaCelValue<?> objectDeltaCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        try {
            return objectDeltaCelValue.getJavaValue().isItemChanged(toPath(path));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    private static Object isItemChanged(ObjectDeltaOperationCelValue objectDeltaOperationCelValue, Object path) {
        if (isCelNull(path)) {
            return NullValue.NULL_VALUE;
        }
        ObjectDelta<?> objectDelta = objectDeltaOperationCelValue.getObjectDelta();
        if (objectDelta == null) {
            return NullValue.NULL_VALUE;
        }
        try {
            return objectDelta.isItemChanged(toPath(path));
        } catch (SchemaException e) {
            // Consider: log the error and return null instead?
            throw createException(e);
        }
    }

    @NotNull
    private static ItemPath toPath(@NotNull Object path) {
        if (path instanceof ItemPathCelValue celPath) {
            return celPath.getJavaValue();
        } else if (path instanceof String s) {
            return ItemPath.fromString(s);
        } else if (path instanceof QNameCelValue celQName) {
            return ItemPath.create(celQName.getJavaValue());
        } else if (path instanceof List<?> segments) {
            return ItemPath.create(CelTypeMapper.toCelValues(segments));
        }
        throw new IllegalArgumentException("Unexpected type of path "+path+" ("+path.getClass().getName()+")");
    }

    private Function createHasRelationFunction(String name, final QName relation) {
        return new Function(
                CelFunctionDecl.newFunctionDeclaration(
                        "has" + name + "Relation",
                        CelOverloadDecl.newMemberOverload(
                                "assignment-has" + name.toLowerCase() + "relation",
                                "Returns true if the assignment has " + name.toLowerCase() + " relation.",
                                SimpleType.BOOL,
                                AssignmentValueCelValue.CEL_TYPE)),
                CelFunctionBinding.from("assignment-has" + name.toLowerCase() + "relation",
                        Object.class,
                        a -> assignmentHasRelationInternal(a, relation),
                        NullabilityProperties.NULLABLE_FALSE));
    }

    private Function createIsTargetFunction(String name, final QName type) {
        return new Function(
                CelFunctionDecl.newFunctionDeclaration(
                        "isTarget" + name,
                        CelOverloadDecl.newMemberOverload(
                                "assignment-istarget" + name.toLowerCase(),
                                "Returns true if the assignment target type is " + name.toLowerCase() + ".",
                                SimpleType.BOOL,
                                AssignmentValueCelValue.CEL_TYPE)),
                CelFunctionBinding.from("assignment-istarget" + name.toLowerCase(),
                        Object.class,
                        a -> assignmentIsTargetInternal(a, type),
                        NullabilityProperties.NULLABLE_FALSE));
    }

    private Function createIsTargetArchetypeFunction(String name, final QName type) {
        return new Function(
                CelFunctionDecl.newFunctionDeclaration(
                        "isTarget" + name,
                        CelOverloadDecl.newMemberOverload(
                                "assignment-istarget" + name.toLowerCase() + "-archetype",
                                "Returns true if the assignment target type is " + name.toLowerCase() + " and it has specified archetype.",
                                SimpleType.BOOL,
                                AssignmentValueCelValue.CEL_TYPE,
                                SimpleType.STRING)),
                CelFunctionBinding.from("assignment-istarget" + name.toLowerCase() + "-archetype",
                        Object.class, String.class,
                        (assignment, archetypeOid) -> assignmentIsTargetArchetypeInternal(assignment, type, archetypeOid),
                        NullabilityProperties.NULLABLE_FALSE));
    }

    private static Object assignmentHasDefaultRelation(Object assignmentCelValue) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return false;
        }
        QName relation = targetRef.getRelation();
        if (relation == null) {
            return true;
        }
        return QNameUtil.match(relation, PrismContext.get().getDefaultRelation());
    }

    private static Object assignmentHasRelation(Object assignmentCelValue, Object relationSpec) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return false;
        }
        QName relation = targetRef.getRelation();
        if (relation == null) {
            relation = PrismContext.get().getDefaultRelation();
        }
        if (relationSpec instanceof String relationSpecStr) {
            return relation.getLocalPart().equals(relationSpecStr);
        }
        if (relationSpec instanceof QNameCelValue relationSpecCelQname) {
            return QNameUtil.match(relationSpecCelQname.getQName(), relation);
        }
        return relation.getLocalPart().equals(relationSpec.toString());
    }

    private static Object assignmentHasRelationInternal(Object assignmentCelValue, QName relationSpec) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return false;
        }
        QName relation = targetRef.getRelation();
        if (relation == null) {
            return false;
        }
        return QNameUtil.match(relationSpec, relation);
    }

    private static Object assignmentTargetType(Object assignmentCelValue) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return NullValue.NULL_VALUE;
        }
        if (targetRef.getType() == null) {
            return NullValue.NULL_VALUE;
        }
        return QNameCelValue.create(targetRef.getType());
    }

    private <O extends ObjectType> Object assignmentTarget(Object assignmentCelValue) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return NullValue.NULL_VALUE;
        }
        O target = determineTarget(targetRef);
        if (target == null) {
            return NullValue.NULL_VALUE;
        }
        return ObjectCelValue.create(target.asPrismObject());
    }

    private <O extends ObjectType> Object assignmentTargetName(Object assignmentCelValue) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return NullValue.NULL_VALUE;
        }
        O target = determineTarget(targetRef);
        if (target == null) {
            return NullValue.NULL_VALUE;
        }
        PolyStringType name = target.getName();
        if (name == null) {
            return NullValue.NULL_VALUE;
        }
        return PolyStringCelValue.create(name.toPolyString());
    }

    private static Object assignmentTargetOid(Object assignmentCelValue) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return NullValue.NULL_VALUE;
        }
        String oid = targetRef.getOid();
        if (oid == null) {
            return NullValue.NULL_VALUE;
        }
        return oid;
    }

    private static Object assignmentTargetRelation(Object assignmentCelValue) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return NullValue.NULL_VALUE;
        }
        QName relation = targetRef.getRelation();
        if (relation == null) {
            relation = PrismContext.get().getDefaultRelation();
        }
        return QNameCelValue.create(relation);
    }

    private static boolean assignmentIsTarget(Object assignmentCelValue, Object typeSpec) {
        return isRefTarget(getTargetRef(assignmentCelValue), typeSpec);
    }

    private boolean assignmentIsTargetArchetype(Object[] args) {
        return assignmentIsTargetArchetype(args[0], args[1], (String) args[2]);
    }

    private boolean assignmentIsTargetArchetype(Object assignmentCelValue, Object typeSpec, String archetypeOid) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (!isRefTarget(targetRef, typeSpec)) {
            return false;
        }
        return isRefArchetype(targetRef, archetypeOid);
    }

    private static boolean assignmentIsTargetInternal(Object assignmentCelValue, QName targetSpec) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return false;
        }
        QName type = targetRef.getType();
        if (type == null) {
            return false;
        }
        return QNameUtil.match(targetSpec, type);
    }

    private static ObjectReferenceType getTargetRef(Object assignmentCelValue) {
        if (isCelNull(assignmentCelValue)) {
            return null;
        }
        return ((AssignmentValueCelValue) assignmentCelValue).getContainerValue().asContainerable().getTargetRef();
    }

    private <O extends ObjectType> boolean assignmentIsTargetArchetypeInternal(Object assignmentCelValue, QName targetSpec, String archetypeOid) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
        if (targetRef == null) {
            return false;
        }
        QName type = targetRef.getType();
        if (type == null) {
            return false;
        }
        if (!QNameUtil.match(targetSpec, type)) {
            return false;
        }
        return isRefArchetype(targetRef, archetypeOid);
    }

    private static boolean isRefTarget(ObjectReferenceType targetRef, Object typeSpec) {
        if (targetRef == null) {
            return false;
        }
        QName type = targetRef.getType();
        if (type == null) {
            return false;
        }
        if (typeSpec instanceof String typeSpecStr) {
            return type.getLocalPart().equals(typeSpecStr);
        }
        if (typeSpec instanceof QNameCelValue typeSpecCelQname) {
            return QNameUtil.match(typeSpecCelQname.getQName(), type);
        }
        return type.getLocalPart().equals(typeSpec.toString());
    }

    private <O extends ObjectType> boolean isRefArchetype(ObjectReferenceType targetRef, String archetypeOid) {
        O target = determineTarget(targetRef);
        if (target == null) {
            return false;
        }
        return midpointExpressionFunctions.hasArchetype(target, archetypeOid);
    }

    @Nullable
    private <O extends ObjectType> O determineTarget(ObjectReferenceType targetRef) {
        try {
            return midpointExpressionFunctions.resolveReferenceIfExists(targetRef);
        } catch (CommonException e) {
            LOGGER.warn("Error resolving object reference {}: {} ({})",
                    targetRef, e.getMessage(), e.getClass().getSimpleName());
            return null;
        }
    }

    private static boolean isEffectivelyEnabled(ObjectCelValue<?> objectCelValue) {
        if (CelTypeMapper.isCelNull(objectCelValue)) {
            return false;
        } else {
            PrismObject<?> object = objectCelValue.getObject();
            return (!(object.isOfType(FocusType.class))
                    || FocusTypeUtil.getEffectiveStatus((FocusType) object.asObjectable()) == ActivationStatusType.ENABLED);
        }
    }

    public static Object prismFind(ObjectCelValue<?> objectCelValue, String stringPath) {
        Object o = objectCelValue.getObject().find(PrismContext.get().itemPathParser().asItemPath(stringPath));
        if (o == null) {
            return NullValue.NULL_VALUE;
        }
        return o;
    }

    private static Object prismIndexMap(AbstractContainerValueCelValue<?> celValue, QNameCelValue celQName) {
        if (CelTypeMapper.isCelNull(celValue) || CelTypeMapper.isCelNull(celQName)) {
            return NullValue.NULL_VALUE;
        }
        return CelTypeMapper.toCelValue(celValue.getContainerValue().find(ItemName.fromQName(celQName.getQName())));
    }

    @NotNull
    private List<?> connectorConfiguration(@Nullable Object o, @Nullable Object propertyName) {
        if (CelTypeMapper.isCelNull(o)) {
            return ImmutableList.of();
        }
        if (CelTypeMapper.isCelNull(propertyName)) {
            return ImmutableList.of();
        }
        if (o instanceof ObjectCelValue<?> mpCelObject) {
            if (mpCelObject.getObject().isOfType(ResourceType.class)) {
                //noinspection unchecked
                PrismObject<ResourceType> resource = (PrismObject<ResourceType>)mpCelObject.getObject();
                PrismContainer<?> connectorConfiguration = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
                if (connectorConfiguration == null) {
                    return ImmutableList.of();
                }
                PrismContainer<Containerable> icfConfiguration = connectorConfiguration.findContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME);
                if (icfConfiguration == null) {
                    return ImmutableList.of();
                }
                ItemPath itemPath;
                if (propertyName instanceof QNameCelValue celQName) {
                    itemPath = ItemName.fromQName(celQName.getQName());
                } else if (propertyName instanceof String localPart) {
                    itemPath = ItemName.from(null, localPart);
                } else {
                    throw createException("Function connectorConfiguration() invoked with unknown parameter " + propertyName.getClass());
                }
                PrismProperty<Object> configProperty = icfConfiguration.findProperty(itemPath);
                if (configProperty == null) {
                    return ImmutableList.of();
                }
                return CelTypeMapper.toJavaValues(configProperty.getRealValues());
            }
            throw createException("Function connectorConfiguration() invoked on non-resource object " + mpCelObject.getObject());
        }
        throw createException("Function connectorConfiguration() invoked on unknown object " + o);
    }

    @NotNull
    private List<?> primaryIdentifiers(@Nullable Object o) {
        if (CelTypeMapper.isCelNull(o)) {
            return ImmutableList.of();
        }
        if (o instanceof ObjectCelValue<?> mpCelObject) {
            if (mpCelObject.getObject().isOfType(ShadowType.class)) {
                //noinspection unchecked
                return processShadowIdentifiers(
                        ShadowUtil.getPrimaryIdentifiers((PrismObject<ShadowType>)mpCelObject.getObject()));
            }
            throw createException("Function primaryIdentifier() invoked on non-shadow object " + mpCelObject.getObject());
        }
        throw createException("Function primaryIdentifier() invoked on unknown object " + o);
    }

    @NotNull
    private List<?> secondaryIdentifiers(@Nullable Object o) {
        if (CelTypeMapper.isCelNull(o)) {
            return ImmutableList.of();
        }
        if (o instanceof ObjectCelValue<?> mpCelObject) {
            if (mpCelObject.getObject().isOfType(ShadowType.class)) {
                //noinspection unchecked
                return processShadowIdentifiers(
                        ShadowUtil.getSecondaryIdentifiers((PrismObject<ShadowType>)mpCelObject.getObject()));
            }
            throw createException("Function secondaryIdentifiers() invoked on non-shadow object " + mpCelObject.getObject());
        }
        throw createException("Function secondaryIdentifiers() invoked on unknown object " + o);
    }

    @NotNull
    private List<?> processShadowIdentifiers(@Nullable Collection<ShadowSimpleAttribute<?>> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return ImmutableList.of();
        }
        return identifiers.stream()
                .map(ssa -> ssa.getRealValue())
                .toList();
    }

    private static final class Library implements CelExtensionLibrary<CelObjectExtensions> {
        private final CelObjectExtensions version0;

        private Library(MidpointFunctions midpointExpressionFunctions) {
            version0 = new CelObjectExtensions(midpointExpressionFunctions);
        }

        @Override
        public String name() {
            return "object";
        }

        @Override
        public ImmutableSet<CelObjectExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelObjectExtensions> library(MidpointFunctions midpointExpressionFunctions) {
        return new Library(midpointExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

}
