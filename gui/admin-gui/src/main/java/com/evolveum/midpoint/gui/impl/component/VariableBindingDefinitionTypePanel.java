/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.LambdaModel;

import java.util.ArrayList;
import java.util.List;

public class VariableBindingDefinitionTypePanel extends BasePanel<VariableBindingDefinitionType> {

    private static final String ID_PATH_PANEL = "pathPanel";
    private static final String ID_RANGE = "range";
    private static final String ID_PREDEFINED_INPUT = "predefinedInput";
    private static final String ID_CONDITION_INPUT = "conditionInput";
    private final boolean showRangePanel;

    public VariableBindingDefinitionTypePanel(String id, IModel<VariableBindingDefinitionType> model) {
        this(id, model, false);
    }

    public VariableBindingDefinitionTypePanel(String id, IModel<VariableBindingDefinitionType> model, boolean showRangePanel) {
        super(id, model);
        this.showRangePanel = showRangePanel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(AttributeModifier.append("class", "d-flex flex-column"));

        ItemPathPanel pathPanel = new ItemPathPanel(ID_PATH_PANEL, createPathModel()) {

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                ItemPath newPath = getModelObject().toItemPath();
                ItemPathType newPathtype = null;
                if (newPath != null) {
                    newPathtype = new ItemPathType(newPath);
                }

                VariableBindingDefinitionType var = VariableBindingDefinitionTypePanel.this.getModelObject();
                if (var == null) {
                    var = new VariableBindingDefinitionType();
                    VariableBindingDefinitionTypePanel.this.getModel().setObject(var);
                }
                VariableBindingDefinitionTypePanel.this.getModelObject().setPath(newPathtype);
            }
        };
        pathPanel.setOutputMarkupId(true);
        add(pathPanel);

        createRangePanel();
    }

    private void createRangePanel() {
        WebMarkupContainer rangeContainer = new WebMarkupContainer(ID_RANGE);
        add(rangeContainer);
        rangeContainer.setOutputMarkupId(true);
        rangeContainer.add(new VisibleBehaviour(VariableBindingDefinitionTypePanel.this::isRangeVisible));

        createPredefinedInputPanel(rangeContainer);

        createConditionInputPanel(rangeContainer);
    }

    private void createConditionInputPanel(WebMarkupContainer rangeContainer) {
        IModel<ExpressionType> conditionModel = LambdaModel.of(
                VariableBindingDefinitionTypePanel.this::getConditionValue,
                VariableBindingDefinitionTypePanel.this::setConditionValue);
        ExpressionPanel conditionInput = new ExpressionPanel(ID_CONDITION_INPUT, conditionModel) {

            @Override
            protected List<RecognizedEvaluator> getChoices() {
                return new ArrayList<>(List.of(RecognizedEvaluator.SCRIPT, RecognizedEvaluator.FILTER));
            }

        };
        conditionInput.setOutputMarkupId(true);
        rangeContainer.add(conditionInput);
    }

    private void createPredefinedInputPanel(WebMarkupContainer rangeContainer) {
        IModel<ValueSetDefinitionPredefinedType> predefinedModel = LambdaModel.of(
                VariableBindingDefinitionTypePanel.this::getPredefinedValue,
                VariableBindingDefinitionTypePanel.this::setPredefinedValue);
        DropDownChoicePanel<ValueSetDefinitionPredefinedType> predefinedInput = WebComponentUtil.createEnumPanel(
                ValueSetDefinitionPredefinedType.class,
                ID_PREDEFINED_INPUT,
                predefinedModel,
                VariableBindingDefinitionTypePanel.this);
        predefinedInput.setOutputMarkupId(true);
        rangeContainer.add(predefinedInput);
    }

    private void setConditionValue(ExpressionType expressionType) {
        ValueSetDefinitionType set = getValueSetDefinitionType();
        set.setCondition(expressionType);
    }

    private ValueSetDefinitionType getValueSetDefinitionType() {
        VariableBindingDefinitionType var = VariableBindingDefinitionTypePanel.this.getModelObject();
        if (var == null) {
            var = new VariableBindingDefinitionType();
            VariableBindingDefinitionTypePanel.this.getModel().setObject(var);
        }

        ValueSetDefinitionType set = var.getSet();
        if (set == null) {
            set = new ValueSetDefinitionType();
        }
        return set;
    }

    private ExpressionType getConditionValue() {
        if (existValueSetDefinitionType()) {
            return VariableBindingDefinitionTypePanel.this.getModelObject().getSet().getCondition();
        }
        return null;
    }

    private boolean existValueSetDefinitionType() {
        VariableBindingDefinitionType var = VariableBindingDefinitionTypePanel.this.getModelObject();
        return var != null && var.getSet() != null;
    }

    private void setPredefinedValue(ValueSetDefinitionPredefinedType valueSetDefinitionPredefinedType) {
        ValueSetDefinitionType set = getValueSetDefinitionType();
        set.setPredefined(valueSetDefinitionPredefinedType);
    }

    private ValueSetDefinitionPredefinedType getPredefinedValue() {
        if (existValueSetDefinitionType()) {
            return VariableBindingDefinitionTypePanel.this.getModelObject().getSet().getPredefined();
        }
        return null;
    }

    private boolean isRangeVisible() {
        return showRangePanel;
    }

    private ItemPathDto createPathModel() {
        VariableBindingDefinitionType variable = getModelObject();
        if (variable == null) {
            return new ItemPathDto();
        }
        return new ItemPathDto(variable.getPath());
    }
}
