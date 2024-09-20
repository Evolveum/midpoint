/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ShadowOwnerReferenceSearchExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowOwnerReferenceSearchExpressionPanel.class);

    private static final String ID_RELATION_QNAME_INPUT = "relationQNameInput";
    private static final String ID_RELATION_QNAME_LABEL = "relationQNameLabel";
    private static final String ID_RELATION_EXPRESSION_INPUT = "relationExpressionInput";
    private static final String ID_RELATION_EXPRESSION_LABEL = "relationExpressionLabel";

    public ShadowOwnerReferenceSearchExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
        updateEvaluatorValue();
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("ShadowOwnerReferenceSearchExpressionPanel.label");
    }

    private void updateEvaluatorValue() {
        ExpressionType expressionType = getModelObject();
        try {
            ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator = getEvaluatorValue().toEvaluator();
            expressionType = ExpressionUtil.updateShadowOwnerReferenceSearchExpressionValue(expressionType, evaluator);
            getModel().setObject(expressionType);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update shadowOwnerReferenceSearch expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update shadowOwnerReferenceSearch expression value: " + ex.getLocalizedMessage());
        }
    }

    protected void initLayout(MarkupContainer parent) {
        parent.add(new Label(ID_RELATION_QNAME_LABEL, createStringResource("ReferenceSearchExpressionEvaluatorType.relation")));

        parent.add(createQNameRelationPanel());

        parent.add(new Label(ID_RELATION_EXPRESSION_LABEL, createStringResource("ReferenceSearchExpressionEvaluatorType.relationExpression")));

        parent.add(createExpressionRelationPanel());
    }

    private Component createQNameRelationPanel() {
        TextPanel<QName> qNameRelationPanel = new TextPanel<>(
                ID_RELATION_QNAME_INPUT, Model.of(getEvaluatorValue().qNameRelation), QName.class, false);
        qNameRelationPanel.setOutputMarkupId(true);

        qNameRelationPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                QName qNameRelation = qNameRelationPanel.getBaseFormComponent().getConvertedInput();
                updateEvaluatorValue(qNameRelation);
                target.add(getFeedback());
            }
        });

        return qNameRelationPanel;
    }

    private Component createExpressionRelationPanel() {
        IModel<ExpressionType> model = new IModel<>() {
            @Override
            public ExpressionType getObject() {
                try {
                    ExpressionType expression = getEvaluatorValue().expressionRelation;
                    if (expression == null) {
                        expression = new ExpressionType();
                    }
                    return expression;
                } finally {
                    updateEvaluatorValue();
                }
            }

            @Override
            public void setObject(ExpressionType expression) {
                updateEvaluatorValue(expression);
            }
        };
        ExpressionPanel expressionRelationPanel = new ExpressionPanel(ID_RELATION_EXPRESSION_INPUT, model){
            @Override
            protected List<RecognizedEvaluator> getChoices() {
                return List.of(RecognizedEvaluator.PATH, RecognizedEvaluator.SCRIPT);
            }
        };
        expressionRelationPanel.setOutputMarkupId(true);
        return expressionRelationPanel;
    }

    private void updateEvaluatorValue(ExpressionType expression) {
        ExpressionType expressionType = getModelObject();
        try {
            ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper evaluatorWrapper = getEvaluatorValue();
            if (!ExpressionUtil.isEmpty(expression)) {
                evaluatorWrapper.expressionRelation(expression);
            }

            ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator = evaluatorWrapper.toEvaluator();
            expressionType = ExpressionUtil.updateShadowOwnerReferenceSearchExpressionValue(expressionType, evaluator);
            getModel().setObject(expressionType);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update script expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update script expression values: " + ex.getLocalizedMessage());
        }
    }

    private void updateEvaluatorValue(QName qNameRelation) {
        ExpressionType expressionType = getModelObject();
        try {
            ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper evaluatorWrapper = getEvaluatorValue();

            ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator = evaluatorWrapper.qNameRelation(qNameRelation).toEvaluator();
            expressionType = ExpressionUtil.updateShadowOwnerReferenceSearchExpressionValue(expressionType, evaluator);
            getModel().setObject(expressionType);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update script expression values: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update script expression values: " + ex.getLocalizedMessage());
        }
    }

    //don't remove it, used by class and method name
    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        return getRelation(expression, pageBase);
    }

    private ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper getEvaluatorValue() {
        try {
            ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator = ExpressionUtil.getShadowOwnerExpressionValue(getModelObject());
            if (evaluator == null) {
                return new ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper();
            }
            return new ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper(evaluator);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get shadowOwnerReferenceSearch expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't get shadowOwnerReferenceSearch expression value: " + ex.getLocalizedMessage());
        }
        return null;
    }

    private static String getRelation(ExpressionType expression, PageBase pageBase) {
        try {
            ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator = ExpressionUtil.getShadowOwnerExpressionValue(expression);
            if (evaluator == null) {
                return "";
            }
            if (evaluator.getRelation() != null) {
                RelationDefinitionType relation = RelationUtil.getRelationDefinition(evaluator.getRelation());
                if (relation != null) {
                    String name = GuiDisplayTypeUtil.getTranslatedLabel(relation.getDisplay());
                    if (StringUtils.isNotBlank(name)) {
                        return name;
                    }
                }

                return evaluator.getRelation().getLocalPart();
            }

            if (evaluator.getRelationExpression() == null
                    || evaluator.getRelationExpression().getExpressionEvaluator().isEmpty()) {
                return "";
            }

            String json = PrismContext.get().jsonSerializer().serialize(
                    evaluator.getRelationExpression().getExpressionEvaluator().get(0));
            if (json.length() > 2) {
                return json.substring(1, json.length() - 2);
            }
            return json;
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get shadowOwnerReferenceSearch expression value: {}", ex.getLocalizedMessage());
            pageBase.error("Couldn't get shadowOwnerReferenceSearch expression value: " + ex.getLocalizedMessage());
        }
        return "";
    }

    public class ShadowOwnerExpressionWrapper implements Serializable {

        private QName qNameRelation;
        private ExpressionType expressionRelation;

        private ShadowOwnerExpressionWrapper() {
        }

        private ShadowOwnerExpressionWrapper(ShadowOwnerReferenceSearchExpressionEvaluatorType evaluator) {
            this.qNameRelation = evaluator.getRelation();
            this.expressionRelation = evaluator.getRelationExpression();
        }

        public ShadowOwnerReferenceSearchExpressionEvaluatorType toEvaluator() {
            return new ShadowOwnerReferenceSearchExpressionEvaluatorType()
                    .relation(qNameRelation).relationExpression(expressionRelation);
        }

        public ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper qNameRelation(QName qNameRelation) {
            this.qNameRelation = qNameRelation;
            return ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper.this;
        }

        public ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper expressionRelation(ExpressionType expressionRelation) {
            this.expressionRelation = expressionRelation;
            return ShadowOwnerReferenceSearchExpressionPanel.ShadowOwnerExpressionWrapper.this;
        }

        public boolean isEmpty() {
            return qNameRelation == null && ExpressionUtil.isEmpty(expressionRelation);
        }
    }
}
