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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
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

    private static final Trace LOGGER = TraceManager.getTrace(OperationResultPanel.class);
    static final String OPERATION_RESOURCE_KEY_PREFIX = "operation.";

    public OperationResultPanel(String id, final IModel<OpResult> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<OpResult> model) {
        Label operation = new Label("operation", new LoadableModel<Object>() {

            @Override
            protected Object load() {
                OpResult result = model.getObject();

                String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
                return getPage().getString(resourceKey, null, resourceKey);
            }
        });
        operation.setOutputMarkupId(true);
        WebMarkupContainer arrow = new WebMarkupContainer("arrow");
        arrow.setMarkupId(operation.getMarkupId() + "_arrow");
        add(arrow);

        add(operation);

        WebMarkupContainer operationContent = new WebMarkupContainer("operationContent");
        operationContent.setMarkupId(operation.getMarkupId() + "_content");
        add(operationContent);

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

        initExceptionLayout(operationContent, model);

        ListView<OpResult> subresults = new ListView<OpResult>("subresults",
                createSubresultsModel(model)) {

            @Override
            protected void populateItem(ListItem<OpResult> item) {
                item.add(new AttributeAppender("class", createMessageLiClass(model), " "));
                item.add(new OperationResultPanel("subresult", item.getModel()));
            }
        };
        add(subresults);
    }

    private void initExceptionLayout(WebMarkupContainer operationContent, final IModel<OpResult> model) {
        WebMarkupContainer exception = new WebMarkupContainer("exception") {

            @Override
            public boolean isVisible() {
                OpResult result = model.getObject();
                return StringUtils.isNotEmpty(result.getExceptionMessage())
                        || StringUtils.isNotEmpty(result.getExceptionsStackTrace());
            }
        };
        operationContent.add(exception);
        exception.add(new MultiLineLabel("exceptionMessage", new PropertyModel<String>(model, "exceptionMessage")));

        WebMarkupContainer errorStack = new WebMarkupContainer("errorStack");
        errorStack.setOutputMarkupId(true);
        exception.add(errorStack);

        WebMarkupContainer errorStackContent = new WebMarkupContainer("errorStackContent");
        errorStackContent.setMarkupId(errorStack.getMarkupId() + "_content");
        exception.add(errorStackContent);

        errorStackContent.add(new MultiLineLabel("exceptionStack", new PropertyModel<String>(model, "exceptionsStackTrace")));
    }

    static IModel<String> createMessageLiClass(final IModel<OpResult> model) {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                OpResult result = model.getObject();
                if (result == null || result.getStatus() == null) {
                    LOGGER.warn("Operation result on panel is null or has null status (will be rendered as warning).");
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

    static IModel<List<Param>> createParamsModel(final IModel<OpResult> model) {
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
