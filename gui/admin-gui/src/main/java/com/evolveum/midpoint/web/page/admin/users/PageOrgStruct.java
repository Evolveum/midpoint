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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.orgStruct.OrgStructPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;

/**
 * @author mserbak
 * 
 */
public class PageOrgStruct extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgStruct.class);

    public static final String PARAM_ORG_RETURN = "org";
    private static final String DOT_CLASS = PageOrgStruct.class.getName() + ".";
	private static final String OPERATION_LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";
	private IModel<List<PrismObject<OrgType>>> roots;

	public PageOrgStruct() {
		roots = new LoadableModel<List<PrismObject<OrgType>>>(false) {
			@Override
			protected List<PrismObject<OrgType>> load() {
				return loadOrgUnit();
			}
		};
		initLayout();
	}

	private void initLayout() {
		List<ITab> tabs = new ArrayList<ITab>();
		for (PrismObject<OrgType> root : roots.getObject()) {
			tabs.add(new TabPanel(new Model<OrgStructDto>(getOrgStructDtoFromPrism(root))));
		}
		add(new TabbedPanel("tabPanel", tabs));
	}

	private OrgStructDto getOrgStructDtoFromPrism(PrismObject<OrgType> root) {
		List<PrismObject<OrgType>> orgUnitList = new ArrayList<PrismObject<OrgType>>();
		orgUnitList.add(root);
		return new OrgStructDto<OrgType>(orgUnitList, null);
	}

	private List<PrismObject<OrgType>> loadOrgUnit() {
		Task task = createSimpleTask(OPERATION_LOAD_ORG_UNIT);
		OperationResult result = new OperationResult(OPERATION_LOAD_ORG_UNIT);

		List<PrismObject<OrgType>> orgUnitList = null;
		try {
			ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(getPrismContext());
			orgUnitList = getModelService().searchObjects(OrgType.class, query, null, task, result);
			result.recordSuccess();
		} catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unable to load org. unit", ex);
			result.recordFatalError("Unable to load org unit", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (orgUnitList == null || orgUnitList.isEmpty()) {
			getSession().error(getString("pageOrgStruct.message.noOrgStructDefined"));
			throw new RestartResponseException(PageUsers.class);
		}
		return orgUnitList;
	}

	private class TabPanel extends AbstractTab {
		private IModel<OrgStructDto> model;

		public TabPanel(IModel<OrgStructDto> model) {
			super(model.getObject().getTitle());
			this.model = model;
		}

		@Override
		public WebMarkupContainer getPanel(String id) {
			return new OrgStructPanel(id, model);
		}

	}
}
