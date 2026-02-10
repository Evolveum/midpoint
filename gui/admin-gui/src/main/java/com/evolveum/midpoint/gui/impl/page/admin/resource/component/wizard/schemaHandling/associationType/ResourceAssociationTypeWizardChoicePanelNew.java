/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.gui.impl.component.tile.WizardAssociationTilePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

public abstract class ResourceAssociationTypeWizardChoicePanelNew
        extends ResourceWizardChoicePanel<ResourceAssociationTypeWizardChoicePanelNew.ResourceAssociationTypePreviewTileType> {

    private final WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper;

    public ResourceAssociationTypeWizardChoicePanelNew(
            String id,
            WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), ResourceAssociationTypePreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<ResourceAssociationTypePreviewTileType>> tileModel) {
        return new WizardAssociationTilePanel<>(id, tileModel) {

            private @NotNull Boolean getDescription() {
                return StringUtils.isNotEmpty(tileModel.getObject().getDescription());
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                Tile<ResourceAssociationTypePreviewTileType> tile = tileModel.getObject();
                onTileClick(tile.getValue(), target);
            }

            @Override
            protected IModel<Badge> getBadgeModel() {
                ResourceAssociationTypePreviewTileType tile = tileModel.getObject().getValue();
                State state = tile.computeState(getValueModel());
                if (state == State.PENDING) {
                    return () -> new Badge("badge bg-info", "fa fa-clock",
                            createStringResource("ResourceAssociationTypeWizardChoicePanel.pending").getString());
                }

                if (state == State.TEMPORARY_LOCKED) {
                    return Model.of();
                }
                return () -> new Badge("badge bg-success",
                        createStringResource("ResourceAssociationTypeWizardChoicePanel.ready").getString());
            }

            @Override
            protected boolean isLocked() {
                ResourceAssociationTypePreviewTileType tile = tileModel.getObject().getValue();
                State state = tile.computeState(getValueModel());
                return state == State.TEMPORARY_LOCKED;
            }

            @Override
            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(this::getDescription);
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-8 col-10 gap-3 m-auto"));
    }

    public enum State {
        NORMAL,
        PENDING,
        TEMPORARY_LOCKED
    }

    public enum ResourceAssociationTypePreviewTileType implements TileEnum {

        BASIC_ATTRIBUTES("fa fa-file-alt",
                "ResourceAssociationTypePreviewTileType.basicAttributes.description"),

        OBJECT_AND_SUBJECT("fa fa-arrows-left-right",
                "ResourceAssociationTypePreviewTileType.objectAndSubject.description"),

        MAPPINGS("fa fa-random",
                "ResourceAssociationTypePreviewTileType.mappings.description"),

        CORRELATION("fa fa-code-branch",
                "ResourceAssociationTypePreviewTileType.correlation.description"),

        SYNCHRONIZATION("fa fa-sync",
                "ResourceAssociationTypePreviewTileType.synchronization.description");

        private final String icon;
        private final String descriptionKey;

        ResourceAssociationTypePreviewTileType(String icon, String descriptionKey) {
            this.icon = icon;
            this.descriptionKey = descriptionKey;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public String getDescription() {
            return translate(descriptionKey);
        }

        public @NotNull State computeState(
                @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel) {

            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> wrapper = valueModel.getObject();
            ShadowAssociationTypeDefinitionType real = wrapper != null ? wrapper.getRealValue() : null;

            if (real == null) {
                return State.PENDING; // or TEMPORARY_LOCKED, depending on your UX
            }

            ShadowAssociationTypeSubjectDefinitionType subject = real.getSubject();
            List<ShadowAssociationTypeObjectDefinitionType> objects = real.getObject();

            return switch (this) {
                case BASIC_ATTRIBUTES -> State.NORMAL;

                case OBJECT_AND_SUBJECT -> (subject == null || objects == null || objects.isEmpty())
                        ? State.PENDING
                        : State.NORMAL;

                case MAPPINGS -> computeMappingsState(subject);
                case CORRELATION -> computeCorrelationState(subject);
                case SYNCHRONIZATION -> computeSyncState(subject);
            };
        }

        private static @NotNull State computeMappingsState(
                @Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
            if (subject == null) {
                return State.TEMPORARY_LOCKED;
            }
            ShadowAssociationDefinitionType association = subject.getAssociation();
            return hasInbound(association)
                    ? State.NORMAL
                    : State.PENDING;
        }

        private static boolean isCorrelationOrSyncLocked(ShadowAssociationTypeSubjectDefinitionType subject) {
            if (subject == null) {
                return true;
            }
            ShadowAssociationDefinitionType association = subject.getAssociation();
            return !hasInbound(association);
        }

        private static @NotNull State computeCorrelationState(
                @Nullable ShadowAssociationTypeSubjectDefinitionType subject) {

            if (isCorrelationOrSyncLocked(subject)) {
                return State.TEMPORARY_LOCKED;
            }

            AssociationSynchronizationExpressionEvaluatorType as = getAssociationSyncEvaluator(subject);
            if (hasCorrelation(as)) {
                return State.NORMAL;
            } else {
                return State.PENDING;
            }
        }

        private static @NotNull State computeSyncState(
                @Nullable ShadowAssociationTypeSubjectDefinitionType subject) {
            if (isCorrelationOrSyncLocked(subject)) {
                return State.TEMPORARY_LOCKED;
            }

            AssociationSynchronizationExpressionEvaluatorType as = getAssociationSyncEvaluator(subject);
            if (hasSynchronization(as)) {
                return State.NORMAL;
            } else {
                return State.PENDING;
            }
        }

        private static boolean hasSynchronization(@Nullable AssociationSynchronizationExpressionEvaluatorType as) {
            if (as == null) {
                return false;
            }

            ItemSynchronizationReactionsType synchronization = as.getSynchronization();
            return synchronization != null && synchronization.getReaction() != null && !synchronization.getReaction().isEmpty();
        }

        private static boolean hasCorrelation(@Nullable AssociationSynchronizationExpressionEvaluatorType as) {
            if (as == null) {
                return false;
            }

            CorrelationDefinitionType correlation = as.getCorrelation();
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

            // limitation: only first inbound expression is considered
            ShadowAssociationDefinitionType association = subject.getAssociation();
            InboundMappingType mapping = association.getInbound().get(0);
            ExpressionType expression = mapping.getExpression();

            if (expression == null) {
                return null;
            }

            for (Object ev : expression.getExpressionEvaluator()) {
                if (ev instanceof JAXBElement<?> je) {
                    Object value = je.getValue();
                    if (value instanceof AssociationSynchronizationExpressionEvaluatorType as) {
                        return as;
                    }
                } else if (ev instanceof AssociationSynchronizationExpressionEvaluatorType as) {
                    return as;
                }
            }
            return null;
        }

        private static boolean hasInbound(@Nullable ShadowAssociationDefinitionType association) {
            return association != null
                    && association.getInbound() != null
                    && !association.getInbound().isEmpty();
        }
    }

    @Override
    protected boolean addDefaultTile() {
        return false;
    }

    @Override
    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("ResourceAssociationTypeWizardChoicePanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> getValueModel() {
        return helper.getValueModel();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
            }
        };
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceAssociationTypeWizardChoicePanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceAssociationTypeWizardChoicePanel.text");
    }
}
