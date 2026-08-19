/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import java.util.List;

import jakarta.xml.bind.JAXBElement;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public enum ResourceGuideAssociationTypeTileState {

    NORMAL(null),
    CONFIGURED(new BadgeSpec(
            "badge text-bg-success opaque",
            "",
            "ResourceAssociationTypeWizardChoicePanel.ready")),
    RECOMMENDED(new BadgeSpec(
            "badge text-bg-primary opaque",
            "",
            "ResourceAssociationTypeWizardChoicePanel.pending")),
    TEMPORARY_LOCKED(null);

    private final @Nullable BadgeSpec spec;

    ResourceGuideAssociationTypeTileState(@Nullable BadgeSpec spec) {
        this.spec = spec;
    }

    public @NotNull IModel<Badge> badgeModel(@NotNull ResourceAssociationTypeWizardChoicePanel panel) {
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

    public static @NotNull ResourceGuideAssociationTypeTileState computeState(
            @NotNull ResourceAssociationTypeWizardChoicePanel.ResourceAssociationTypePreviewTileType tile,
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel) {

        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> wrapper = valueModel.getObject();
        ShadowAssociationTypeDefinitionType real = wrapper != null ? wrapper.getRealValue() : null;

        if (real == null) {
            return RECOMMENDED;
        }

        ShadowAssociationTypeSubjectDefinitionType subject = real.getSubject();
        List<ShadowAssociationTypeObjectDefinitionType> objects = real.getObject();

        return switch (tile) {
            case BASIC_ATTRIBUTES -> CONFIGURED;

            case OBJECT_AND_SUBJECT -> (subject == null || objects == null || objects.isEmpty())
                    ? RECOMMENDED
                    : CONFIGURED;

            case MAPPINGS -> computeMappingsState(subject);

            case CORRELATION -> computeCorrelationState(subject);

            case SYNCHRONIZATION -> computeSynchronizationState(subject);
        };
    }

    private static @NotNull ResourceGuideAssociationTypeTileState computeMappingsState(
            @Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
        if (subject == null) {
            return TEMPORARY_LOCKED;
        }

        return hasMapping(subject) ? CONFIGURED : RECOMMENDED;
    }

    private static @NotNull ResourceGuideAssociationTypeTileState computeCorrelationState(
            @Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
        if (isCorrelationOrSyncLocked(subject)) {
            return TEMPORARY_LOCKED;
        }

        AssociationSynchronizationExpressionEvaluatorType evaluator = getAssociationSyncEvaluator(subject);
        return hasCorrelation(evaluator) ? CONFIGURED : RECOMMENDED;
    }

    private static @NotNull ResourceGuideAssociationTypeTileState computeSynchronizationState(
            @Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
        if (isCorrelationOrSyncLocked(subject)) {
            return TEMPORARY_LOCKED;
        }

        AssociationSynchronizationExpressionEvaluatorType evaluator = getAssociationSyncEvaluator(subject);
        return hasSynchronization(evaluator) ? CONFIGURED : RECOMMENDED;
    }

    private static boolean isCorrelationOrSyncLocked(@Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
        if (subject == null) {
            return true;
        }

        return !hasInboundMapping(subject);
    }

    private static boolean hasMapping(@Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
        if (subject == null) {
            return false;
        }

        return hasInboundMapping(subject) || hasOutboundMapping(subject);
    }

    private static boolean hasInboundMapping(@NotNull ShadowAssociationTypeSubjectDefinitionType subject) {
        AssociationSynchronizationExpressionEvaluatorType syncEvaluator = getAssociationSyncEvaluator(subject);
        if (syncEvaluator != null) {
            return hasItems(syncEvaluator.getAttribute()) || hasItems(syncEvaluator.getObjectRef());
        }
        return false;
    }

    private static boolean hasOutboundMapping(@NotNull ShadowAssociationTypeSubjectDefinitionType subject) {
        AssociationConstructionExpressionEvaluatorType constructionEvaluator = getAssociationConstructionEvaluator(subject);
        if (constructionEvaluator != null) {
            return hasItems(constructionEvaluator.getAttribute()) || hasItems(constructionEvaluator.getObjectRef());
        }
        return false;
    }

    private static boolean hasItems(@Nullable List<?> items) {
        return items != null && !items.isEmpty();
    }

    private static boolean hasSynchronization(@Nullable AssociationSynchronizationExpressionEvaluatorType evaluator) {
        if (evaluator == null) {
            return false;
        }

        ItemSynchronizationReactionsType synchronization = evaluator.getSynchronization();
        return synchronization != null
                && synchronization.getReaction() != null
                && !synchronization.getReaction().isEmpty();
    }

    private static boolean hasCorrelation(@Nullable AssociationSynchronizationExpressionEvaluatorType evaluator) {
        if (evaluator == null) {
            return false;
        }

        CorrelationDefinitionType correlation = evaluator.getCorrelation();
        if (correlation == null) {
            return false;
        }

        CompositeCorrelatorType correlators = correlation.getCorrelators();
        return correlators != null
                && correlators.getItems() != null
                && !correlators.getItems().isEmpty();
    }

    private static @Nullable AssociationSynchronizationExpressionEvaluatorType getAssociationSyncEvaluator(
            @NotNull ShadowAssociationTypeSubjectDefinitionType subject) {
        ShadowAssociationDefinitionType association = subject.getAssociation();
        if (association == null || association.getInbound() == null || association.getInbound().isEmpty()) {
            return null;
        }

        // limitation: only first inbound expression is considered
        return findExpressionEvaluator(
                association.getInbound().get(0).getExpression(),
                AssociationSynchronizationExpressionEvaluatorType.class);
    }

    private static @Nullable AssociationConstructionExpressionEvaluatorType getAssociationConstructionEvaluator(
            @NotNull ShadowAssociationTypeSubjectDefinitionType subject) {
        ShadowAssociationDefinitionType association = subject.getAssociation();
        if (association == null || association.getOutbound() == null || association.getOutbound().isEmpty()) {
            return null;
        }

        // limitation: only first outbound expression is considered
        return findExpressionEvaluator(
                association.getOutbound().get(0).getExpression(),
                AssociationConstructionExpressionEvaluatorType.class);
    }

    private static <T> @Nullable T findExpressionEvaluator(
            @Nullable ExpressionType expression,
            @NotNull Class<T> evaluatorType) {

        if (expression == null || expression.getExpressionEvaluator() == null) {
            return null;
        }

        for (Object evaluator : expression.getExpressionEvaluator()) {
            if (evaluator instanceof JAXBElement<?> jaxb) {
                Object value = jaxb.getValue();
                if (evaluatorType.isInstance(value)) {
                    return evaluatorType.cast(value);
                }
            } else if (evaluatorType.isInstance(evaluator)) {
                return evaluatorType.cast(evaluator);
            }
        }

        return null;
    }

    public static @Nullable String getTooltipKey(
            @NotNull ResourceAssociationTypeWizardChoicePanel.ResourceAssociationTypePreviewTileType tile,
            @NotNull ShadowAssociationTypeDefinitionType real) {

        ShadowAssociationTypeSubjectDefinitionType subject = real.getSubject();

        return switch (tile) {
            case MAPPINGS -> subject == null
                    ? "ResourceAssociationTypeWizardChoicePanel.mappingsLocked"
                    : null;

            case CORRELATION, SYNCHRONIZATION -> isCorrelationOrSyncLocked(subject)
                    ? "ResourceAssociationTypeWizardChoicePanel.correlationSynchronizationLocked"
                    : null;

            default -> null;
        };
    }

    public record BadgeSpec(String cssClass, @Nullable String iconCss, String labelKey) {
    }
}
