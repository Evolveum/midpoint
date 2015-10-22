/*
 * Copyright (c) 2010-2015 Evolveum
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

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/user", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description")})
public class PageUser extends PageAdminFocus<UserType> {

    public static final String PARAM_RETURN_PAGE = "returnPage";
    private static final String DOT_CLASS = PageUser.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";  
    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_TASKS = "tasks";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
            = new LoadableModel<ExecuteChangeOptionsDto>(false) {

        @Override
        protected ExecuteChangeOptionsDto load() {
            return new ExecuteChangeOptionsDto();
        }
    };

    private ProgressReporter progressReporter;

    public PageUser() {
        initialize(null);
    }

    public PageUser(PageParameters parameters, PageTemplate previousPage) {
        getPageParameters().overwriteWith(parameters);
        setPreviousPage(previousPage);
        initialize(null);
    }

    public PageUser(final PrismObject<UserType> userToEdit) {
        initialize(userToEdit);
    }

    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel() {
    	return new UserSummaryPanel(ID_SUMMARY_PANEL, getFocusModel());
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        // uncoment later -> check for changes to not allow leave the page when
        // some changes were made
        // try{
        // if (userModel.getObject().getOldDelta() != null &&
        // !userModel.getObject().getOldDelta().isEmpty() ||
        // userModel.getObject().getObjectDelta() != null &&
        // !userModel.getObject().getObjectDelta().isEmpty()){
        // showModalWindow(MODAL_ID_CONFIRM_CANCEL, target);
        // } else{
        setSpecificResponsePage();

        // }
        // }catch(Exception ex){
        // LoggingUtils.logException(LOGGER, "Could not return to user list",
        // ex);
        // }
    }

    protected void setSpecificResponsePage() {
        StringValue orgReturn = getPageParameters().get(PARAM_RETURN_PAGE);
        if (PageOrgTree.PARAM_ORG_RETURN.equals(orgReturn.toString())) {
        	
            setResponsePage(getSessionStorage().getPreviousPage());
        } else if (getPreviousPage() != null) {
            goBack(PageDashboard.class);        // the class parameter is not necessary, is previousPage is set
        } else if (getSessionStorage() != null){
        	if (getSessionStorage().getPreviousPageInstance() != null){
        		setResponsePage(getSessionStorage().getPreviousPageInstance());
        	} else if (getSessionStorage().getPreviousPage() != null){
        		setResponsePage(getSessionStorage().getPreviousPage());
        	} else {
        		setResponsePage(PageUsers.class);
        	}
        } else {
        	setResponsePage(PageUsers.class);
        }
    }

    private List<FocusProjectionDto> getSelectedAccounts() {
        List<FocusProjectionDto> selected = new ArrayList<FocusProjectionDto>();

        List<FocusProjectionDto> all = getFocusShadows();
        for (FocusProjectionDto account : all) {
            if (account.isLoadedOK() && account.getObject().isSelected()) {
                selected.add(account);
            }
        }

        return selected;
    }
 

    // many things could change (e.g. assignments, tasks) - here we deal only with tasks
    @Override
    public PageBase reinitialize() {
        TablePanel taskTable = (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TASKS, ID_TASK_TABLE));
        TaskDtoProvider provider = (TaskDtoProvider) taskTable.getDataTable().getDataProvider();

        provider.clearCache();
        taskTable.modelChanged();
        return this;
    }

	@Override
	protected UserType createNewFocus() {
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
	protected void initTabs(List tabs) {
		// TODO Auto-generated method stub
		
	}
}
