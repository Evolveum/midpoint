/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class OperationResultPanel extends Panel {

    public OperationResultPanel(String id, final IModel<OpResult> model) {
        super(id);

        add(new AttributeAppender("class", new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return getDetailsCss(model);
            }
        }, " "));

        initLayout(model);
    }

    private String getDetailsCss(final IModel<OpResult> model) {
        OpResult result = model.getObject();
        if (result == null || result.getStatus() == null) {
            return "messages-warn-content";
        }

        switch (result.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                return "messages-error-content";
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                return "messages-info-content";
            case SUCCESS:
                return "messages-succ-content";
            case UNKNOWN:
            case WARNING:
            default:
                return "messages-warn-content";
        }
    }

    private void initLayout(final IModel<OpResult> model) {
        WebMarkupContainer messageLi = new WebMarkupContainer("messageLi");
        add(messageLi);
        Label operation = new Label("operation", new PropertyModel<Object>(model, "operation"));
        operation.setOutputMarkupId(true);
        WebMarkupContainer arrow = new WebMarkupContainer("arrow");
        arrow.setMarkupId(operation.getMarkupId() + "_arrow");
        messageLi.add(arrow);

        messageLi.add(operation);//todo localize
        messageLi.add(new AttributeAppender("class", createMessageLiClass(model), " "));

        WebMarkupContainer operationContent = new WebMarkupContainer("operationContent");
        operationContent.setMarkupId(operation.getMarkupId() + "_content");
        messageLi.add(operationContent);

        operationContent.add(new Label("message", new PropertyModel<String>(model, "message")));

        ListView<Param> params = new ListView<Param>("params",
                createParamsModel(model)) {

            @Override
            protected void populateItem(ListItem<Param> item) {
                item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "name")));
                item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
            }
        };
        operationContent.add(params);

        WebMarkupContainer exception = new WebMarkupContainer("exception") {

            @Override
            public boolean isVisible() {
                OpResult result = model.getObject();
                return StringUtils.isNotEmpty(result.getExceptionMessage())
                        || StringUtils.isNotEmpty(result.getExceptionsStackTrace());
            }
        };
        operationContent.add(exception);
        exception.add(new Label("exceptionMessage", new PropertyModel<String>(model, "exceptionMessage")));

        WebMarkupContainer errorStack = new WebMarkupContainer("errorStack");
        errorStack.setOutputMarkupId(true);
        exception.add(errorStack);

        WebMarkupContainer errorStackContent = new WebMarkupContainer("errorStackContent");
        errorStackContent.setMarkupId(errorStack.getMarkupId() + "_content");
        exception.add(errorStackContent);

        errorStackContent.add(new Label("exceptionStack", new PropertyModel<String>(model, "exceptionsStackTrace")));

        ListView<OpResult> subresults = new ListView<OpResult>("subresults",
                createSubresultsModel(model)) {

            @Override
            protected void populateItem(ListItem<OpResult> item) {
                item.add(new OperationResultPanel("subresult", item.getModel()));
            }
        };
        messageLi.add(subresults);
    }

    private IModel<String> createMessageLiClass(final IModel<OpResult> model) {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                OpResult result = model.getObject();
                if (result == null || result.getStatus() == null) {
                    return "messages-warn-details-section";
                }
                switch (result.getStatus()) {
                    case FATAL_ERROR:
                    case PARTIAL_ERROR:
                        return "messages-error-details-section";
                    case IN_PROGRESS:
                    case NOT_APPLICABLE:
                        return "messages-info-details-section";
                    case SUCCESS:
                        return "messages-succ-details-section";
                    case UNKNOWN:
                    case WARNING:
                    default:
                        return "messages-warn-details-section";
                }
            }
        };
    }

    private IModel<List<Param>> createParamsModel(final IModel<OpResult> model) {
        return new LoadableModel<List<Param>>(false) {

            @Override
            protected List<Param> load() {
                OpResult result = model.getObject();
                return result.getParams();
            }
        };
    }

    private IModel<List<OpResult>> createSubresultsModel(final IModel<OpResult> model) {
        return new LoadableModel<List<OpResult>>(false) {

            @Override
            protected List<OpResult> load() {
                OpResult result = model.getObject();
                List<OpResult> subresults = result.getSubresults();
                if (subresults == null) {
                    subresults = new ArrayList<OpResult>();
                }

                return subresults;
            }
        };
    }
}
