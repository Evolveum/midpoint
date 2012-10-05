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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.orgStruct.OrgStructPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;

/**
 * @author mserbak
 * 
 */
public class PageOrgStruct extends PageAdminUsers {

	private static final String DOT_CLASS = PageOrgStruct.class.getName() + ".";
	private static final String OPERATION_LOAD_ORGUNIT = DOT_CLASS + "load org unit";
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
		Task task = createSimpleTask(OPERATION_LOAD_ORGUNIT);
		OperationResult result = new OperationResult(OPERATION_LOAD_ORGUNIT);

		List<PrismObject<OrgType>> orgUnitList = null;
		try {
			ObjectQuery query = ObjectQueryUtil.createRootOrgQuery(getPrismContext());
			orgUnitList = getModelService().searchObjects(OrgType.class, query, task, result);
			result.recordSuccess();
		} catch (Exception ex) {
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
