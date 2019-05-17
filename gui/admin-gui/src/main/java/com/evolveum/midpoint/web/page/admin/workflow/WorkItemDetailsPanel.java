/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.web.component.data.IconedObjectNamePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.model.IModel;


/**
 * Created by honchar
 */
public class WorkItemDetailsPanel extends BasePanel<CaseWorkItemType>{
    private static final long serialVersionUID = 1L;

    private static final String ID_DISPLAY_NAME_PANEL = "displayNamePanel";
    private static final String ID_REQUESTED_BY = "requestedBy";
    private static final String ID_REQUESTED_FOR = "requestedFor";
    private static final String ID_TARGET = "target";
    private static final String ID_REASON = "reason";
    private static final String ID_COMMENT = "requesterCommentMessage";
    private static final String ID_DELTAS_TO_APPROVE = "deltasToBeApproved";

    public WorkItemDetailsPanel(String id, IModel<CaseWorkItemType> caseWorkItemTypeIModel) {
        super(id, caseWorkItemTypeIModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        IconedObjectNamePanel requestedBy = new IconedObjectNamePanel(ID_REQUESTED_BY,
                WorkItemTypeUtil.getRequestorReference(getModelObject()));
        requestedBy.setOutputMarkupId(true);
        add(requestedBy);

        //todo fix what is requested for object ?
        IconedObjectNamePanel requestedFor = new IconedObjectNamePanel(ID_REQUESTED_FOR,
                WorkItemTypeUtil.getRequestorReference(getModelObject()));
        requestedFor.setOutputMarkupId(true);
        add(requestedFor);
    }
}
