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
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.provider.ClusteringAttributeSelectionProvider;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
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
import org.jetbrains.annotations.Nullable;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisClusteringAttributeTable;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class ClusteringAttributeSelectorPanel extends InputPanel {

    private static final String ID_MULTISELECT = "multiselect";
    private static final String ID_SELECT_MANUALLY = "selectManually";
    private static final String ID_CONTAINER = "container";

    private @NotNull IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> model;
    boolean isRoleMode = false;

    private boolean isSettingsPanelVisible;

    //not sure if it's not better to pass ClusteringAttributeRuleType directly
    public ClusteringAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> model,
            @NotNull RoleAnalysisProcessModeType processModeType) {
        super(id);

        this.model = model;

        if (processModeType == RoleAnalysisProcessModeType.ROLE) {
            isRoleMode = true;
        }
    }

    private @NotNull LoadableModel<Collection<ClusteringAttributeRuleType>> initSelectedModel() {
        return new LoadableModel<>(true) {

            @Override
            protected Collection<ClusteringAttributeRuleType> load() {

                PrismContainerWrapper<ClusteringAttributeRuleType> clusteringRulesContainers = getClusteringRulesContainers();
                return clusteringRulesContainers
                        .getValues()
                        .stream()
                        .filter(v -> v.getStatus() != ValueStatus.DELETED)
                        .map(v -> v.getRealValue())
                        .toList();
            }
        };
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
            public void onClick(@NotNull AjaxRequestTarget target) {
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
        RoleAnalysisClusteringAttributeTable clusteringAttributeTable = new RoleAnalysisClusteringAttributeTable(
                ID_CONTAINER, () -> getClusteringRulesContainers(), isRoleMode) {
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

    private void updateModelWithRules(@NotNull Collection<ClusteringAttributeRuleType> refs, @NotNull AjaxRequestTarget target) {
        PrismContainerWrapper<ClusteringAttributeRuleType> propertyWrapper = getClusteringRulesContainers();

        if (refs.isEmpty()) {
            updateModificationStatus(refs, propertyWrapper);
            return;
        }

        double weightPerAttribute = roundUpTwoDecimal(1.0 / refs.size());
        double appliedWeight = 0.0;
        int remainingCount = refs.size();

        for (ClusteringAttributeRuleType poiRef : refs) {
            boolean isLast = (--remainingCount == 0);
            appliedWeight = setupWeight(poiRef, isLast, appliedWeight, weightPerAttribute);

            if (isAlreadyPresent(poiRef, propertyWrapper)) {
                continue;
            }

            if (isRequiredModify(poiRef, propertyWrapper)) {
                modifyExistingRule(poiRef, propertyWrapper, target);
            } else {
                createNewValueWrapper(poiRef, propertyWrapper, target);
            }
        }

        updateModificationStatus(refs, propertyWrapper);
    }

    private void updateModificationStatus(Collection<ClusteringAttributeRuleType> refs,
            @NotNull PrismContainerWrapper<ClusteringAttributeRuleType> propertyWrapper) {
        List<PrismContainerValueWrapper<ClusteringAttributeRuleType>> toBeDeleted = new ArrayList<>();

        for (PrismContainerValueWrapper<ClusteringAttributeRuleType> value : propertyWrapper.getValues()) {
            if (!shouldValueExist(value, refs)) {
                if (value.getStatus() == ValueStatus.ADDED) {
                    toBeDeleted.add(value);
                } else {
                    value.setStatus(ValueStatus.DELETED);
                    value.setRealValue(null); //check later why is this needed to record delta
                }
            } else if (value.getStatus() != ValueStatus.ADDED) {
                value.setStatus(ValueStatus.NOT_CHANGED);
            }
        }

        //remove added values (ValueStatus.ADDED)
        toBeDeleted.forEach(v -> {
            propertyWrapper.getValues().remove(v);
            propertyWrapper.getItem().remove(v.getNewValue());
        });
    }

    private void createNewValueWrapper(@NotNull ClusteringAttributeRuleType rule,
            @NotNull PrismContainerWrapper<ClusteringAttributeRuleType> propertyWrapper,
            AjaxRequestTarget target) {
        PrismContainerValue<ClusteringAttributeRuleType> newValue = propertyWrapper.getItem().createNewValue();

        newValue.asContainerable()
                .path(rule.getPath())
                .similarity(rule.getSimilarity())
                .isMultiValue(rule.getIsMultiValue())
                .weight(rule.getWeight());

        WebPrismUtil.createNewValueWrapper(propertyWrapper, newValue, getPageBase(), target);
    }

    private void modifyExistingRule(
            @NotNull ClusteringAttributeRuleType rule, @NotNull PrismContainerWrapper<ClusteringAttributeRuleType> ruleWrapper,
            @NotNull AjaxRequestTarget target) {
        List<PrismContainerValueWrapper<ClusteringAttributeRuleType>> existingValues = ruleWrapper.getValues();

        PrismContainerValueWrapper<ClusteringAttributeRuleType> valueToModify = existingValues.stream()
                .filter(v -> pathsEqual(rule, v))
                .findFirst()
                .orElse(null);

        if (valueToModify != null) {
            if (valueToModify.getStatus() == ValueStatus.ADDED) {
                modifyClusteringRuleValues(rule, valueToModify, target, ValueStatus.ADDED);
            } else {
                modifyClusteringRuleValues(rule, valueToModify, target, ValueStatus.MODIFIED);
            }
        }
    }

    private void modifyClusteringRuleValues(
            @NotNull ClusteringAttributeRuleType selectedRules,
            @NotNull PrismContainerValueWrapper<ClusteringAttributeRuleType> valueToModify,
            @NotNull AjaxRequestTarget target,
            @NotNull ValueStatus status) {

        updateProperty(valueToModify, ClusteringAttributeRuleType.F_PATH, null, status);
        updateProperty(valueToModify, ClusteringAttributeRuleType.F_SIMILARITY, selectedRules.getSimilarity(), status);
        updateProperty(valueToModify, ClusteringAttributeRuleType.F_IS_MULTI_VALUE, selectedRules.getIsMultiValue(), status);
        updateProperty(valueToModify, ClusteringAttributeRuleType.F_WEIGHT, selectedRules.getWeight(), status);

        valueToModify.setStatus(status);
        target.add(ClusteringAttributeSelectorPanel.this);
    }

    private static void updateProperty(
            @NotNull PrismContainerValueWrapper<ClusteringAttributeRuleType> valueToModify,
            @NotNull ItemPath propertyName,
            @Nullable Object newValue,
            @NotNull ValueStatus status) {

        try {
            PrismPropertyValueWrapper<Object> property = valueToModify.findProperty(propertyName).getValue();
            if (newValue != null) {
                property.setRealValue(newValue);
            }
            property.setStatus(status);
        } catch (SchemaException e) {
            throw new IllegalStateException("Cannot update property " + propertyName + " in " + valueToModify, e);
        }
    }

    private boolean shouldValueExist(
            @NotNull PrismContainerValueWrapper<ClusteringAttributeRuleType> value,
            @NotNull Collection<ClusteringAttributeRuleType> selectedRules) {
        return selectedRules.stream().anyMatch(r -> containersEqual(r, value.getRealValue()));
    }

    private boolean isAlreadyPresent(
            @NotNull ClusteringAttributeRuleType selectedRules,
            @NotNull PrismContainerWrapper<ClusteringAttributeRuleType> values) {
        return values.getValues().stream().anyMatch(container -> containersEqual(selectedRules, container.getRealValue()));
    }

    private boolean isRequiredModify(
            @NotNull ClusteringAttributeRuleType selectedRules,
            @NotNull PrismContainerWrapper<ClusteringAttributeRuleType> propertyWrapper) {
        return propertyWrapper.getValues().stream().anyMatch(r -> pathsEqual(selectedRules, r)
                && !containersEqual(selectedRules, r.getRealValue()));
    }

    private double roundUpTwoDecimal(double weightPerAttribute) {
        BigDecimal weightPerAttributeBD = BigDecimal.valueOf(weightPerAttribute);
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
        return getFormComponent();
    }

    private Select2MultiChoice<?> getFormComponent() {
        return (Select2MultiChoice<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public @NotNull IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> getClusteringSettingModel() {
        return model;
    }

    public boolean isEditable() {
        return true;
    }

    private static boolean pathsEqual(
            @NotNull ClusteringAttributeRuleType selectedRules,
            @NotNull PrismContainerValueWrapper<ClusteringAttributeRuleType> ruleWrapper) {
        return ruleWrapper.getRealValue().getPath().equivalent(selectedRules.getPath());
    }

    private boolean containersEqual(
            @NotNull ClusteringAttributeRuleType value,
            @NotNull ClusteringAttributeRuleType rule) {
        return value.getPath().equivalent(rule.getPath()) &&
                Objects.equals(value.getSimilarity(), rule.getSimilarity()) &&
                Objects.equals(value.getIsMultiValue(), rule.getIsMultiValue()) &&
                Objects.equals(value.getWeight(), rule.getWeight());
    }

    private PrismContainerWrapper<ClusteringAttributeRuleType> getClusteringRulesContainers() {
        PrismContainerWrapper<ClusteringAttributeRuleType> property;
        try {
            property = getClusteringSettingModel().getObject()
                    .findContainer(ClusteringAttributeSettingType.F_CLUSTERING_ATTRIBUTE_RULE);
        } catch (SchemaException e) {
            throw new SystemException("Cannot find clustering attribute rule containers", e);
        }

        return property;
    }
}
