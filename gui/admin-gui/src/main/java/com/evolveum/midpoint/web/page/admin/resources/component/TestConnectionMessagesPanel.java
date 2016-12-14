/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class TestConnectionMessagesPanel extends BasePanel {
    private static final String DOT_CLASS = TestConnectionMessagesPanel.class.getName() + ".";

    private static final String OPERATION_TEST_CONNECTION = DOT_CLASS + "testConnection";

    private static final String ID_MESSAGES_PANEL = "messagesPanel";
    private static final String ID_MESSAGES = "messages";
    private PageBase parentPage;
    private ListModel<OpResult> model;

    public TestConnectionMessagesPanel(String id, String resourceOid, PageBase parentPage) {
        super(id);
        this.parentPage = parentPage;
        initResultsModel(resourceOid);
        initLayout();
    }

    private void initResultsModel(String resourceOid) {
        OperationResult result = new OperationResult(OPERATION_TEST_CONNECTION);
        List<OpResult> resultsDto = new ArrayList<>();
        if (StringUtils.isNotEmpty(resourceOid)) {
            try {
                Task task = parentPage.createSimpleTask(OPERATION_TEST_CONNECTION);
                result = parentPage.getModelService().testResource(resourceOid, task);
                resultsDto = WebComponentUtil.getTestConnectionResults(result, parentPage);
            } catch (ObjectNotFoundException e) {
                result.recordFatalError("Failed to test resource connection", e);
            }

            if (result.isSuccess()) {
                result.recomputeStatus();
            }
        }
        model = new ListModel<OpResult>(resultsDto);
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer messagesPanel = new WebMarkupContainer(ID_MESSAGES_PANEL);
        messagesPanel.setOutputMarkupId(true);
        add(messagesPanel);

        RepeatingView resultView = new RepeatingView(ID_MESSAGES);
        if (model.getObject() != null && model.getObject().size() > 0) {
            initResultsPanel(resultView, parentPage);
        }
        resultView.setOutputMarkupId(true);
        messagesPanel.add(resultView);
    }

    public void initResultsPanel(RepeatingView resultView, Page parentPage) {
        for (OpResult result : model.getObject()) {
            OperationResultPanel resultPanel = new OperationResultPanel(resultView.newChildId(), new Model<>(result), parentPage);
            resultPanel.setOutputMarkupId(true);
            resultView.add(resultPanel);
        }
    }


}
