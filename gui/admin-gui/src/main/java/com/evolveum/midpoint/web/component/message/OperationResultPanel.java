/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.message;

import com.evolveum.midpoint.gui.api.component.result.Context;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.Param;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LoadableModel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
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
    	WebMarkupContainer operationPanel = new WebMarkupContainer("operationPanel");
    	operationPanel.setOutputMarkupId(true);
    	add(operationPanel);
        Label operation = new Label("operation", new LoadableModel<Object>() {

            @Override
            protected Object load() {
                OpResult result = model.getObject();

                String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
                return getPage().getString(resourceKey, null, resourceKey);
            }
        });
        operation.setOutputMarkupId(true);
        operationPanel.add(operation);
        operationPanel.add(initCountPanel(model));
        
        WebMarkupContainer arrow = new WebMarkupContainer("arrow");
        arrow.add(new AttributeAppender("class", createArrowClass(model), " "));
        arrow.setMarkupId(operationPanel.getMarkupId() + "_arrow");
        add(arrow);
        

        WebMarkupContainer operationContent = new WebMarkupContainer("operationContent");
        operationContent.setMarkupId(operationPanel.getMarkupId() + "_content");
        add(operationContent);

        operationContent.add(new Label("message", new PropertyModel<String>(model, "message")));

        initParams(operationContent, model);
        initContexts(operationContent, model);
        //initCount(operationContent, model);
        initExceptionLayout(operationContent, model);

       
    }
    
    private WebMarkupContainer initCountPanel(final IModel<OpResult> model){
    	WebMarkupContainer countPanel = new WebMarkupContainer("countPanel");
    	countPanel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                OpResult result = model.getObject();
                return result.getCount() > 1;
            }
        });
    	countPanel.add(new Label("count", new PropertyModel<String>(model, "count")));
    	return countPanel;
    }

/*    private void initCount(WebMarkupContainer operationContent, final IModel<OpResult> model) {
        WebMarkupContainer countLi = new WebMarkupContainer("countLi");
        countLi.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                OpResult result = model.getObject();
                return result.getCount() > 1;
            }
        });
        operationContent.add(countLi);
        countLi.add(new Label("count", new PropertyModel<String>(model, "count")));
    }*/

    private void initParams(WebMarkupContainer operationContent, final IModel<OpResult> model) {
        ListView<Param> params = new ListView<Param>("params",
                createParamsModel(model)) {

            @Override
            protected void populateItem(ListItem<Param> item) {
                item.add(new Label("paramName", new PropertyModel<Object>(item.getModel(), "name")));
                item.add(new Label("paramValue", new PropertyModel<Object>(item.getModel(), "value")));
            }
        };
        operationContent.add(params);
        
        ListView<OpResult> subresults = new ListView<OpResult>("subresults",
                createSubresultsModel(model)) {

            @Override
            protected void populateItem(final ListItem<OpResult> item) {
                item.add(new AttributeAppender("class", createMessageLiClass(item.getModel()), " "));
                item.add(new AttributeModifier("title", new LoadableModel<String>() {

					@Override
					protected String load() {
						return getString("operationResultPanel.title." + createMessageTooltip(item.getModel()).getObject());
					}
				}));
                item.add(new OperationResultPanel("subresult", item.getModel()));
            }
        };
        operationContent.add(subresults);
        
        
    }
    
    private void initContexts(WebMarkupContainer operationContent, final IModel<OpResult> model) {
        ListView<Context> contexts = new ListView<Context>("contexts",
                createContextsModel(model)) {
			@Override
			protected void populateItem(ListItem<Context> item) {
				item.add(new Label("contextName", new PropertyModel<Object>(item.getModel(), "name")));
                item.add(new Label("contextValue", new PropertyModel<Object>(item.getModel(), "value")));
			}
        };
        operationContent.add(contexts);
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
                    case HANDLED_ERROR:
                        return "messages-exp-details-section";
                    case UNKNOWN:
                    case WARNING:
                    default:
                        return "messages-warn-details-section";
                }
            }
        };
    }
    
    static IModel<String> createMessageTooltip(final IModel<OpResult> model){
        return new LoadableModel<String>() {

			@Override
			protected String load() {
            	OpResult result = model.getObject();
                switch (result.getStatus()) {        	
                    case FATAL_ERROR:
                    	return "fatalError";
                    case PARTIAL_ERROR:
                    	return "partialError";
                    case IN_PROGRESS:
                    	return "inProgress";
                    case NOT_APPLICABLE:
                    	return "info";
                    case SUCCESS:
                    	return "success";
                    case HANDLED_ERROR:
                    	return "expectedError";
                    case UNKNOWN:
                    	return "unknown";
                    case WARNING:
                    default:
                    	return "warn";
                }
			}
        };
    }
    
    static IModel<String> createArrowClass(final IModel<OpResult> model) {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                OpResult result = model.getObject();
                if (result == null || result.getStatus() == null) {
                    LOGGER.warn("Operation result on panel is null or has null status (will be rendered as warning).");
                    return "messages-warn-details-bold-arrow";
                }
                switch (result.getStatus()) {
                    case FATAL_ERROR:
                    case PARTIAL_ERROR:
                        return "messages-error-details-bold-arrow";
                    case IN_PROGRESS:
                    case NOT_APPLICABLE:
                        return "messages-info-details-bold-arrow";
                    case SUCCESS:
                        return "messages-succ-details-bold-arrow";
                    case HANDLED_ERROR:
                        return "messages-exp-details-bold-arrow";
                    case UNKNOWN:
                    case WARNING:
                    default:
                        return "messages-warn-details-bold-arrow";
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
    
    static IModel<List<Context>> createContextsModel(final IModel<OpResult> model) {
        return new LoadableModel<List<Context>>(false) {

            @Override
            protected List<Context> load() {
                OpResult result = model.getObject();
                return result.getContexts();
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
