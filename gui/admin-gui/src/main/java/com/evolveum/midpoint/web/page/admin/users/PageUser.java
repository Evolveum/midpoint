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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusShadowDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
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
public class PageUser extends PageAdminFocus {

    public static final String PARAM_RETURN_PAGE = "returnPage";
    private static final String DOT_CLASS = PageUser.class.getName() + ".";
  


    private static final String ID_MAIN_FORM = "mainForm";
  
    private static final String ID_TASK_TABLE = "taskTable";
 
  
    private static final String ID_TASKS = "tasks";
 
    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_SUMMARY_NAME = "summaryName";
    private static final String ID_SUMMARY_FULL_NAME = "summaryFullName";
    private static final String ID_SUMMARY_GIVEN_NAME = "summaryGivenName";
    private static final String ID_SUMMARY_FAMILY_NAME = "summaryFamilyName";
    private static final String ID_SUMMARY_PHOTO = "summaryPhoto";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    private IModel<PrismObject<UserType>> summaryUser;

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

    private void initSummaryInfo(Form mainForm){

        WebMarkupContainer summaryContainer = new WebMarkupContainer(ID_SUMMARY_PANEL);
        summaryContainer.setOutputMarkupId(true);

        summaryContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                if(getPageParameters().get(OnePageParameterEncoder.PARAMETER).isEmpty()){
                    return false;
                } else {
                    return true;
                }
            }
        });

        mainForm.add(summaryContainer);

        summaryUser = new AbstractReadOnlyModel<PrismObject<UserType>>() {

            @Override
            public PrismObject<UserType> getObject() {
                ObjectWrapper user = getFocusWrapper();
                return user.getObject();
            }
        };

        summaryContainer.add(new Label(ID_SUMMARY_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_NAME)));
        summaryContainer.add(new Label(ID_SUMMARY_FULL_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_FULL_NAME)));
        summaryContainer.add(new Label(ID_SUMMARY_GIVEN_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_GIVEN_NAME)));
        summaryContainer.add(new Label(ID_SUMMARY_FAMILY_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_FAMILY_NAME)));

        Image img = new Image(ID_SUMMARY_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                if(summaryUser.getObject().asObjectable().getJpegPhoto() != null){
                    return new ByteArrayResource("image/jpeg", summaryUser.getObject().asObjectable().getJpegPhoto());
                } else {
                    return new ContextRelativeResource("img/placeholder.png");
                }

            }
        });
        summaryContainer.add(img);
    }



    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return userOid != null && StringUtils.isNotEmpty(userOid.toString());
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
        } else if (getSessionStorage() != null && getSessionStorage().getPreviousPage() != null){
            setResponsePage(getSessionStorage().getPreviousPage());
        } else {
        	setResponsePage(PageUsers.class);
        }
    }



    protected void reviveCustomModels() throws SchemaException {
        WebMiscUtil.revive(summaryUser, getPrismContext());
    }



    private List<FocusShadowDto> getSelectedAccounts() {
        List<FocusShadowDto> selected = new ArrayList<FocusShadowDto>();

        List<FocusShadowDto> all = getFocusShadows();
        for (FocusShadowDto account : all) {
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
	protected FocusType createNewFocus() {
		return new UserType();
	}

	@Override
	protected void initCustomLayout(Form mainForm) {
		initSummaryInfo(mainForm);
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
