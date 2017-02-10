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

package com.evolveum.midpoint.web.component.wf.processes.itemApproval;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.wf.DecisionsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemApprovalProcessStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;

/**
 * @author mederly
 */
public class ItemApprovalHistoryPanel extends BasePanel<WfContextType> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemApprovalHistoryPanel.class);

    private static final String ID_DECISIONS_DONE = "decisionsDone";

    public ItemApprovalHistoryPanel(String id, IModel<WfContextType> model, UserProfileStorage.TableId tableId, int pageSize) {
        super(id, model);
        initLayout(tableId, pageSize);
    }

    private void initLayout(UserProfileStorage.TableId tableId, int pageSize) {

        add(new DecisionsPanel(ID_DECISIONS_DONE, new AbstractReadOnlyModel<List<DecisionDto>>() {
            @Override
            public List<DecisionDto> getObject() {
                List<DecisionDto> rv = new ArrayList<>();
                WfContextType wfContextType = getModelObject();
				if (wfContextType == null) {
					return rv;
				}
				if (!wfContextType.getEvent().isEmpty()) {
					wfContextType.getEvent().forEach(e -> addIgnoreNull(rv, DecisionDto.create(e, getPageBase())));
				} else {
					ItemApprovalProcessStateType instanceState = WfContextUtil.getItemApprovalProcessInfo(wfContextType);
					if (instanceState != null) {
						instanceState.getDecisions().forEach(d -> addIgnoreNull(rv, DecisionDto.create(d)));
					}
				}
                return rv;
            }
        }, tableId, pageSize));

    }
}
