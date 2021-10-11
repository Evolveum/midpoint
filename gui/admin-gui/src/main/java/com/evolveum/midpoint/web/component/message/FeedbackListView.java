/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.message;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.component.result.ValidationErrorPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 * @author katkav
 */
public class FeedbackListView extends ListView<FeedbackMessage> {

    private static final long serialVersionUID = 1L;

    public FeedbackListView(String id, Component component) {
        super(id);
        setDefaultModel(new FeedbackMessagesModel(component));
    }

    @Override
    protected void populateItem(final ListItem<FeedbackMessage> item) {

        final FeedbackMessage message = item.getModelObject();

        if (message.getMessage() instanceof OpResult) {
            final OpResult opResult = (OpResult) message.getMessage();
            OperationResultPanel panel = new OperationResultPanel("message", Model.of(opResult)) {

                private static final long serialVersionUID = 1L;

                @Override
                public void close(AjaxRequestTarget target) {
                    super.close(target);
                    message.markRendered();
                }

                protected void onAfterRender() {
                    opResult.setAlreadyShown(true);
                    super.onAfterRender();
                }
            };
            panel.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                public boolean isVisible() {
                    return !opResult.isAlreadyShown();
                }
            });

            panel.setOutputMarkupId(true);
            item.add(panel);
        } else {

            message.markRendered();
            ValidationErrorPanel validationPanel = new ValidationErrorPanel("message", item.getModel()) {

                private static final long serialVersionUID = 1L;

                @Override
                public void close(AjaxRequestTarget target) {
                    super.close(target);
                    message.markRendered();
                }

            };
            validationPanel.setOutputMarkupId(true);
            item.add(validationPanel);

        }
    }
}
