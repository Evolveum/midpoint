/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;

import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

public class GenerateExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(GenerateExpressionPanel.class);

    private static final String ID_MODE = "mode";
    private static final String ID_VALUE_POLICY = "valuePolicy";

    public GenerateExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
        GenerateExpressionWrapper wrapper = getEvaluatorValue();
        if (wrapper == null || wrapper.isEmpty()) {
            updateEvaluatorValue((GenerateExpressionEvaluatorModeType) null);
        }
    }

    @Override
    protected void initLayout(MarkupContainer parent) {
        DropDownChoicePanel<GenerateExpressionEvaluatorModeType> modePanel =
                WebComponentUtil.createEnumPanel(
                        GenerateExpressionEvaluatorModeType.class,
                        ID_MODE,
                        Model.of(getEvaluatorValue().mode),
                        GenerateExpressionPanel.this);
        modePanel.setOutputMarkupId(true);
        modePanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                GenerateExpressionEvaluatorModeType updatedValue = modePanel.getBaseFormComponent().getConvertedInput();
                updateEvaluatorValue(updatedValue);
                target.add(getFeedback());
            }
        });
        parent.add(modePanel);

        ValueChoosePanel<ObjectReferenceType> valuePolicyPanel =
                new ValueChoosePanel<>(ID_VALUE_POLICY, Model.of(getEvaluatorValue().valuePolicyRef)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
                super.choosePerformed(target, object);
                getBaseFormComponent().validate();
                updateEvaluatorValue(
                        new ObjectReferenceType()
                                .oid(object.getOid())
                                .type(ValuePolicyType.COMPLEX_TYPE));
                target.add(getFeedback());
            }

            @Override
            public List<QName> getSupportedTypes() {
                return List.of(ValuePolicyType.COMPLEX_TYPE);
            }

            @Override
            protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
                return (Class<O>) ValuePolicyType.class;
            }
        };
        valuePolicyPanel.setOutputMarkupId(true);
        parent.add(valuePolicyPanel);
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("GenerateExpressionPanel.label");
    }

    //don't remove it, used by class and method name
    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        GenerateExpressionEvaluatorType evaluator = getEvaluator(expression, pageBase);
        if (evaluator == null) {
            return null;
        }
        boolean isModelNotEmpty = evaluator.getMode() != null;
        boolean isValuePolicyNotEmpty = evaluator.getValuePolicyRef() != null;
        String translatedMode = null;
        String translatedValuePolicyRef = null;

        if (isModelNotEmpty) {
            translatedMode = pageBase.getString(evaluator.getMode());
        }

        if (isValuePolicyNotEmpty) {
            translatedValuePolicyRef = WebComponentUtil.getReferencedObjectDisplayNameAndName(
                    evaluator.getValuePolicyRef(), true, pageBase);
        }

        return pageBase.getString(
                "GenerateExpressionPanel.mode." + isModelNotEmpty + ".valuePolicy." + isValuePolicyNotEmpty,
                translatedMode,
                translatedValuePolicyRef);
    }

    protected void updateEvaluatorValue(GenerateExpressionEvaluatorModeType mode) {
        try {
            GenerateExpressionEvaluatorType evaluator = getEvaluatorValue().mode(mode).toEvaluator();
            ExpressionUtil.updateGenerateExpressionValue(getModelObject(), evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update generate expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update generate expression values: " + ex.getLocalizedMessage());
        }
    }

    protected void updateEvaluatorValue(ObjectReferenceType valuePolicy) {
        try {
            GenerateExpressionEvaluatorType evaluator = getEvaluatorValue().valuePolicyRef(valuePolicy).toEvaluator();
            ExpressionUtil.updateGenerateExpressionValue(getModelObject(), evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update generate expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update generate expression values: " + ex.getLocalizedMessage());
        }
    }

    private GenerateExpressionWrapper getEvaluatorValue() {
        try {
            GenerateExpressionEvaluatorType evaluator = ExpressionUtil.getGenerateExpressionValue(getModelObject());
            if (evaluator == null) {
                return new GenerateExpressionWrapper();
            }
            return new GenerateExpressionWrapper(evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get script expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't get script expression value: " + ex.getLocalizedMessage());
        }
        return null;
    }

    private static GenerateExpressionEvaluatorType getEvaluator(ExpressionType expression, PageBase pageBase) {
        try {
            return ExpressionUtil.getGenerateExpressionValue(expression);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get script expression value: {}", ex.getLocalizedMessage());
            pageBase.error("Couldn't get script expression value: " + ex.getLocalizedMessage());
        }
        return null;
    }

    public class GenerateExpressionWrapper implements Serializable {

        private GenerateExpressionEvaluatorModeType mode;
        private ObjectReferenceType valuePolicyRef;

        private GenerateExpressionWrapper() {
        }

        private GenerateExpressionWrapper(GenerateExpressionEvaluatorType evaluator) {
            this.mode = evaluator.getMode();
            this.valuePolicyRef = evaluator.getValuePolicyRef();
        }

        public GenerateExpressionEvaluatorType toEvaluator() {
            return new GenerateExpressionEvaluatorType().mode(mode).valuePolicyRef(valuePolicyRef);
        }

        public GenerateExpressionWrapper mode(GenerateExpressionEvaluatorModeType mode) {
            this.mode = mode;
            return GenerateExpressionWrapper.this;
        }

        public GenerateExpressionWrapper valuePolicyRef(ObjectReferenceType valuePolicyRef) {
            this.valuePolicyRef = valuePolicyRef;
            return GenerateExpressionWrapper.this;
        }

        public boolean isEmpty() {
            return mode == null && valuePolicyRef == null;
        }
    }
}
