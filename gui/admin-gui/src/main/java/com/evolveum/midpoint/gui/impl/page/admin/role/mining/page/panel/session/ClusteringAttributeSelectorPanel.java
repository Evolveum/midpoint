/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.createClusteringAttributeChoiceSet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component.AttributeSettingPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisClusteringAttributeTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeSettingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class ClusteringAttributeSelectorPanel extends InputPanel {
    private static final String ID_MULTISELECT = "multiselect";
    private static final String ID_SELECT_MANUALLY = "selectManually";
    private static final String ID_CONTAINER = "container";

    protected List<ClusteringAttributeRuleType> objectToChooseFrom;
    protected IModel<List<ClusteringAttributeRuleType>> selectedObject = Model.ofList(new ArrayList<>());
    IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model;
    RoleAnalysisProcessModeType processModeType;

    public ClusteringAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model,
            @NotNull RoleAnalysisProcessModeType processModeType) {
        super(id);
        this.model = model;
        this.processModeType = processModeType;
        this.objectToChooseFrom = createClusteringAttributeChoiceSet(processModeType);
        initSelectedModel(model);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public boolean isPopupAllowed() {
        if (getPage().getPage() instanceof PageRoleAnalysisSession sessionPage) {
            return sessionPage.isShowByWizard();
        }
        return false;
    }

    private void initLayout() {
        Component container = getContainer();

        add(container);

        initSelectionFragment();
        AjaxLink<?> configureAttributes = new AjaxLink<>(ID_SELECT_MANUALLY) {

            @Override
            public void onClick(AjaxRequestTarget target) {

                if (isPopupAllowed()) {
                    AttributeSettingPopupPanel detailsPanel = new AttributeSettingPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                            Model.of("Configure attributes"), model);
                    getPageBase().showMainPopup(detailsPanel, target);
                } else {
                    Component attributeSettingPanel = getAttributeSettingPanel();
                    if (attributeSettingPanel.isVisible()) {
                        List<ClusteringAttributeRuleType> clusteringAttributeRule = model.getObject().getRealValue().getClusteringAttributeRule();
                        ClusteringAttributeSettingType realValue = model.getObject().getRealValue();
                        realValue.getClusteringAttributeRule().clear();
                        for (ClusteringAttributeRuleType clusteringAttributeRuleType : clusteringAttributeRule) {
                            realValue.getClusteringAttributeRule().add(clusteringAttributeRuleType.clone());
                        }
                    }

                    getAttributeSettingPanel().setVisible(!attributeSettingPanel.isVisible());
                    target.add(getAttributeSettingPanel().getParent());
                }
            }
        };
        add(configureAttributes);
    }

    @NotNull
    private Component getContainer() {
        Component container;
        if (isPopupAllowed()) {
            container = new WebMarkupContainer(ID_CONTAINER);
            container.setOutputMarkupId(true);
        } else {
            List<ClusteringAttributeRuleType> clusteringAttributeRule = new ArrayList<>(
                    model.getObject().getRealValue().getClusteringAttributeRule());
            RoleAnalysisClusteringAttributeTable clusteringAttributeTable = buildConfigureTablePanel(clusteringAttributeRule);
            clusteringAttributeTable.add(AttributeAppender.replace("class", "col-12 p-0"));
            container = clusteringAttributeTable;

            container.setOutputMarkupId(true);
            container.setVisible(false);
        }
        return container;
    }

    @NotNull
    private static RoleAnalysisClusteringAttributeTable buildConfigureTablePanel(
            List<ClusteringAttributeRuleType> clusteringAttributeRule) {
        ListModel<ClusteringAttributeRuleType> clusteringAttributeRuleModel = new ListModel<>(clusteringAttributeRule) {
            @Override
            public List<ClusteringAttributeRuleType> getObject() {
                return super.getObject();
            }

            @Override
            public void setObject(List<ClusteringAttributeRuleType> object) {
                super.setObject(object);
            }
        };

        RoleAnalysisClusteringAttributeTable clusteringAttributeTable = new RoleAnalysisClusteringAttributeTable(
                ID_CONTAINER, clusteringAttributeRuleModel, true) {

        };
        clusteringAttributeTable.setOutputMarkupId(true);
        return clusteringAttributeTable;
    }

    public Component getAttributeSettingPanel() {
        return get(getPageBase().createComponentPath(ID_CONTAINER));
    }

    private void initSelectionFragment() {
        IModel<Collection<ClusteringAttributeRuleType>> multiselectModel = buildMultiSelectModel();

        ChoiceProvider<ClusteringAttributeRuleType> choiceProvider = buildChoiceProvider();

        Select2MultiChoice<ClusteringAttributeRuleType> multiselect = new Select2MultiChoice<>(ID_MULTISELECT, multiselectModel,
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Collection<ClusteringAttributeRuleType> refs = multiselect.getModel().getObject();
                updateSelected(refs);
                getAttributeSettingPanel().replaceWith(getContainer().setOutputMarkupId(true));
                target.add(getAttributeSettingPanel().getParent());

            }
        });
        add(multiselect);

    }

    @NotNull
    private IModel<Collection<ClusteringAttributeRuleType>> buildMultiSelectModel() {
        return new IModel<>() {
            @Override
            public Collection<ClusteringAttributeRuleType> getObject() {
                return new ArrayList<>(getSelectedObject().getObject());
            }

            @Override
            public void setObject(Collection<ClusteringAttributeRuleType> object) {
                updateSelected(object);
            }
        };
    }

    private void initSelectedModel(@NotNull IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model) {
        ClusteringAttributeSettingType realValue = model.getObject().getRealValue();

        if (realValue == null) {
            realValue = new ClusteringAttributeSettingType();
            model.getObject().setRealValue(realValue);
        }

        selectedObject = new LoadableModel<>(false) {

            @Override
            protected List<ClusteringAttributeRuleType> load() {
                ClusteringAttributeSettingType realValue = getModel().getObject().getRealValue();
                return new ArrayList<>(realValue.getClusteringAttributeRule());
            }
        };
    }

    @NotNull
    private ChoiceProvider<ClusteringAttributeRuleType> buildChoiceProvider() {
        return new ChoiceProvider<>() {
            @Override
            public String getDisplayValue(ClusteringAttributeRuleType roleAnalysisAttributeDef) {
                return roleAnalysisAttributeDef.getAttributeIdentifier();
            }

            @Override
            public String getIdValue(ClusteringAttributeRuleType roleAnalysisAttributeDef) {
                return roleAnalysisAttributeDef.getAttributeIdentifier();
            }

            @Override
            public void query(String inputString, int i, Response<ClusteringAttributeRuleType> response) {
                if (inputString == null || inputString.isEmpty()) {
                    response.addAll(getObjectToChooseFrom());
                    return;
                }

                response.addAll(performSearch(inputString));
            }

            @Override
            public Collection<ClusteringAttributeRuleType> toChoices(Collection<String> collection) {
                Collection<ClusteringAttributeRuleType> choices = new ArrayList<>();

                List<ClusteringAttributeRuleType> objectToChooseFrom = getObjectToChooseFrom();
                objectToChooseFrom.forEach(def -> {
                    if (collection.contains(def.getAttributeIdentifier())) {
                        choices.add(def);
                    }
                });

                return choices;
            }
        };
    }

    public void updateSelected(@NotNull Collection<ClusteringAttributeRuleType> poiRefs) {

        IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> model = getModel();
        ClusteringAttributeSettingType realValue = model.getObject().getRealValue();

        if (poiRefs.isEmpty()) {
            getSelectedObject().setObject(new ArrayList<>());
            realValue.getClusteringAttributeRule().clear();
            return;
        }

        if (realValue == null) {
            realValue = new ClusteringAttributeSettingType();
            model.getObject().setRealValue(realValue);
        }

        List<ClusteringAttributeRuleType> clusteringAttributeRule = realValue.getClusteringAttributeRule();
        Set<String> identifiers = clusteringAttributeRule.stream().map(ClusteringAttributeRuleType::getAttributeIdentifier).collect(Collectors.toSet());

        int attributeRulesCount = poiRefs.size();
        double weightPerAttribute = 1.0 / attributeRulesCount;
        weightPerAttribute = roundUpTwoDecimal(weightPerAttribute);

        double appliedWeight = 0.0;
        int remainingCount = attributeRulesCount;
        realValue.getClusteringAttributeRule().clear();
        for (ClusteringAttributeRuleType poiRef : poiRefs) {
            remainingCount--;
            boolean isLast = (remainingCount == 0);

            if (identifiers.contains(poiRef.getAttributeIdentifier())) {
                appliedWeight = setupWeight(poiRef, realValue, isLast, appliedWeight, weightPerAttribute);
                identifiers.remove(poiRef.getAttributeIdentifier());
                continue;
            }

            appliedWeight = setupWeight(poiRef, realValue, isLast, appliedWeight, weightPerAttribute);

            clusteringAttributeRule.add(poiRef.clone());
            identifiers.remove(poiRef.getAttributeIdentifier());
        }

        if (!identifiers.isEmpty()) {
            clusteringAttributeRule.removeIf(rule -> identifiers.contains(rule.getAttributeIdentifier()));
        }

        getSelectedObject().setObject(new ArrayList<>(poiRefs));
    }

    private double roundUpTwoDecimal(double weightPerAttribute) {
        BigDecimal weightPerAttributeBD = new BigDecimal(weightPerAttribute);
        weightPerAttributeBD = weightPerAttributeBD.setScale(2, RoundingMode.HALF_UP);
        weightPerAttribute = weightPerAttributeBD.doubleValue();
        return weightPerAttribute;
    }

    private double setupWeight(
            @NotNull ClusteringAttributeRuleType poiRef,
            @NotNull ClusteringAttributeSettingType realValue,
            boolean isLast,
            double appliedWeight,
            double weightPerAttribute) {
        if (isLast) {
            double lastRuleWeight = 1.0 - appliedWeight;
            lastRuleWeight = roundUpTwoDecimal(lastRuleWeight);
            poiRef.setWeight(lastRuleWeight);
            realValue.getClusteringAttributeRule().add(poiRef);
        } else {
            poiRef.setWeight(weightPerAttribute);
            appliedWeight += weightPerAttribute;
            realValue.getClusteringAttributeRule().add(poiRef);
        }
        return appliedWeight;
    }

    private @NotNull List<ClusteringAttributeRuleType> performSearch(String term) {
        List<ClusteringAttributeRuleType> results = new ArrayList<>();

        for (ClusteringAttributeRuleType def : getObjectToChooseFrom()) {
            if (def.getAttributeIdentifier().toLowerCase().contains(term.toLowerCase())) {
                results.add(def);
            }
        }
        return results;
    }

    public List<ClusteringAttributeRuleType> getObjectToChooseFrom() {
        return objectToChooseFrom;
    }

    public IModel<List<ClusteringAttributeRuleType>> getSelectedObject() {
        return this.selectedObject;
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getFormC();
    }

    private Select2MultiChoice<?> getFormC() {
        return (Select2MultiChoice<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public IModel<PrismPropertyValueWrapper<ClusteringAttributeSettingType>> getModel() {
        return model;
    }
}
