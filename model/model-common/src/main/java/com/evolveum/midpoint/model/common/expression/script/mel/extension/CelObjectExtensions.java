/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.common.expression.script.mel.value.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    public CelObjectExtensions() {
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
                    CelFunctionBinding.from("prism-container-index_map-qname", ContainerValueCelValue.class, QNameCelValue.class,
                            CelObjectExtensions::prismIndexMap)),

                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "isEffectivelyEnabled",
                                CelOverloadDecl.newMemberOverload(
                                        "prism-object-isEffectivelyEnabled",
                                        "Returns true if the object is effectively enabled.",
                                        SimpleType.ANY,
                                        ObjectCelValue.CEL_TYPE)),
                        CelFunctionBinding.from("prism-object-isEffectivelyEnabled", ObjectCelValue.class,
                                CelObjectExtensions::isEffectivelyEnabled)),

            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "findItem",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-object-finditem-string",
                                    "Returns an item to which the specified item path refers.",
                                    SimpleType.ANY,
                                    ObjectCelValue.CEL_TYPE,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("prism-object-finditem-string", ObjectCelValue.class, String.class,
                            CelObjectExtensions::prismFind)),

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
                    CelFunctionBinding.from("mp-resource-connectorConfiguration", Object.class, Object.class,
                            this::connectorConfiguration)),

            // shadow.primaryIdentifiers()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "primaryIdentifiers",
                            CelOverloadDecl.newMemberOverload(
                                    "mp-shadow-primaryIdentifiers",
                                    "Returns list of values of shadow primary identifier.",
                                    ListType.create(SimpleType.DYN),
                                    ObjectCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("mp-shadow-primaryIdentifiers", Object.class,
                            this::primaryIdentifiers)),

            // shadow.secondaryIdentifiers()
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "secondaryIdentifiers",
                            CelOverloadDecl.newMemberOverload(
                                    "mp-shadow-secondaryIdentifiers",
                                    "Returns list of values of shadow secondary identifier.",
                                    ListType.create(SimpleType.DYN),
                                    ObjectCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("mp-shadow-secondaryIdentifiers", Object.class,
                            this::secondaryIdentifiers)),

            // Assignment functions

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

            createIsTargetFunction("Role", RoleType.COMPLEX_TYPE),
            createIsTargetFunction("Org", OrgType.COMPLEX_TYPE),
            createIsTargetFunction("Service", ServiceType.COMPLEX_TYPE),
            createIsTargetFunction("Policy", PolicyType.COMPLEX_TYPE),
            createIsTargetFunction("User", UserType.COMPLEX_TYPE),

            // TODO: isTargetRole(archetype), etc.

            // TODO: targetName() ???

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
                            NullabilityProperties.NULLABLE_NULL))

        );

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

    private static Object assignmentIsTarget(Object assignmentCelValue, Object typeSpec) {
        ObjectReferenceType targetRef = getTargetRef(assignmentCelValue);
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

    private static Object assignmentIsTargetInternal(Object assignmentCelValue, QName targetSpec) {
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

        private Library() {
            version0 = new CelObjectExtensions();
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

    public static CelExtensionLibrary<CelObjectExtensions> library() {
        return new Library();
    }

    @Override
    public int version() {
        return 0;
    }

}
