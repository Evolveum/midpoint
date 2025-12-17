/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.correaltion;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationItemRulePanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.*;

public class CorrelationCandidatePanel extends BasePanel<ProcessedObject<?>> {

    private static final String ID_CANDIDATE_PANEL_CONTAINER = "candidatePanelContainer";
    private static final String ID_CANDIDATE_HEADER_TITLE = "candidateHeaderTitle";
    private static final String ID_CANDIDATE_HEADER_BUTTON = "candidateHeaderButton";

    private static final String ID_CANDIDATE_LIST_VIEW = "candidateListView";
    private static final String ID_CANDIDATE_NAME = "candidateName";
    private static final String ID_CANDIDATE_IDENTIFIER = "candidateIdentifier";
    private static final String ID_CANDIDATE_VIEW_LINK = "candidateViewLink";

    private static final String ID_RULE_PANEL_CONTAINER = "rulePanelContainer";
    private static final String ID_RULE_HEADER_TITLE = "ruleHeaderTitle";

    private static final String ID_RULE_LIST_VIEW = "ruleListView";
    private static final String ID_RULE_NAME = "ruleName";
    private static final String ID_RULE_VIEW_LINK = "ruleViewLink";

    record CorrelationRuleDetails(String title, Object value, boolean isHeader) implements Serializable {
    }

    List<CorrelationRuleDetails> correlationRuleDetailsList = new ArrayList<>();
    IModel<SimulationResultType> simulationResultModel;
    IModel<CorrelationDefinitionType> correlationDefinitionModel;
    Map<ItemPath, ItemPath> shadowCorrelationPathMap;

    public CorrelationCandidatePanel(String id,
            IModel<ProcessedObject<?>> model,
            IModel<SimulationResultType> simulationResulModel,
            IModel<CorrelationDefinitionType> correlationDefinitionModel,
            Map<ItemPath, ItemPath> shadowCorrelationPathMap) {
        super(id, model);
        this.simulationResultModel = simulationResulModel;
        this.correlationDefinitionModel = correlationDefinitionModel;
        this.shadowCorrelationPathMap = shadowCorrelationPathMap;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        loadModel();
        initLayout();
    }

    private void initLayout() {
        initCandidateListView();
        initCorrelationRuleListView();
    }

    /**
     * Loads correlation rule details based on the current simulation result and processed shadow.
     */
    private void loadModel() {
        CorrelationDefinitionType correlationDefinition = correlationDefinitionModel.getObject();

        if (correlationDefinition == null) {
            return;
        }

        ShadowType processedShadow = (ShadowType) getModelObject().getBefore();
        PrismObject<ShadowType> shadowPrismObject = processedShadow.asPrismObject();

        CompositeCorrelatorType correlators = correlationDefinition.getCorrelators();
        if (correlators == null || correlators.getItems() == null) {
            return;
        }

        correlators.getItems().forEach(item -> {
            correlationRuleDetailsList.add(
                    new CorrelationRuleDetails(item.getName(), item, true)
            );

            for (CorrelationItemType correlationItem : item.getItem()) {
                ItemPathType itemPath = correlationItem.getRef();
                String displayName = CorrelationUtil.getItemDisplayName(itemPath, UserType.class);

                Object realValue = "N/A";
                ItemPath shadowPath = shadowCorrelationPathMap.get(itemPath.getItemPath());
                if (shadowPath != null) {
                    realValue = getPropertyRealValue(shadowPrismObject,
                            ItemPath.create(ShadowType.F_ATTRIBUTES.getLocalPart(), shadowPath));
                }
                correlationRuleDetailsList.add(
                        new CorrelationRuleDetails(displayName, realValue, false)
                );
            }
        });
    }

    /**
     * Retrieves the real value of a property using the given item path.
     */
    private Object getPropertyRealValue(PrismObject<ShadowType> shadow, ItemPath itemPath) {
        if (shadow == null || itemPath == null) {
            return "N/A";
        }

        PrismProperty<Object> property = shadow.findProperty(itemPath);
        if (property == null || property.getRealValue() == null) {
            return "N/A";
        }

        return property.getRealValue();
    }

    private void initCorrelationRuleListView() {

        WebMarkupContainer container = new WebMarkupContainer(ID_RULE_PANEL_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        container.add(new Label(
                ID_RULE_HEADER_TITLE,
                createStringResource("CorrelationCandidatePanel.correlationRules.title")));

        ListView<CorrelationRuleDetails> ruleListView =
                new ListView<>(ID_RULE_LIST_VIEW, Model.ofList(correlationRuleDetailsList)) {

                    @Override
                    protected void populateItem(@NotNull ListItem<CorrelationRuleDetails> item) {
                        CorrelationRuleDetails details = item.getModelObject();

                        if (details.isHeader()) {
                            item.add(createLabel(ID_RULE_NAME, details.title(), "font-weight-semibold"));
                            item.add(createViewRuleButton(details));
                        } else {
                            item.add(createLabel(ID_RULE_NAME, details.title(), "text-muted"));
                            item.add(createLabel(ID_RULE_VIEW_LINK,
                                    String.valueOf(details.value()), "text-muted"));

                            item.add(AttributeModifier.append("class", "pt-1"));
                        }
                    }
                };

        container.add(ruleListView);
    }

    private @NotNull Label createLabel(String id, String text, String cssClass) {
        Label label = new Label(id, Model.of(text));
        if (cssClass != null && !cssClass.isEmpty()) {
            label.add(AttributeModifier.append("class", cssClass));
        }
        return label;
    }

    private @NotNull AjaxIconButton createViewRuleButton(CorrelationRuleDetails details) {
        final AjaxIconButton viewButton = new AjaxIconButton(
                ID_RULE_VIEW_LINK,
                Model.of(""),
                createStringResource("CorrelationCandidatePanel.link.viewRule")) {

            @Override
            public void onClick(AjaxRequestTarget target) {

                if (!(details.value() instanceof ItemsSubCorrelatorType subCorrelator)) {
                    return;
                }

                final var resourceObjectTypeDefinition =
                        findResourceObjectTypeDefinitionType(
                                getPageBase(), getSimulationResultModel().getObject());

                final PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> definitionWrapper =
                        WebPrismUtil.createContainerValueWrapper(getPageBase(), resourceObjectTypeDefinition);

                final var correlatorWrapper =
                        WebPrismUtil.createContainerValueWrapper(getPageBase(), subCorrelator);

                if (definitionWrapper == null || correlatorWrapper == null) {
                    return;
                }

                WebPrismUtil.setReadOnlyRecursively(correlatorWrapper);
                WebPrismUtil.setReadOnlyRecursively(definitionWrapper);

                final CorrelationItemRulePanel rulePanel = new CorrelationItemRulePanel(
                        getPageBase().getMainPopupBodyId(),
                        () -> correlatorWrapper,
                        () -> definitionWrapper) {

                    @Override
                    protected boolean isReadOnly() {
                        return true;
                    }
                };

                getPageBase().showMainPopup(rulePanel, target);
            }
        };

        viewButton.setOutputMarkupId(true);
        viewButton.showTitleAsLabel(true);
        return viewButton;
    }

    private void initCandidateListView() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CANDIDATE_PANEL_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        var candidateModel = getCorrelationCandidateModel(getModelObject());

        container.add(new Label(ID_CANDIDATE_HEADER_TITLE,
                createStringResource("CorrelationCandidatePanel.count.title", candidateModel.getObject().size())));

        AjaxIconButton manualCorrelationButton = new AjaxIconButton(ID_CANDIDATE_HEADER_BUTTON,
                Model.of("fa fa-link"),
                createStringResource("CorrelationCandidatePanel.button.manualCorrelation")) {
            @Override
            public void onClick(AjaxRequestTarget target) {

            }
        };
        manualCorrelationButton.setOutputMarkupId(true);
        manualCorrelationButton.showTitleAsLabel(true);
        container.add(manualCorrelationButton);

        ProcessedObject<?> modelObject = getModelObject();
        ObjectDelta<?> delta = modelObject.getDelta();
        List<String> correlatedOwnersOid = findCorrelatedOwners(delta);

        ListView<ResourceObjectOwnerOptionType> listView =
                new ListView<>(ID_CANDIDATE_LIST_VIEW, candidateModel) {

                    @Override
                    protected void populateItem(@NotNull ListItem<ResourceObjectOwnerOptionType> item) {
                        WebMarkupContainer candidateRow = new WebMarkupContainer("candidateRow");
                        candidateRow.setOutputMarkupId(true);
                        item.add(candidateRow);

                        ResourceObjectOwnerOptionType option = item.getModelObject();

                        ObjectReferenceType ref = option.getCandidateOwnerRef();
                        String candidateOid = ref.getOid();

                        String displayName = WebModelServiceUtils.resolveReferenceName(ref, getPageBase());

                        if (correlatedOwnersOid.contains(candidateOid)) {
                            candidateRow.add(AttributeModifier.append("class", "bg-success-light"));
                            displayName += " (Correlated)";
                        }

                        String identifier =
                                option.getIdentifier() != null ?
                                        option.getIdentifier() : "";

                        candidateRow.add(new Label(ID_CANDIDATE_NAME, Model.of(displayName)));
                        candidateRow.add(new Label(ID_CANDIDATE_IDENTIFIER, Model.of(identifier)));

                        AjaxIconButton viewLink = new AjaxIconButton(ID_CANDIDATE_VIEW_LINK,
                                Model.of("fa fa-eye"),
                                createStringResource("CorrelationCandidatePanel.link.viewDetails")) {
                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                DetailsPageUtil.dispatchToObjectDetailsPage(ref, getPageBase(), false);
                            }
                        };
                        viewLink.setOutputMarkupId(true);
                        viewLink.showTitleAsLabel(true);
                        candidateRow.add(viewLink);
                        item.add(candidateRow);
                    }
                };

        container.add(listView);
    }

    private IModel<SimulationResultType> getSimulationResultModel() {
        return simulationResultModel;
    }

}
