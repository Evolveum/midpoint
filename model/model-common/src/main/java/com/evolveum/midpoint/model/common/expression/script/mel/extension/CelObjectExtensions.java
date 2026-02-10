/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.common.expression.script.mel.value.ObjectCelValue;

import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
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

            // resource.connectorConfiguration(propertyName)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "connectorConfiguration",
                            CelOverloadDecl.newMemberOverload(
                                    "mp-resource-connectorConfiguration",
                                    "TODO",
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
                                    "TODO",
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
                                    "TODO",
                                    ListType.create(SimpleType.DYN),
                                    ObjectCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("mp-shadow-secondaryIdentifiers", Object.class,
                            this::secondaryIdentifiers))
        );
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
