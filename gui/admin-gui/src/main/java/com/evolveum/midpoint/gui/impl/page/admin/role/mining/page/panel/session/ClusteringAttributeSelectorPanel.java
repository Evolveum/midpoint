/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider.ClusteringAttributeSelectionProvider;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisClusteringAttributeTable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;

public class ClusteringAttributeSelectorPanel extends InputPanel {
    private static final String ID_MULTISELECT = "multiselect";
    private static final String ID_SELECT_MANUALLY = "selectManually";
    private static final String ID_CONTAINER = "container";

    private IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> model;
    boolean isRoleMode = false;

    private boolean isSettingsPanelVisible;

    public ClusteringAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> model,
            @NotNull RoleAnalysisProcessModeType processModeType) {
        super(id);
        this.model = model;

        if (processModeType == RoleAnalysisProcessModeType.ROLE) {
            isRoleMode = true;
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Component container = getClusteringAttributeSettingsPanel();
        add(container);

        initSelectionFragment();
        AjaxLink<?> configureAttributes = new AjaxLink<>(ID_SELECT_MANUALLY) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                isSettingsPanelVisible = !isSettingsPanelVisible;
                target.add(ClusteringAttributeSelectorPanel.this);
            }

        };
        add(configureAttributes);
    }

    @NotNull
    private Component getClusteringAttributeSettingsPanel() {
        RoleAnalysisClusteringAttributeTable clusteringAttributeTable = buildConfigureTablePanel();
        clusteringAttributeTable.add(AttributeModifier.replace("class", "col-12 p-0"));

        clusteringAttributeTable.setOutputMarkupId(true);
        clusteringAttributeTable.add(new VisibleBehaviour(() -> isSettingsPanelVisible));
        return clusteringAttributeTable;
    }

    @NotNull
    private RoleAnalysisClusteringAttributeTable buildConfigureTablePanel() {
        PrismContainerWrapperModel<ClusteringAttributeSettingType, ClusteringAttributeRuleType> rulesModel = PrismContainerWrapperModel.fromContainerWrapper(model, ClusteringAttributeSettingType.F_CLUSTERING_ATTRIBUTE_RULE);

        RoleAnalysisClusteringAttributeTable clusteringAttributeTable = new RoleAnalysisClusteringAttributeTable(
                ID_CONTAINER, rulesModel, isRoleMode) {
            @Override
            public boolean isEditable() {
                return ClusteringAttributeSelectorPanel.this.isEditable();
            }
        };
        clusteringAttributeTable.setOutputMarkupId(true);
        return clusteringAttributeTable;
    }

    @NotNull
    private ChoiceProvider<ClusteringAttributeRuleType> buildChoiceProvider() {
        if (isRoleMode) {
            return new ClusteringAttributeSelectionProvider(RoleType.class, getPageBase());
        }
        return new ClusteringAttributeSelectionProvider(UserType.class, getPageBase());
    }

    private void initSelectionFragment() {
        ChoiceProvider<ClusteringAttributeRuleType> choiceProvider = buildChoiceProvider();

        Select2MultiChoice<ClusteringAttributeRuleType> multiselect = new Select2MultiChoice<>(ID_MULTISELECT,
                initSelectedModel(),
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateModelWithRules(multiselect.getModel().getObject(), target);
                target.add(ClusteringAttributeSelectorPanel.this);
            }
        });
        multiselect.add(new EnableBehaviour(this::isEditable));
        add(multiselect);

    }

    private void updateModelWithRules(Collection<ClusteringAttributeRuleType> refs, AjaxRequestTarget target) {
        int attributeRulesCount = refs.size();
        if (attributeRulesCount == 0) {
            return;
        }
        double weightPerAttribute = 1.0 / attributeRulesCount;
        weightPerAttribute = roundUpTwoDecimal(weightPerAttribute);

        double appliedWeight = 0.0;
        int remainingCount = attributeRulesCount;

        try {
            PrismContainerWrapper<ClusteringAttributeSettingType> settingType = model.getObject();
            PrismContainerWrapper<ClusteringAttributeRuleType> rules = settingType.findContainer(ClusteringAttributeSettingType.F_CLUSTERING_ATTRIBUTE_RULE);
            rules.getValues().clear();

            for (ClusteringAttributeRuleType poiRef : refs) {

                remainingCount--;
                boolean isLast = (remainingCount == 0);

                appliedWeight = setupWeight(poiRef, isLast, appliedWeight, weightPerAttribute);

                PrismContainerValue<ClusteringAttributeRuleType> newRule = rules.getItem().createNewValue();
                newRule.asContainerable()
                        .path(poiRef.getPath())
                        .similarity(poiRef.getSimilarity())
                        .isMultiValue(poiRef.getIsMultiValue())
                        .weight(poiRef.getWeight());

                WebPrismUtil.createNewValueWrapper(rules, newRule, getPageBase(), target);
            }
        } catch (SchemaException e) {
            LOGGER.error("Cannot update model with rules: {}", e.getMessage(), e);
        }
    }

    private LoadableModel<Collection<ClusteringAttributeRuleType>> initSelectedModel() {
        return new LoadableModel<>(false) {

            @Override
            protected Collection<ClusteringAttributeRuleType> load() {

                PrismContainerWrapper<ClusteringAttributeRuleType> rules = null;
                try {
                    rules = model.getObject().findContainer(ClusteringAttributeSettingType.F_CLUSTERING_ATTRIBUTE_RULE);
                } catch (SchemaException e) {
                    //TODO handle
                }
                if (rules == null) {
                    return new ArrayList<>();
                }
                return rules.getValues()
                        .stream()
                        .map(PrismContainerValueWrapper::getRealValue)
                        .toList();
            }

        };
    }

    private double roundUpTwoDecimal(double weightPerAttribute) {
        BigDecimal weightPerAttributeBD = new BigDecimal(weightPerAttribute);
        weightPerAttributeBD = weightPerAttributeBD.setScale(2, RoundingMode.HALF_UP);
        weightPerAttribute = weightPerAttributeBD.doubleValue();
        return weightPerAttribute;
    }

    private double setupWeight(
            @NotNull ClusteringAttributeRuleType poiRef,
            boolean isLast,
            double appliedWeight,
            double weightPerAttribute) {
        if (isLast) {
            double lastRuleWeight = 1.0 - appliedWeight;
            lastRuleWeight = roundUpTwoDecimal(lastRuleWeight);
            poiRef.setWeight(lastRuleWeight);
        } else {
            poiRef.setWeight(weightPerAttribute);
            appliedWeight += weightPerAttribute;
        }
        return appliedWeight;
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getFormC();
    }

    private Select2MultiChoice<?> getFormC() {
        return (Select2MultiChoice<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> getModel() {
        return model;
    }

    public boolean isEditable() {
        return true;
    }
}
