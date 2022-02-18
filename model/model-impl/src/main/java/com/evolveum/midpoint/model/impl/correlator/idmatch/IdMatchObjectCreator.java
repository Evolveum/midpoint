/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchObject;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.VariableItemPathSegment;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates ID Match object for given situation.
 */
class IdMatchObjectCreator {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchObjectCreator.class);

    @NotNull private final CorrelatorContext<IdMatchCorrelatorType> correlatorContext;
    @NotNull private final FocusType preFocus;
    @NotNull private final ShadowType shadow;

    /** Value serving as a prefix for SOR IDs generated. */
    @NotNull private final String sorIdPrefix;

    /** A property to be used as part of the SOR ID in the requests. */
    @NotNull private final ItemPath sorIdPropertyPath;

    IdMatchObjectCreator(
            @NotNull CorrelatorContext<IdMatchCorrelatorType> correlatorContext,
            @NotNull FocusType preFocus,
            @NotNull ShadowType shadow) {
        this.correlatorContext = correlatorContext;
        this.preFocus = preFocus;
        this.shadow = shadow;

        IdMatchCorrelatorType configBean = correlatorContext.getConfigurationBean();
        sorIdPrefix = requireNonNullElse(configBean.getSorIdentifierPrefix(), "");
        if (configBean.getSorIdentifierProperty() != null) {
            sorIdPropertyPath = configBean.getSorIdentifierProperty().getItemPath();
        } else {
            sorIdPropertyPath = ItemPath.create(
                    new VariableItemPathSegment(new QName(ExpressionConstants.VAR_SHADOW)),
                    ShadowType.F_ATTRIBUTES,
                    SchemaConstants.ICFS_UID);
        }
    }

    public IdMatchObject create() throws SchemaException {
        IdMatchAttributesType attributes = new IdMatchAttributesType(PrismContext.get());
        for (PrismProperty<?> property : getCorrelationProperties()) {
            //noinspection unchecked
            attributes.asPrismContainerValue().add(
                    property.clone());
        }
        return IdMatchObject.create(getSorIdentifierValue(), attributes);
    }

    private @NotNull List<PrismProperty<?>> getCorrelationProperties() throws SchemaException {
        List<CorrelationPropertyDefinitionType> explicitDefinitions = getExplicitPropertyDefinitions();
        if (!explicitDefinitions.isEmpty()) {
            List<PrismProperty<?>> properties = new ArrayList<>();
            for (CorrelationPropertyDefinitionType explicitDefinition : explicitDefinitions) {
                PrismProperty<?> sourceProperty = resolveProperty(explicitDefinition.getSource().getItemPath());
                if (sourceProperty != null) {
                    properties.add(sourceProperty);
                }
            }
            return properties;
        } else {
            // Fallback: take all single-valued properties from the focus
            return MatchingUtil.getSingleValuedProperties(preFocus);
        }
    }

    private String getSorIdentifierValue() throws SchemaException {
        PrismProperty<?> identifier = resolveProperty(sorIdPropertyPath);
        if (identifier == null || identifier.isEmpty()) {
            throw new SchemaException("No SOR identifier value (" + sorIdPropertyPath + ") found");
        }
        if (identifier.size() > 1) {
            throw new SchemaException("SOR identifier (" + sorIdPropertyPath + ") has more than one value: " + identifier);
        }
        return sorIdPrefix + identifier.getRealValue();
    }

    // TODO re-use variable support in expression util
    private @Nullable PrismProperty<?> resolveProperty(@NotNull ItemPath itemPath) throws SchemaException {
        QName varName = itemPath.firstToVariableNameOrNull();
        LOGGER.trace("Trying to find SOR identifier value by looking at: {} (var = {})", itemPath, varName);
        if (varName == null) {
            return MatchingUtil.findProperty(preFocus, itemPath);
        } else if (isShadow(varName)) {
            return MatchingUtil.findProperty(shadow, itemPath.rest());
        } else if (isFocus(varName)) {
            return MatchingUtil.findProperty(preFocus, itemPath.rest());
        } else {
            throw new SchemaException("Unknown variable: " + varName);
        }
    }

    private boolean isShadow(QName varName) {
        String localPart = varName.getLocalPart();
        return ExpressionConstants.VAR_SHADOW.equals(localPart)
                || ExpressionConstants.VAR_PROJECTION.equals(localPart)
                || ExpressionConstants.VAR_ACCOUNT.equals(localPart);
    }

    private boolean isFocus(QName varName) {
        String localPart = varName.getLocalPart();
        return ExpressionConstants.VAR_FOCUS.equals(localPart)
                || ExpressionConstants.VAR_USER.equals(localPart);
    }

    private List<CorrelationPropertyDefinitionType> getExplicitPropertyDefinitions() {
        ObjectSynchronizationType synchronizationBean = correlatorContext.getSynchronizationBean();
        if (synchronizationBean != null && synchronizationBean.getCorrelationProperties() != null) {
            return synchronizationBean.getCorrelationProperties().getProperty();
        } else {
            return List.of();
        }
    }
}
