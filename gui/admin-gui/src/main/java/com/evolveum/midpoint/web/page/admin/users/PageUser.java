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
package com.evolveum.midpoint.web.page.admin.users;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author semancik
 */
@PageDescriptor(url = "/admin/user", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description")})
public class PageUser extends PageAdminFocus<UserType> {

    private static final String DOT_CLASS = PageUser.class.getName() + ".";

    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_TASKS = "tasks";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    public PageUser() {
        initialize(null);
    }

    public PageUser(PageParameters parameters, PageBase previousPage) {
        getPageParameters().overwriteWith(parameters);
        setPreviousPage(previousPage);
        initialize(null);
    }

    public PageUser(final PrismObject<UserType> userToEdit) {
        initialize(userToEdit);
    }

    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel() {
    	return new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel());
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        // uncoment later -> check for changes to not allow leave the page when
        // some changes were made
        // try{
        // if (userModel.getObject().getOldDelta() != null &&
        // !userModel.getObject().getOldDelta().isEmpty() ||
        // userModel.getObject().getFocusPrimaryDelta() != null &&
        // !userModel.getObject().getFocusPrimaryDelta().isEmpty()){
        // showModalWindow(MODAL_ID_CONFIRM_CANCEL, target);
        // } else{
        goBackPage();

        // }
        // }catch(Exception ex){
        // LoggingUtils.logException(LOGGER, "Could not return to user list",
        // ex);
        // }
    }
    

    // many things could change (e.g. assignments, tasks) - here we deal only with tasks
    @Override
    public PageBase reinitialize() {
        TablePanel taskTable = (TablePanel) get(createComponentPath(ID_MAIN_PANEL, ID_TASKS, ID_TASK_TABLE));
        TaskDtoProvider provider = (TaskDtoProvider) taskTable.getDataTable().getDataProvider();

        provider.clearCache();
        taskTable.modelChanged();
        return this;
    }

	@Override
	protected UserType createNewObject() {
		return new UserType();
	}
	
	@Override
	protected Class getRestartResponsePage() {
		return PageUsers.class;
	}
	
	@Override
	protected Class getCompileTimeClass() {
		return UserType.class;
	}

	@Override
	protected AbstractObjectMainPanel<UserType> createMainPanel(String id) {
		return new FocusMainPanel<UserType>(id, getObjectModel(), getAssignmentsModel(), getProjectionModel(), this);
	}

	@Override
	public PageBase getDefaultBackPage() {
		return new PageUsers();
	}

}
