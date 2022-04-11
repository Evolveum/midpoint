/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.page.admin.workflow.EvaluatedTriggerGroupPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

public class ApprovalProcessesPreviewPanel extends BasePanel<List<ApprovalProcessExecutionInformationDto>> {

    private static final String ID_PROCESSES = "processes";
    private static final String ID_NAME = "name";
    private static final String ID_PREVIEW = "preview";
    private static final String ID_TRIGGERS = "triggers";

    public ApprovalProcessesPreviewPanel(String id, IModel<List<ApprovalProcessExecutionInformationDto>> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        ListView<ApprovalProcessExecutionInformationDto> list = new ListView<ApprovalProcessExecutionInformationDto>(ID_PROCESSES, getModel()) {
            @Override
            protected void populateItem(ListItem<ApprovalProcessExecutionInformationDto> item) {
                item.add(new Label(ID_NAME, LoadableModel.create(() -> {
                    String targetName = item.getModelObject().getTargetName();
                    if (targetName != null) {
                        return ApprovalProcessesPreviewPanel.this.getString("ApprovalProcessesPreviewPanel.processRelatedTo", targetName);
                    } else {
                        return getString("ApprovalProcessesPreviewPanel.process");
                    }
                }, false)));
                item.add(new ApprovalProcessExecutionInformationPanel(ID_PREVIEW, item.getModel()));
                item.add(new EvaluatedTriggerGroupPanel(ID_TRIGGERS, new PropertyModel<>(item.getModel(), ApprovalProcessExecutionInformationDto.F_TRIGGERS)));
            }
        };
        add(list);
    }

}
