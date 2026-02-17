/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ObjectCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ReferenceCelValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.NullableType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.CelValue;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * Extensions for CEL compiler and runtime implementing access to midPoint functionality.
 *
 * @author Radovan Semancik
 */
public class CelMidPointExtensions extends AbstractMidPointCelExtensions {

    private static final Trace LOGGER = TraceManager.getTrace(CelMidPointExtensions.class);

    private static final String FUNCTION_NAME_PREFIX = "midpoint";
    private static final String FUNCTION_NAME_PREFIX_DOT = FUNCTION_NAME_PREFIX+".";
    private static final String FUNCTION_NAME_PREFIX_DASH = FUNCTION_NAME_PREFIX+"-";

    private final PrismContext prismContext;
    private final MidpointFunctions midpointExpressionFunctions;

    public CelMidPointExtensions(MidpointFunctions midpointExpressionFunctions) {
        this.midpointExpressionFunctions = midpointExpressionFunctions;
        this.prismContext = PrismContext.get();
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

                // createEmptyObject(...) and createEmptyObjectWithName(...) do not make sense for MEL

                // executeChanges(...): CEL functions are supposed to be free of side effects.
                // The executeChanges() method is a side effect on steroids, as well as potential security issue.
                // Therefore, we are not implementing it ... at least for now.
                // Similarly for addObject(), modifyObject(), deleteObject(), recompute()

                // describeResourceObjectSet, describeResourceObjectSetLong, describeResourceObjectSetShort: not implemented
                // Are these even useful in CEL?

                // midpoint.getLinkedShadow(focus, resourceOid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadow",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadow",
                                        "TODO.",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getLinkedShadow",
                                ObjectCelValue.class, String.class,
                                this::getLinkedShadowRepo)

                ),

                // midpoint.getLinkedShadow(focus, resourceOid, repositoryObjectOnly)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadow",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowRepo",
                                        "TODO.",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowRepo",
                                ImmutableList.of(ObjectCelValue.class, String.class, Boolean.class),
                                this::getLinkedShadowRepo)

                ),

                // midpoint.getLinkedShadow(focus, resourceOid, kind, intent)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadow",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowKindIntent",
                                        "TODO.",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowKindIntent",
                                ImmutableList.of(ObjectCelValue.class, String.class, String.class, String.class),
                                this::getLinkedShadowKindIntent)

                ),

                // midpoint.getLinkedShadow(focus, resourceOid, kind, intent, repositoryObjectOnly)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadow",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowKindIntentRepo",
                                        "TODO.",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowKindIntentRepo",
                                ImmutableList.of(ObjectCelValue.class, String.class, String.class, String.class, Boolean.class),
                                this::getLinkedShadowKindIntentRepo)

                ),

                // midpoint.getLinkedShadows(focus, resourceOid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadows",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadows",
                                        "TODO.",
                                        ListType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getLinkedShadows",
                                ObjectCelValue.class, String.class,
                                this::getLinkedShadowsRepo)

                ),

                // midpoint.getLinkedShadows(focus, resourceOid, repositoryObjectOnly)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadows",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowsRepo",
                                        "TODO.",
                                        ListType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getLinkedShadowsRepo",
                                ImmutableList.of(ObjectCelValue.class, String.class, Boolean.class),
                                this::getLinkedShadowsRepo)
                ),

                // midpoint.getManagersOids(user)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getManagersOids",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getManagersOids",
                                        "TODO.",
                                        ListType.create(SimpleType.STRING),
                                        ObjectCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getManagersOids", ObjectCelValue.class,
                                this::getManagersOids)

                ),

                // midpoint.getManagersOidsExceptUser(user)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getManagersOidsExceptUser",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getManagersOidsExceptUser",
                                        "TODO.",
                                        ListType.create(SimpleType.STRING),
                                        ObjectCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getManagersOidsExceptUser", ObjectCelValue.class,
                                this::getManagersOidsExceptUser)

                ),

                // midpoint.getObject(qname(type), oid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getObject",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getObject-qname",
                                        "Returns object for provided OID. It retrieves the object from an appropriate source "
                                                + "for an object type (e.g. internal repository, resource or both), merging data as necessary, "
                                                + "processing any policies, caching mechanisms, etc..",
                                        ObjectCelValue.CEL_TYPE,
                                        NullableType.create(QNameCelValue.CEL_TYPE), SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getObject-qname", QNameCelValue.class, String.class,
                                this::getObject)

                ),

                // midpoint.getObject(string(type), oid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getObject",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getObject-string",
                                        "Returns object for provided OID. It retrieves the object from an appropriate source "
                                                + "for an object type (e.g. internal repository, resource or both), merging data as necessary, "
                                                + "processing any policies, caching mechanisms, etc..",
                                        ObjectCelValue.CEL_TYPE,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getObject-string", String.class, String.class,
                                this::getObject)

                ),

                // TODO(maybe): getObject with options

                // midpoint.getObjectsInConflictOnPropertyValue(object, propertyPathString, propertyValue, getAllConflicting)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getObjectsInConflictOnPropertyValue",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getObjectsInConflictOnPropertyValue",
                                        "TODO.",
                                        ListType.create(ObjectCelValue.CEL_TYPE),
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.ANY, SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getObjectsInConflictOnPropertyValue",
                                ImmutableList.of(ObjectCelValue.class, String.class, Object.class, Boolean.class),
                                this::getObjectsInConflictOnPropertyValue)

                ),

                // TODO:later: midpoint.getObject(type, oid, options)
                // Need to figure out how to efficiently work with options in CEL

                // TODO: to consider:
                // getObjectRef(@Nullable ShadowAssociationValueType associationValueBean)
                // getObjectName(@Nullable ShadowAssociationValueType associationValueBean)
                // getMetadata(...): can we work with ValueMetadataType in CEL?

                // TODO: to implement:
                // get*Timestamp() TODO
                // get*Refs(), get*Approver*(), etc.

                // midpoint.getOrgByName(name)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getOrgByName",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getOrgByName",
                                        "TODO.",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getOrgByName", String.class,
                                this::getOrgByName)

                ),

                // getUserByOid() uses repository service directly, bypassing authorization checking.
                // This is not safe for MEL!

//                // midpoint.getUserByOid(oid)
//                new Function(
//                        CelFunctionDecl.newFunctionDeclaration(
//                                FUNCTION_NAME_PREFIX_DOT + "getUserByOid",
//                                CelOverloadDecl.newGlobalOverload(
//                                        FUNCTION_NAME_PREFIX_DASH + "getUserByOid",
//                                        "Uses repository service directly, bypassing authorization checking.",
//                                        ObjectCelValue.CEL_TYPE,
//                                        SimpleType.STRING)),
//                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getUserByOid", String.class,
//                                this::getUserByOid)
//
//                ),

                // midpoint.hello
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "hello",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "hello",
                                        "Test/sanity function.",
                                        SimpleType.STRING,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "hello", String.class,
                                this::hello)

                ),

                // midpoint.isFocusActivated()
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "isFocusActivated",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "isFocusActivated",
                                        "Does the current clockwork operation bring the focus into existence and being effectively enabled? "
                                                + "(So, previously it was either non-existent or effectively disabled.)",
                                        SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "isFocusActivated",
                                ImmutableList.of(),
                                this::isFocusActivated)
                ),

                // midpoint.isFocusDeactivated()
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "isFocusDeactivated",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "isFocusDeactivated",
                                        "Does the current clockwork operation delete or effectively disable the focus? "
                                                + "(So, previously it existed and was effectively enabled.)",
                                        SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "isFocusDeactivated",
                                ImmutableList.of(),
                                this::isFocusDeactivated)
                ),

                // midpoint.isFocusDeleted()
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "isFocusDeleted",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "isFocusDeleted",
                                        "Does the current clockwork operation delete the focus?",
                                        SimpleType.BOOL)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "isFocusDeleted",
                                ImmutableList.of(),
                                this::isFocusDeleted)
                ),

                // midpoint.isUniquePropertyValue(object, propertyPathString, propertyValue)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "isUniquePropertyValue",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "isUniquePropertyValue",
                                        "TODO",
                                        SimpleType.BOOL,
                                        ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.ANY)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "isUniquePropertyValue",
                                ImmutableList.of(ObjectCelValue.class, String.class, Object.class),
                                this::isUniquePropertyValue)

                ),

                // midpoint.isUniqueAccountValue(resource, shadow, attributeName, attributeValue)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "isUniqueAccountValue",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "isUniqueAccountValue",
                                        "Checks if the value `attributeValue` of `attributeName` in given shadow is unique on given resource.",
                                        SimpleType.BOOL,
                                        ObjectCelValue.CEL_TYPE, ObjectCelValue.CEL_TYPE, SimpleType.STRING, SimpleType.ANY)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "isUniqueAccountValue",
                                ImmutableList.of(ObjectCelValue.class, ObjectCelValue.class, String.class, Object.class),
                                this::isUniqueAccountValue)

                ),

                // midpoint.resolveReference(ref)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "resolveReference",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "resolveReference",
                                        "TODO.",
                                        ObjectCelValue.CEL_TYPE,
                                        ReferenceCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "resolveReference", ReferenceCelValue.class,
                                this::resolveReference)

                ),

                // midpoint.resolveReferenceIfExists(ref)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "resolveReferenceIfExists",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "resolveReferenceIfExists",
                                        "TODO.",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        ReferenceCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "resolveReferenceIfExists", ReferenceCelValue.class,
                                this::resolveReferenceIfExists)

                ),

                // midpoint.searchObjects(qname(type), filter)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "searchObjects",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "searchObjects-qname",
                                        "Searches through all object of a specified type. Returns a list of objects that "
                                                + "match search criteria.",
                                        ListType.create(ObjectCelValue.CEL_TYPE),
                                        NullableType.create(QNameCelValue.CEL_TYPE), SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "searchObjects-qname", QNameCelValue.class, Object.class,
                                this::searchObjects)

                ),

                // midpoint.searchObjects(string(type), filter)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "searchObjects",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "searchObjects-string",
                                        "Searches through all object of a specified type. Returns a list of objects that "
                                                + "match search criteria.",
                                        ListType.create(ObjectCelValue.CEL_TYPE),
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "searchObjects-string", String.class, Object.class,
                                this::searchObjects)

                ),

                // searchObjectsIterative: not implemented, at least not for now
                // Could we even do that in CEL?

                // midpoint.searchShadowOwner(oid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "searchShadowOwner",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "searchShadowOwner",
                                        "TODO",
                                        NullableType.create(ObjectCelValue.CEL_TYPE),
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "searchShadowOwner", String.class,
                                this::searchShadowOwner)

                )

                // selectIdentityItemValues: not implemented yet.
                // This would probably require some rework, as FocusIdentitySourceTypeUtil is not available in CEL.

        );
    }

    private boolean isFocusActivated(Object[] objects) {
        return midpointExpressionFunctions.isFocusActivated();
    }

    private boolean isFocusDeactivated(Object[] objects) {
        return midpointExpressionFunctions.isFocusDeactivated();
    }

    private boolean isFocusDeleted(Object[] objects) {
        return midpointExpressionFunctions.isFocusDeleted();
    }

    private CelValue getLinkedShadowRepo(ObjectCelValue<FocusType> celFocus, String oid) {
        try {
            return toCelObject(midpointExpressionFunctions.getLinkedShadow(toJavaObjectable(celFocus), oid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private CelValue getLinkedShadowRepo(Object[] args) {
        try {
            return toCelObject(midpointExpressionFunctions.getLinkedShadow(
                    toJavaObjectable((ObjectCelValue<FocusType>)args[0]),
                    (String)args[1],
                    (Boolean)args[2]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private CelValue getLinkedShadowKindIntent(Object[] args) {
        try {
            return toCelObject(midpointExpressionFunctions.getLinkedShadow(
                    toJavaObjectable((ObjectCelValue<FocusType>)args[0]),
                    (String)args[1],
                    ShadowKindType.fromValue((String)args[2]),
                    (String)args[3]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private CelValue getLinkedShadowKindIntentRepo(Object[] args) {
        try {
            return toCelObject(midpointExpressionFunctions.getLinkedShadow(
                    toJavaObjectable((ObjectCelValue<FocusType>)args[0]),
                    (String)args[1],
                    ShadowKindType.fromValue((String)args[2]),
                    (String)args[3],
                    (Boolean)args[4]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private List<CelValue> getLinkedShadowsRepo(ObjectCelValue<FocusType> celFocus, String oid) {
        try {
            return toCelObjectList(midpointExpressionFunctions.getLinkedShadows(toJavaObjectable(celFocus), oid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private List<CelValue> getLinkedShadowsRepo(Object[] args) {
        try {
            return toCelObjectList(midpointExpressionFunctions.getLinkedShadows(
                    toJavaObjectable((ObjectCelValue<FocusType>)args[0]),
                    (String)args[1],
                    (Boolean)args[2]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private Collection<String> getManagersOids(ObjectCelValue<UserType> celUser) {
        try {
            return midpointExpressionFunctions.getManagersOids(celUser.getObject().asObjectable());
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private Collection<String> getManagersOidsExceptUser(ObjectCelValue<UserType> celUser) {
        try {
            return midpointExpressionFunctions.getManagersOidsExceptUser(celUser.getObject().asObjectable());
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private CelValue getOrgByName(String name) {
        try {
            return toCelObject(midpointExpressionFunctions.getOrgByName(name));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private CelValue getUserByOid(String oid) {
        try {
            return toCelObject(midpointExpressionFunctions.getObject(UserType.class, oid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    // Test/sanity function.
    private String hello(String s) {
        if (midpointExpressionFunctions == null) {
            throw createException("No midpointExpressionFunctions");
        }
        return "Hello " + s;
    }

    private <O extends ObjectType> CelValue getObject(String typeLocalPart, String oid) {
        return getObject(new QName(ObjectFactory.NAMESPACE, typeLocalPart), oid);
    }

    private <O extends ObjectType> CelValue getObject(QNameCelValue celTypeQname, String oid) {
        return getObject(celTypeQname.getQName(), oid);
    }

    private <O extends ObjectType> CelValue getObject(QName type, String oid) {
        Class<O> typeClass = prismContext.getSchemaRegistry().determineClassForType(type);
        try {
            return toCelObject(midpointExpressionFunctions.getObject(typeClass, oid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <O extends ObjectType> List<CelValue> getObjectsInConflictOnPropertyValue(Object[] args) {
        try {
            return toCelObjectList(midpointExpressionFunctions.getObjectsInConflictOnPropertyValue(
                    toJavaObjectable((ObjectCelValue<O>) args[0]),
                    (String) args[1], args[2], (Boolean)args[3]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private <O extends ObjectType> List<CelValue> toCelObjectList(List<O> javaObjects) {
        return javaObjects.stream().map(CelMidPointExtensions::toCelObject).toList();
    }

    @SuppressWarnings("unchecked")
    private <O extends ObjectType> boolean isUniquePropertyValue(Object[] args) {
        ObjectCelValue<O> object = (ObjectCelValue<O>) args[0];
        try {
            return midpointExpressionFunctions.isUniquePropertyValue(toJavaObjectable(object), (String) args[1], args[2]);
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isUniqueAccountValue(Object[] args) {
        ObjectCelValue<ResourceType> resource = (ObjectCelValue<ResourceType>) args[0];
        ObjectCelValue<ShadowType> shadow = (ObjectCelValue<ShadowType>) args[1];
        try {
            return midpointExpressionFunctions.isUniqueAccountValue(
                    toJavaObjectable(resource),
                    toJavaObjectable(shadow),
                    (String) args[2], args[3]);
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private <O extends ObjectType> CelValue resolveReference(ReferenceCelValue referenceCelValue) {
        try {
            return toCelObject(midpointExpressionFunctions.resolveReference((ObjectReferenceType)referenceCelValue.getObjectReferenceValue().asReferencable()));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private <O extends ObjectType> CelValue resolveReferenceIfExists(ReferenceCelValue referenceCelValue) {
        try {
            return toCelObject(midpointExpressionFunctions.resolveReferenceIfExists((ObjectReferenceType)referenceCelValue.getObjectReferenceValue().asReferencable()));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private <O extends ObjectType> List<CelValue> searchObjects(String typeLocalPart, Object filter) {
        return searchObjects(new QName(ObjectFactory.NAMESPACE, typeLocalPart), filter);
    }

    private <O extends ObjectType> List<CelValue> searchObjects(QNameCelValue celTypeQname, Object filter) {
        return searchObjects(celTypeQname.getQName(), filter);
    }

    private <O extends ObjectType> List<CelValue> searchObjects(QName type, Object filter) {
        Class<O> typeClass = prismContext.getSchemaRegistry().determineClassForType(type);
        String filterString;
        if (CelTypeMapper.isCellNull(filter)) {
            filterString = null;
        } else {
            filterString = (String) filter;
        }
        try {
            return toCelObjectList(midpointExpressionFunctions.searchObjects(typeClass, filterString));
        } catch (CommonException e) {
            throw createException(e);
        }
    }


    @Nullable
    private <F extends FocusType> CelValue searchShadowOwner(String accountOid) {
        try {
            return toCelObjectPrism(midpointExpressionFunctions.searchShadowOwner(accountOid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }


    private static <O extends ObjectType> CelValue toCelObject(O o) {
        if (o == null) {
            return NullValue.NULL_VALUE;
        }
        //noinspection unchecked
        return ObjectCelValue.create((PrismObject<O>)o.asPrismObject());
    }

    private static <O extends ObjectType> CelValue toCelObjectPrism(PrismObject<O> o) {
        if (o == null) {
            return NullValue.NULL_VALUE;
        }
        //noinspection unchecked
        return ObjectCelValue.create(o);
    }


    private <O extends ObjectType> O toJavaObjectable(ObjectCelValue<O> celObject) {
        return celObject.getObject().asObjectable();
    }


    private static final class Library implements CelExtensionLibrary<CelMidPointExtensions> {
        private final CelMidPointExtensions version0;

        private Library(MidpointFunctions midpointExpressionFunctions) {
            version0 = new CelMidPointExtensions(midpointExpressionFunctions);
        }

        @Override
        public String name() {
            return FUNCTION_NAME_PREFIX;
        }

        @Override
        public ImmutableSet<CelMidPointExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelMidPointExtensions> library(MidpointFunctions midpointExpressionFunctions) {
        return new Library(midpointExpressionFunctions);
    }

    @Override
    public int version() {
        return 0;
    }

}
