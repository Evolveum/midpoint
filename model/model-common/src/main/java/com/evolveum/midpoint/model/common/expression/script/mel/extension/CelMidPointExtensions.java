/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ObjectCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
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
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;

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

    private final MidpointFunctions midpointExpressionFunctions;

    public CelMidPointExtensions(MidpointFunctions midpointExpressionFunctions) {
        this.midpointExpressionFunctions = midpointExpressionFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

                // midpoint.getLinkedShadow(focus, resourceOid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getLinkedShadow",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getLinkedShadow",
                                        "TODO.",
                                        ObjectCelValue.CEL_TYPE,
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
                                        ObjectCelValue.CEL_TYPE,
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
                                        ObjectCelValue.CEL_TYPE,
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
                                        ObjectCelValue.CEL_TYPE,
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
                                        "TODO.",
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
                                        "TODO.",
                                        ObjectCelValue.CEL_TYPE,
                                        SimpleType.STRING, SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getObject-string", String.class, String.class,
                                this::getObject)

                ),

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

                // midpoint.getOrgByName(name)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getOrgByName",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getOrgByName",
                                        "TODO.",
                                        ObjectCelValue.CEL_TYPE,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getOrgByName", String.class,
                                this::getOrgByName)

                ),

                // midpoint.getUserByOid(oid)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                FUNCTION_NAME_PREFIX_DOT + "getUserByOid",
                                CelOverloadDecl.newGlobalOverload(
                                        FUNCTION_NAME_PREFIX_DASH + "getUserByOid",
                                        "TODO.",
                                        ObjectCelValue.CEL_TYPE,
                                        SimpleType.STRING)),
                        CelFunctionBinding.from(FUNCTION_NAME_PREFIX_DASH + "getUserByOid", String.class,
                                this::getUserByOid)

                ),

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

                )


        );
    }

    private ObjectCelValue<ShadowType> getLinkedShadowRepo(ObjectCelValue<FocusType> celFocus, String oid) {
        try {
            return toCelObject(midpointExpressionFunctions.getLinkedShadow(toJavaObjectable(celFocus), oid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private ObjectCelValue<ShadowType> getLinkedShadowRepo(Object[] args) {
        try {
            return toCelObject(midpointExpressionFunctions.getLinkedShadow(
                    toJavaObjectable((ObjectCelValue<FocusType>)args[0]),
                    (String)args[1],
                    (Boolean)args[2]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private ObjectCelValue<ShadowType> getLinkedShadowKindIntent(Object[] args) {
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

    private ObjectCelValue<ShadowType> getLinkedShadowKindIntentRepo(Object[] args) {
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

    private List<ObjectCelValue<ShadowType>> getLinkedShadowsRepo(ObjectCelValue<FocusType> celFocus, String oid) {
        try {
            return toCelObjectList(midpointExpressionFunctions.getLinkedShadows(toJavaObjectable(celFocus), oid));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private List<ObjectCelValue<ShadowType>> getLinkedShadowsRepo(Object[] args) {
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

    private ObjectCelValue<OrgType> getOrgByName(String name) {
        try {
            return toCelObject(midpointExpressionFunctions.getOrgByName(name));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private ObjectCelValue<UserType> getUserByOid(String oid) {
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

    private <O extends ObjectType> ObjectCelValue<O> getObject(String typeLocalPart, String oid) {
        return getObject(new QName(ObjectFactory.NAMESPACE, typeLocalPart), oid);
    }

    private <O extends ObjectType> ObjectCelValue<O> getObject(QNameCelValue celTypeQname, String oid) {
        return getObject(celTypeQname.getQName(), oid);
    }

    private <O extends ObjectType> ObjectCelValue<O> getObject(QName type, String oid) {
        Class<O> typeClass = PrismContext.get().getSchemaRegistry().determineClassForType(type);
        try {
            return toCelObject(midpointExpressionFunctions.getObject(typeClass, oid));
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException e) {
            throw createException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <O extends ObjectType> List<ObjectCelValue<O>> getObjectsInConflictOnPropertyValue(Object[] args) {
        try {
            return toCelObjectList(midpointExpressionFunctions.getObjectsInConflictOnPropertyValue(
                    toJavaObjectable((ObjectCelValue<O>) args[0]),
                    (String) args[1], args[2], (Boolean)args[3]));
        } catch (CommonException e) {
            throw createException(e);
        }
    }

    private <O extends ObjectType> List<ObjectCelValue<O>> toCelObjectList(List<O> javaObjects) {
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

    private static <O extends ObjectType> ObjectCelValue<O> toCelObject(O o) {
        if (o == null) {
            return null;
        }
        //noinspection unchecked
        return ObjectCelValue.create((PrismObject<O>)o.asPrismObject());
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
