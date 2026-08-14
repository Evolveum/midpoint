/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Tells that the mapping has a condition, so that it does not have to be looked up in the edit
 * dialog.
 */
public class MappingConditionPanel extends BasePanel<PrismContainerValueWrapper<MappingType>> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingConditionPanel.class);

    private static final String ID_CONDITION_BOX = "conditionBox";
    private static final String ID_CONDITION_STATUS = "conditionStatus";

    /**
     * Copy the drawer works with, kept until the user applies it or closes the drawer
     */
    private IModel<ExpressionType> editedCondition;

    public MappingConditionPanel(String id, IModel<PrismContainerValueWrapper<MappingType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupPlaceholderTag(true);
        add(new VisibleBehaviour(() -> hasCondition(getMapping())));

        add(createConditionBox());
    }

    private Component createConditionBox() {
        AjaxLink<Void> conditionBox = new AjaxLink<>(ID_CONDITION_BOX) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showConditionDrawer(target);
            }
        };

        conditionBox.add(createConditionStatusLabel());
        return conditionBox;
    }

    private Component createConditionStatusLabel() {
        return new Label(ID_CONDITION_STATUS, new StringResourceModel("MappingConditionPanel.conditionSet", this)
                .setParameters(getModel().flatMap(this::evaluatorNameOf)));
    }

    private void showConditionDrawer(AjaxRequestTarget target) {
        editedCondition = Model.of(createConditionCopy());

        DrawerModel drawerModel = new MappingConditionDrawerModel(this::findConditionWrapper, editedCondition) {

            @Override
            protected void storePerformed(ExpressionType condition, AjaxRequestTarget target) {
                if (storeCondition(condition, target)) {
                    closeDrawer(target);
                }
            }

            @Override
            protected void closePerformed(AjaxRequestTarget target) {
                closeDrawer(target);
            }
        };
        getPageBase().showDrawer(drawerModel, target);
    }

    private ExpressionType createConditionCopy() {
        MappingType mapping = getMapping();
        ExpressionType condition = mapping != null ? mapping.getCondition() : null;
        return condition != null ? condition.clone() : new ExpressionType();
    }

    private void closeDrawer(AjaxRequestTarget target) {
        editedCondition = null;
        getPageBase().hideDrawer(target);
        target.add(MappingConditionPanel.this);
    }

    private MappingType getMapping() {
        PrismContainerValueWrapper<MappingType> mappingValue = getModelObject();
        return mappingValue != null ? mappingValue.getRealValue() : null;
    }

    private static boolean hasCondition(MappingType mapping) {
        ExpressionType condition = mapping != null ? mapping.getCondition() : null;
        if (ExpressionUtil.isEmpty(condition)) {
            return false;
        }

        try {
            return hasSupportedEvaluatorContent(condition);
        } catch (SchemaException ex) {
            LOGGER.debug("Couldn't read the evaluator of the mapping condition: {}", ex.getMessage(), ex);
            return false;
        }
    }

    private static boolean hasSupportedEvaluatorContent(ExpressionType condition) throws SchemaException {
        ExpressionUtil.ExpressionEvaluatorType evaluatorType = ExpressionUtil.getExpressionType(condition);
        if (evaluatorType == null) {
            return false;
        }

        return switch (evaluatorType) {
            case SCRIPT -> ExpressionUtil.hasScriptCode(condition);
            case FILTER -> ExpressionUtil.hasFilterValue(condition);
            default -> false;
        };
    }

    private StringResourceModel evaluatorNameOf(PrismContainerValueWrapper<MappingType> mappingValue) {
        MappingType mapping = mappingValue.getRealValue();
        ExpressionType condition = mapping != null ? mapping.getCondition() : null;
        if (condition == null) {
            return null;
        }

        ExpressionUtil.ExpressionEvaluatorType evaluatorType = ExpressionUtil.getExpressionType(condition);
        return evaluatorType != null ? createStringResource(evaluatorType) : null;
    }

    private PrismPropertyWrapper<ExpressionType> findConditionWrapper() {
        PrismContainerValueWrapper<MappingType> mappingValue = getModelObject();
        if (mappingValue == null) {
            return null;
        }
        try {
            return mappingValue.findProperty(MappingType.F_CONDITION);
        } catch (SchemaException ex) {
            LOGGER.warn("Couldn't find the condition of the mapping {}: {}", getMapping(), ex.getMessage(), ex);
            return null;
        }
    }

    private boolean storeCondition(ExpressionType condition, AjaxRequestTarget target) {
        try {
            PrismPropertyWrapper<ExpressionType> wrapper = findConditionWrapper();
            PrismValueWrapper<ExpressionType> value = wrapper != null ? wrapper.getValue() : null;
            if (value == null) {
                LOGGER.error("Condition of the mapping {} not found, there is nothing to store into", getMapping());
                storeFailed(target);
                return false;
            }

            value.setRealValue(ExpressionUtil.hasEvaluatorContent(condition) ? condition : null);
            return true;
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't store the condition of the mapping {}: {}", getMapping(), ex.getMessage(), ex);
            storeFailed(target);
            return false;
        }
    }

    private void storeFailed(AjaxRequestTarget target) {
        getSession().error(getString("MappingConditionPanel.message.storeFailed"));
        target.add(getFeedbackPanel());
    }
}
