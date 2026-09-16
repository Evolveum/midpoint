/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

public abstract class EvaluatorExpressionPanel extends BasePanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_VALUE_CONTAINER = "valueContainer";
    private static final String ID_FEEDBACK = "feedback";

    /**
     * Type of the object the expression is evaluated against. Null when the context does not tell us.
     */
    private final IModel<QName> expressionTargetTypeModel;

    public EvaluatorExpressionPanel(String id, IModel<ExpressionType> model) {
        this(id, model, null);
    }

    public EvaluatorExpressionPanel(String id, IModel<ExpressionType> model, IModel<QName> expressionTargetTypeModel) {
        super(id, model);
        this.expressionTargetTypeModel = expressionTargetTypeModel;
    }

    /**
     * Type of the object the expression is evaluated against, for panels that need to know the
     * schema they work with.
     *
     * @return the type, or null when the context does not tell us and the panel has to manage
     * without it.
     */
    protected QName getExpressionTargetType() {
        return expressionTargetTypeModel != null ? expressionTargetTypeModel.getObject() : null;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        WebMarkupContainer parent = new WebMarkupContainer(ID_VALUE_CONTAINER);
        parent.setOutputMarkupId(true);
        add(parent);

        FeedbackLabels feedback = new FeedbackLabels(ID_FEEDBACK);
        feedback.setFilter(new ContainerFeedbackMessageFilter(parent));
        feedback.setOutputMarkupPlaceholderTag(true);
        parent.add(feedback);

        initLayout(parent);
    }

    protected abstract void initLayout(MarkupContainer valueContainer);

    public abstract IModel<String> getValueContainerLabelModel(PageBase pageBase);

    protected final Component getValueContainer() {
        return get(ID_VALUE_CONTAINER);
    }

    protected FeedbackLabels getFeedback() {
        return (FeedbackLabels) get(createComponentPath(ID_VALUE_CONTAINER, ID_FEEDBACK));
    }
}
