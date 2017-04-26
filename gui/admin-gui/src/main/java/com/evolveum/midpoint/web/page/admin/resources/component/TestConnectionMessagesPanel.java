/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class TestConnectionMessagesPanel extends BasePanel {
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = TestConnectionMessagesPanel.class.getName() + ".";

    private static final String OPERATION_TEST_CONNECTION = DOT_CLASS + "testConnection";

    private static final String ID_MESSAGES_PANEL = "messagesPanel";
    private static final String ID_CONNECTOR_MESSAGES_PANEL = "connectorMessagesPanel";
    private static final String ID_CONNECTOR_NAME = "connectorName";
    private static final String ID_CONNECTOR_MESSAGES = "connectorMessages";
    private static final String ID_RESOURCE_MESSAGES = "resourceMessages";
    
    private PageBase parentPage;
    private ListModel<OpResult> modelResourceResults;
    private ListModel<ConnectorStruct> connectorResourceResults;

    public TestConnectionMessagesPanel(String id, String resourceOid, PageBase parentPage) {
        super(id);
        this.parentPage = parentPage;
        initResultsModel(resourceOid);
        initLayout();
    }

    private void initResultsModel(String resourceOid) {
        OperationResult result = new OperationResult(OPERATION_TEST_CONNECTION);
        List<OpResult> resourceResultsDto = new ArrayList<>();
        List<ConnectorStruct> connectorStructs = new ArrayList<>();
        if (StringUtils.isNotEmpty(resourceOid)) {
        	Task task = parentPage.createSimpleTask(OPERATION_TEST_CONNECTION);
            try {
                result = parentPage.getModelService().testResource(resourceOid, task);
            } catch (ObjectNotFoundException e) {
                result.recordFatalError("Failed to test resource connection", e);
            }

            for (OperationResult subresult: result.getSubresults()) {
            	if (isConnectorResult(subresult)) {
            		ConnectorStruct connectorStruct = new ConnectorStruct();
            		connectorStruct.connectorName = (String) subresult.getParams().get(OperationResult.PARAM_NAME);
            		if (connectorStruct.connectorName == null) {
            			connectorStruct.connectorName = "";
            		}
            		connectorStruct.connectorResultsDto = new ArrayList<>();
            		for (OperationResult subsubresult: subresult.getSubresults()) {
            			if (isKnownResult(subsubresult)) {
            				connectorStruct.connectorResultsDto.add(OpResult.getOpResult(parentPage, subsubresult));
            			}
            		}
            		connectorStructs.add(connectorStruct);
            	} else if (isKnownResult(subresult)) {
            		// resource operation
            		resourceResultsDto.add(OpResult.getOpResult(parentPage, subresult));
            	}
                	
            }

            if (result.isSuccess()) {
                result.recomputeStatus();
            }
        }
        modelResourceResults = new ListModel<>(resourceResultsDto);
        connectorResourceResults = new ListModel<>(connectorStructs);
    }

	private boolean isConnectorResult(OperationResult subresult) {
		return subresult.getOperation().equals(ConnectorTestOperation.CONNECTOR_TEST.getOperation());
	}
	
    private boolean isKnownResult(OperationResult subresult) {
    	for (ConnectorTestOperation connectorOperation : ConnectorTestOperation.values()) {
    		if (connectorOperation.getOperation().equals(subresult.getOperation())) {
    			return true;
    		}
    	}
		return false;
	}


	private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer messagesPanel = new WebMarkupContainer(ID_MESSAGES_PANEL);
        messagesPanel.setOutputMarkupId(true);
        add(messagesPanel);
        
        ListView<ConnectorStruct> connectorView = new ListView<ConnectorStruct>(ID_CONNECTOR_MESSAGES_PANEL, connectorResourceResults) {
			private static final long serialVersionUID = 1L;
			@Override
			protected void populateItem(ListItem<ConnectorStruct> item) {
				Label connectorNameLabel = new Label(ID_CONNECTOR_NAME, item.getModelObject().connectorName);
				item.add(connectorNameLabel);
	        	RepeatingView connectorResultView = new RepeatingView(ID_CONNECTOR_MESSAGES);
	        	List<OpResult> resultsDto = item.getModelObject().connectorResultsDto;
	        	if (resultsDto != null) {
	                initResultsPanel(connectorResultView, resultsDto, parentPage);
	            }
	        	item.add(connectorResultView);
			}
        	
        };
        messagesPanel.add(connectorView);

        RepeatingView resultView = new RepeatingView(ID_RESOURCE_MESSAGES);
        if (modelResourceResults.getObject() != null) {
            initResultsPanel(resultView, modelResourceResults.getObject(), parentPage);
        }
        resultView.setOutputMarkupId(true);
        messagesPanel.add(resultView);
    }

    public void initResultsPanel(RepeatingView resultView, List<OpResult> opresults, Page parentPage) {
        for (OpResult result : opresults) {
            OperationResultPanel resultPanel = new OperationResultPanel(resultView.newChildId(), new Model<>(result), parentPage);
            resultPanel.setOutputMarkupId(true);
            resultView.add(resultPanel);
        }
    }

	private class ConnectorStruct implements Serializable {
		private String connectorName;
		private List<OpResult> connectorResultsDto;
	}
}
