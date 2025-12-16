/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.synchronization;

import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.synchronization.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Confirmation dialog shown when synchronization configuration is missing for a resource object type.
 * Allows the user to automatically create default synchronization reactions or open the configuration wizard.
 */
public class ConfigureSynchronizationConfirmationPanel extends ConfirmationPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_DIRECTION_CHOICE_PANEL = "directionChoicePanel";

    private static final String ID_SPECIFICATIONS_CONTAINER = "specificationsContainer";
    private static final String ID_SOURCE_DIRECTION_SPECIFICATION_PANEL = "sourceDirectionSpecificationPanel";
    private static final String ID_TARGET_DIRECTION_SPECIFICATION_PANEL = "targetDirectionSpecificationPanel";

    private static final String ID_INFO_BOX = "infoBox";

    IModel<SourceSynchronizationAnswers> sourceAnswersModel =
            Model.of(new SourceSynchronizationAnswers(
                    UnmatchedSourceChoice.ADD_FOCUS,
                    DeletedSourceChoice.DO_NOTHING));

    IModel<TargetSynchronizationAnswers> targetAnswersModel =
            Model.of(new TargetSynchronizationAnswers(
                    UnmatchedTargetChoice.DO_NOTHING,
                    DeletedTargetChoice.REMOVE_BROKEN_LINK,
                    DisputedTargetChoice.CREATE_CORRELATION_CASE));

    IModel<DirectionChoicePanel.DirectionSelection> directionSelectionModel = Model.of(
            DirectionChoicePanel.DirectionSelection.NONE);

    public ConfigureSynchronizationConfirmationPanel(
            String id,
            IModel<String> message) {
        super(id, message);
    }

    @Override
    protected Component createYesButton() {
        AjaxButton generateButton = buildGenerateButton();
        generateButton.add(new EnableBehaviour(
                () -> !directionSelectionModel.getObject().equals(DirectionChoicePanel.DirectionSelection.NONE)
        ));

        generateButton.add(new TooltipBehavior());
        generateButton.add(new AttributeModifier("title", () -> {
            boolean enabled = !directionSelectionModel.getObject().equals(DirectionChoicePanel.DirectionSelection.NONE);
            if (!enabled) {
                return getPageBase().createStringResource("ConfigureSynchronizationConfirmationPanel.accept.tooltip")
                        .getString();
            }
            return null;
        }));

        return generateButton;
    }

    private @NotNull AjaxButton buildGenerateButton() {
        AjaxButton generateButton = new AjaxButton(ID_YES,
                createStringResource("ConfigureSynchronizationConfirmationPanel.accept")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                SmartIntegrationService smartIntegrationService = getPageBase().getSmartIntegrationService();
                SynchronizationReactionsType generatedReactions = directionSelectionModel.getObject().equals(DirectionChoicePanel.DirectionSelection.TARGET)
                        ? smartIntegrationService.buildTargetSynchronizationReactionsFromAnswers(targetAnswersModel.getObject())
                        : smartIntegrationService.buildSourceSynchronizationReactionsFromAnswers(sourceAnswersModel.getObject());

                performSynchronizationConfiguration(target, generatedReactions);
            }
        };
        generateButton.setOutputMarkupId(true);
        return generateButton;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        DirectionChoicePanel directionChoicePanel = new DirectionChoicePanel(ID_DIRECTION_CHOICE_PANEL, directionSelectionModel) {
            @Override
            protected void onDirectionChange(@NotNull AjaxRequestTarget target) {
                target.add(getSpecificationsContainer());
                target.add(ConfigureSynchronizationConfirmationPanel.this.getFooter().get(ID_YES));
            }
        };
        add(directionChoicePanel);

        WebMarkupContainer specificationsContainer =
                new WebMarkupContainer(ID_SPECIFICATIONS_CONTAINER);
        specificationsContainer.setOutputMarkupId(true);
        add(specificationsContainer);

        SourceDirectionSpecificationPanel sourceDirectionSpecificationPanel =
                new SourceDirectionSpecificationPanel(ID_SOURCE_DIRECTION_SPECIFICATION_PANEL, sourceAnswersModel);
        sourceDirectionSpecificationPanel.setOutputMarkupId(true);
        sourceDirectionSpecificationPanel.add(new VisibleBehaviour(
                () -> directionSelectionModel.getObject().equals(DirectionChoicePanel.DirectionSelection.SOURCE)));
        specificationsContainer.add(sourceDirectionSpecificationPanel);

        TargetDirectionSpecificationPanel targetDirectionSpecificationPanel =
                new TargetDirectionSpecificationPanel(ID_TARGET_DIRECTION_SPECIFICATION_PANEL, targetAnswersModel);
        targetDirectionSpecificationPanel.setOutputMarkupId(true);
        targetDirectionSpecificationPanel.add(new VisibleBehaviour(
                () -> directionSelectionModel.getObject().equals(DirectionChoicePanel.DirectionSelection.TARGET)));
        specificationsContainer.add(targetDirectionSpecificationPanel);

        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
        infoBox.add(new VisibleBehaviour(
                () -> !directionSelectionModel.getObject().equals(DirectionChoicePanel.DirectionSelection.NONE)));
        infoBox.setOutputMarkupId(true);
        specificationsContainer.add(infoBox);
    }

    private WebMarkupContainer getSpecificationsContainer() {
        return (WebMarkupContainer) get(ID_SPECIFICATIONS_CONTAINER);
    }

    protected void performSynchronizationConfiguration(AjaxRequestTarget target, SynchronizationReactionsType generatedReactions) {

    }

    @Override
    protected IModel<String> createYesLabel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.customize");
    }

    @Override
    protected IModel<String> createNoLabel() {
        return createStringResource("ConfigureSynchronizationConfirmationPanel.close");
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CorrelationWizardPanelWizardPanel.configureSynchronizationConfirmationPanel.title");
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return createStringResource("fa fa-gear");
    }

    @Override
    public int getWidth() {
        return 800;
    }

}
