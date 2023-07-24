/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

public abstract class EvaluatorExpressionPanel extends BasePanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_VALUE_INPUT = "valueInput";
    private static final String ID_VALUE_CONTAINER = "valueContainer";
    private static final String ID_FEEDBACK = "feedback";

    public EvaluatorExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
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

    public abstract IModel<String> getValueContainerLabelModel();

    protected final Component getValueContainer() {
        return get(ID_VALUE_CONTAINER);
    }

    protected FeedbackLabels getFeedback() {
        return (FeedbackLabels) get(createComponentPath(ID_VALUE_CONTAINER, ID_FEEDBACK));
    }
}
