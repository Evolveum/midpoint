/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisMatchingRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis;

public class AttributeIdentifierDropDownPanel extends InputPanel {
    private static final String ID_DROP_DOWN = "dropDown";
    private final ItemRealValueModel<String> model;
    RoleAnalysisAttributeDef selectedAttribute;

    public AttributeIdentifierDropDownPanel(String id,
            ItemRealValueModel<String> realValueModel,
            RoleAnalysisProcessModeType processMode,
            PrismContainerValueWrapper<?> container) {
        super(id);

        this.model = realValueModel;

        List<RoleAnalysisAttributeDef> attributes;
        attributes = initialDataPreparation(realValueModel, processMode);
        RoleAnalysisMatchingRuleType matchingRule = null;
        if (container.getRealValue() instanceof RoleAnalysisMatchingRuleType) {
            matchingRule = (RoleAnalysisMatchingRuleType) container.getRealValue();
        }
        initDropDown(attributes, matchingRule);

    }

    private void initDropDown(
            @NotNull List<RoleAnalysisAttributeDef> attributes,
            @Nullable RoleAnalysisMatchingRuleType container) {
        IModel<RoleAnalysisAttributeDef> selectedModeModel = Model.of(selectedAttribute);

        ChoiceRenderer<RoleAnalysisSortMode> renderer = new ChoiceRenderer<>("displayValue");

        DropDownChoice<RoleAnalysisAttributeDef> dropDown = new DropDownChoice(
                ID_DROP_DOWN, selectedModeModel,
                attributes, renderer);

        dropDown.setOutputMarkupId(true);
        dropDown.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                selectedAttribute = selectedModeModel.getObject();
                model.setObject(selectedAttribute.getDisplayValue());
                boolean isContainer = selectedAttribute.isContainer();

                if (container != null) {
                    container.getMatchRule().isMultiValue(selectedAttribute.isContainer());
                    if (isContainer) {
                        container.getMatchRule().setMatchSimilarity(100.0);
                    }
                    target.add(getMatchRuleContainer());
                }
            }
        });
        dropDown.setOutputMarkupId(true);
        add(dropDown);
    }

    @NotNull
    private List<RoleAnalysisAttributeDef> initialDataPreparation(
            @NotNull ItemRealValueModel<String> realValueModel,
            @NotNull RoleAnalysisProcessModeType processMode) {
        List<RoleAnalysisAttributeDef> attributes;
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            attributes = getAttributesForRoleAnalysis();
        } else {
            attributes = getAttributesForUserAnalysis();
        }

        if (realValueModel.getObject() != null) {
            for (RoleAnalysisAttributeDef attribute : attributes) {
                if (attribute.getDisplayValue().equals(realValueModel.getObject())) {
                    selectedAttribute = attribute;
                }
            }
        }
        return attributes;
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_DROP_DOWN);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private WebMarkupContainer getMatchRuleContainer() {
        return (WebMarkupContainer) getBaseFormComponent().getPage().visitChildren(WebMarkupContainer.class, (component, visit) -> {
            if (component.getId().equals("match_rule_container")) {
                visit.stop(component);
            }
        });
    }
}
