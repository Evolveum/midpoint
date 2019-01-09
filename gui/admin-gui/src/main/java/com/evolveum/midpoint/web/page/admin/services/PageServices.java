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

package com.evolveum.midpoint.web.page.admin.services;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.FocusListComponent;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ServiceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;


/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(url = "/admin/services", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageAdminServices.auth.servicesAll.label",
                description = "PageAdminServices.auth.servicesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                label = "PageServices.auth.services.label",
                description = "PageServices.auth.services.description")})
public class PageServices extends PageAdminObjectList<ServiceType> {
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = PageServices.class.getName() + ".";
	private static final Trace LOGGER = TraceManager.getTrace(PageServices.class);
	private static final String OPERATION_DELETE_SERVICES = DOT_CLASS + "deleteServices";

	private IModel<Search> searchModel;

	public PageServices() {
		super();
	}

	private final FocusListInlineMenuHelper<ServiceType> listInlineMenuHelper = new FocusListInlineMenuHelper<ServiceType>(ServiceType.class, this, this) {
		private static final long serialVersionUID = 1L;

		protected boolean isShowConfirmationDialog(ColumnMenuAction action) {
			return PageServices.this.isShowConfirmationDialog(action);
		}

		protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName) {
			return PageServices.this.getConfirmationMessageModel(action, actionName);
		}
	};

	@Override
	public void objectDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
		serviceDetailsPerformed(target, service);
	}

	@Override
	protected List<IColumn<SelectableBean<ServiceType>, String>> initColumns() {
		return ColumnUtils.getDefaultServiceColumns();
	}

	@Override
	protected List<InlineMenuItem> createRowActions() {
		return listInlineMenuHelper.createRowActions();
	}

	@Override
	protected void newObjectActionPerformed(AjaxRequestTarget target) {
		navigateToNext(PageService.class);
	}

	@Override
	protected Class<ServiceType> getType(){
		return ServiceType.class;
	}

	@Override
	protected UserProfileStorage.TableId getTableId(){
		return TableId.TABLE_SERVICES;
	}

	protected void serviceDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, service.getOid());
		navigateToNext(PageService.class, parameters);
	}

 	private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
		return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);
//		if (action.getRowModel() == null) {
//			return createStringResource("PageServices.message.confirmationMessageForMultipleObject",
//					actionName, getObjectListPanel().getSelectedObjectsCount() );
//		} else {
//			return createStringResource("PageServices.message.confirmationMessageForSingleObject",
//					actionName, ((ObjectType)((SelectableBean)action.getRowModel().getObject()).getValue()).getName());
//		}

	}

	private boolean isShowConfirmationDialog(ColumnMenuAction action){
		return action.getRowModel() != null ||
				getObjectListPanel().getSelectedObjectsCount() > 0;
	}

}
