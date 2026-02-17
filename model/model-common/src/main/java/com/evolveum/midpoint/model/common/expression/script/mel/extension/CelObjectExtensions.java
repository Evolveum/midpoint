/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.value.AbstractContainerValueCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ContainerValueCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ObjectCelValue;

import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
                            "find",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-object-find-string",
                                    "Returns an item to which the specified item path refers.",
                                    SimpleType.ANY,
                                    ObjectCelValue.CEL_TYPE,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("prism-object-find-string", ObjectCelValue.class, String.class,
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
                            this::secondaryIdentifiers))
        );
    }

    private static boolean isEffectivelyEnabled(ObjectCelValue<?> objectCelValue) {
        if (CelTypeMapper.isCellNull(objectCelValue)) {
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
        if (CelTypeMapper.isCellNull(celValue) || CelTypeMapper.isCellNull(celQName)) {
            return NullValue.NULL_VALUE;
        }
        return CelTypeMapper.toCelValue(celValue.getContainerValue().find(ItemName.fromQName(celQName.getQName())));
    }

    @NotNull
    private List<?> connectorConfiguration(@Nullable Object o, @Nullable Object propertyName) {
        if (CelTypeMapper.isCellNull(o)) {
            return ImmutableList.of();
        }
        if (CelTypeMapper.isCellNull(propertyName)) {
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
        if (CelTypeMapper.isCellNull(o)) {
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
        if (CelTypeMapper.isCellNull(o)) {
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
            return "mpObject";
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
