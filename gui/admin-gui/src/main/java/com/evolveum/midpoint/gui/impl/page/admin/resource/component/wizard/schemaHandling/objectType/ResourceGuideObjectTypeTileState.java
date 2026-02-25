/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents UI state of tiles in the Resource Object Type wizard.
 * Determines badge, recommendation, and lock status based on
 * current object type configuration.
 */
public enum ResourceGuideObjectTypeTileState {

    NORMAL(null),
    CONFIGURED(new BadgeSpec(
            "badge bg-light text-success border border-success",
            "",
            "ResourceObjectTypeWizardChoicePanel.configured")),
    RECOMMENDED(new BadgeSpec(
            "badge bg-light text-primary border border-primary",
            "",
            "ResourceObjectTypeWizardChoicePanel.recommended")),
    TEMPORARY_LOCKED(null);

    private final @Nullable BadgeSpec spec;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceGuideObjectTypeTileState.class);

    ResourceGuideObjectTypeTileState(@Nullable BadgeSpec spec) {
        this.spec = spec;
    }

    public @NotNull IModel<Badge> badgeModel(@NotNull ResourceObjectTypeWizardChoicePanel panel) {
        if (spec == null) {
            return Model.of();
        }

        String text = panel.getPageBase().createStringResource(spec.labelKey()).getString();
        return spec.iconCss() != null
                ? Model.of(new Badge(spec.cssClass(), spec.iconCss(), text))
                : Model.of(new Badge(spec.cssClass(), text));
    }

    public boolean isLocked() {
        return this == TEMPORARY_LOCKED;
    }

    public static @NotNull ResourceGuideObjectTypeTileState computeState(
            @NotNull ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType tile,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            ResourceObjectTypeWizardChoicePanel components) {

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> wrapper = valueModel.getObject();
        ResourceObjectTypeDefinitionType real = wrapper != null ? wrapper.getRealValue().clone() : null;
        if (real == null) {
            return NORMAL;
        }

        //noinspection unchecked
        WebPrismUtil.cleanupEmptyContainerValue(real.asPrismContainerValue());

        boolean correlationConfigured = isCorrelationConfigured(real);
        boolean skipCorrelationConfiguration = isSkipCorrelationConfiguration(real, components.getPageBase());
        boolean mappingConfigured = isAttributeMappingConfigured(real);
        boolean synchronizationConfigured = isSynchronizationConfigured(real);

        return switch (tile) {
            case BASIC -> CONFIGURED;

            case CORRELATION -> {
                if (correlationConfigured) {
                    yield CONFIGURED;
                }
                if (skipCorrelationConfiguration) {
                    yield NORMAL;
                }

                yield RECOMMENDED;
            }

            case ATTRIBUTE_MAPPING -> {
                if (mappingConfigured) {
                    yield CONFIGURED;
                }
                if (correlationConfigured) {
                    yield RECOMMENDED;
                }

                if (skipCorrelationConfiguration && synchronizationConfigured) {
                    yield RECOMMENDED;
                }

                yield NORMAL;
            }

            case SYNCHRONIZATION -> {
                if (synchronizationConfigured) {
                    yield CONFIGURED;
                }
                if (correlationConfigured || skipCorrelationConfiguration) {
                    yield RECOMMENDED;
                }
                yield NORMAL;
            }
            case CAPABILITIES, POLICIES -> NORMAL;

            case ACTIVATION -> isActivationEnabled(real) ? NORMAL : TEMPORARY_LOCKED;

            case CREDENTIALS -> isCredentialsEnabled(real) ? NORMAL : TEMPORARY_LOCKED;
        };
    }

    private static boolean isSynchronizationConfigured(@NotNull ResourceObjectTypeDefinitionType real) {
        SynchronizationReactionsType synchronization = real.getSynchronization();
        return synchronization != null
                && synchronization.getReaction() != null
                && !synchronization.getReaction().isEmpty();
    }

    //For now, we decided to not ignore initial focus object.
    private static boolean isSkipCorrelationConfiguration(
            @NotNull ResourceObjectTypeDefinitionType real,
            @NotNull PageBase pageBase) {

        boolean isSynchronizationConfigured = isSynchronizationConfigured(real);

        if (isSynchronizationConfigured) {
            return true;
        }

        ResourceObjectFocusSpecificationType focus = real.getFocus();
        ObjectReferenceType archetypeRef = focus.getArchetypeRef();

        Task task = pageBase.createSimpleTask("Count focus");

        ObjectQuery query;

        if (archetypeRef != null) {
            query = PrismContext.get().queryFor(FocusType.class)
                    .item(FocusType.F_ARCHETYPE_REF).ref(archetypeRef.getOid())
                    .build();
        } else {
            query = PrismContext.get().queryFor(FocusType.class)
                    .build();
        }

        ModelService modelService = pageBase.getModelService();
        try {
            Integer counted = modelService.countObjects(FocusType.class, query, null, task, task.getResult());
            if (counted == null) {
                return true;
            }

            // TODO this is temporary solution. We will design better one when we decide how to handle initial focus object.
            if (archetypeRef == null && focus.getType() != null && focus.getType() == UserType.COMPLEX_TYPE) {
                return counted <= 1;
            }

            return counted == 0;
        } catch (Exception e) {
            LOGGER.error("Failed to count focus objects: {}", e.getMessage(), e);
            return true;
        }
    }

    private static boolean isActivationEnabled(@NotNull ResourceObjectTypeDefinitionType real) {
        CapabilityCollectionType caps = real.getConfiguredCapabilities();

        if (caps == null || caps.getActivation() == null) {
            return true; // default enabled
        }

        return !Boolean.FALSE.equals(caps.getActivation().isEnabled());
    }

    private static boolean isCredentialsEnabled(@NotNull ResourceObjectTypeDefinitionType real) {
        CapabilityCollectionType caps = real.getConfiguredCapabilities();

        if (caps == null || caps.getCredentials() == null) {
            return true; // default enabled
        }

        return !Boolean.FALSE.equals(caps.getCredentials().isEnabled());
    }

    private static boolean isCorrelationConfigured(@NotNull ResourceObjectTypeDefinitionType real) {
        CorrelationDefinitionType correlationDef = real.getCorrelation();
        return correlationDef != null && correlationDef.getCorrelators() != null;
    }

    private static boolean isAttributeMappingConfigured(@NotNull ResourceObjectTypeDefinitionType real) {
        List<ResourceAttributeDefinitionType> attributeDefList = real.getAttribute();

        if (attributeDefList == null || attributeDefList.isEmpty()) {
            return false;
        }

        for (ResourceAttributeDefinitionType attributeDefinitionType : attributeDefList) {
            List<InboundMappingType> inbound = attributeDefinitionType.getInbound();
            if (inbound != null && !inbound.isEmpty()) {
                for (InboundMappingType inboundMappingType : inbound) {
                    InboundMappingUseType use = inboundMappingType.getUse();
                    if (use != InboundMappingUseType.CORRELATION) {
                        return true;
                    }
                }
            }

            MappingType outbound = attributeDefinitionType.getOutbound();
            if (outbound != null) {
                return true;
            }
        }

        return false;
    }

    public static @Nullable String getTooltipKey(
            @NotNull ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType tile,
            @NotNull ResourceObjectTypeDefinitionType real) {
        return switch (tile) {
            case ACTIVATION -> isActivationEnabled(real)
                    ? null
                    : "ResourceObjectTypeWizardChoicePanel.activationLocked";
            case CREDENTIALS -> isCredentialsEnabled(real)
                    ? null
                    : "ResourceObjectTypeWizardChoicePanel.credentialsLocked";
            default -> null;
        };
    }

    public record BadgeSpec(String cssClass, @Nullable String iconCss, String labelKey) {
    }
}
