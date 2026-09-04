/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.range;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;

/**
 * Range of a mapping, telling which of the existing target values the mapping is in charge of.
 *
 * @author jjarabinec
 */
public class MappingRangePanel extends BasePanel<PrismContainerValueWrapper<MappingType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RANGE_CHOICE = "rangeChoice";
    private static final String ID_CONDITION = "condition";

    private MappingRangeModel rangeModel;

    private MappingRangeOption selectedOption;

    public MappingRangePanel(String id, IModel<PrismContainerValueWrapper<MappingType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        rangeModel = new MappingRangeModel(getModel());

        add(createRangeChoice());
        add(createConditionPanel());
    }

    private Component createRangeChoice() {
        IModel<List<MappingRangeOption>> options = () -> MappingRangeUtils.optionsFor(getModelObject());

        DropDownChoicePanel<MappingRangeOption> choice = new DropDownChoicePanel<>(
                ID_RANGE_CHOICE, createOptionModel(), options, new EnumChoiceRenderer<>(this), true);

        choice.setOutputMarkupId(true);
        choice.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                super.onUpdate(target);
                target.add(MappingRangePanel.this);
            }
        });

        return choice;
    }

    private Component createConditionPanel() {
        ExpressionPanel condition = new ExpressionPanel(ID_CONDITION, createConditionModel()) {

            @Override
            protected List<RecognizedEvaluator> getChoices() {
                return List.of(RecognizedEvaluator.SCRIPT, RecognizedEvaluator.FILTER);
            }
        };

        condition.setEvaluatorPanelExpanded(true);
        condition.add(new VisibleBehaviour(() -> MappingRangeOption.CONDITION == getSelectedOption()));
        return condition;
    }

    private IModel<MappingRangeOption> createOptionModel() {
        return new IModel<>() {

            @Override
            public MappingRangeOption getObject() {
                return getSelectedOption();
            }

            @Override
            public void setObject(MappingRangeOption option) {
                selectedOption = option;
                storeOption(option);
            }
        };
    }

    private MappingRangeOption getSelectedOption() {
        return selectedOption != null ? selectedOption : MappingRangeOption.of(rangeModel.getObject());
    }

    private void storeOption(MappingRangeOption option) {
        ValueSetDefinitionType updated = copyOfRange();

        if (MappingRangeOption.CONDITION == option) {
            updated.setPredefined(null);
        } else {
            updated.setCondition(null);
            updated.setPredefined(option != null ? option.getPredefined() : null);
        }

        rangeModel.setObject(updated);
    }

    private IModel<ExpressionType> createConditionModel() {
        return new IModel<>() {

            @Override
            public ExpressionType getObject() {
                ValueSetDefinitionType set = rangeModel.getObject();
                return set != null ? set.getCondition() : null;
            }

            @Override
            public void setObject(ExpressionType condition) {
                ValueSetDefinitionType updated = copyOfRange();
                updated.setCondition(ExpressionUtil.hasEvaluatorContent(condition) ? condition : null);
                rangeModel.setObject(updated);
            }
        };
    }

    private ValueSetDefinitionType copyOfRange() {
        ValueSetDefinitionType current = rangeModel.getObject();
        return current != null ? current.clone() : new ValueSetDefinitionType();
    }
}
