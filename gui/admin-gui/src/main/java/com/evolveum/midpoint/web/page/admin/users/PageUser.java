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
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.CountableLoadableModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.assignment.AssignmentDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.UserDelegationsTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusConsentTabPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import java.util.*;

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
    private static final String OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS = DOT_CLASS + "loadDelegatedByMeAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST = DOT_CLASS + "createAssignmentPreviewDtoList";

    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_TASKS = "tasks";
    private LoadableModel<List<AssignmentEditorDto>> delegationsModel;
    private LoadableModel<List<AssignmentsPreviewDto>> privilegesListModel;
    private CountableLoadableModel<AssignmentDto> consentsModel;
    private UserDelegationsTabPanel userDelegationsTabPanel = null;

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    public PageUser() {
        initialize(null);
    }

    public PageUser(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageUser(final PrismObject<UserType> userToEdit) {
        initialize(userToEdit);
    }

    @Override
    protected void initializeModel(final PrismObject<UserType> objectToEdit) {
        super.initializeModel(objectToEdit);
        delegationsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {
            @Override
            protected List<AssignmentEditorDto> load() {
                if (StringUtils.isNotEmpty(getObjectWrapper().getOid())) {
                    return loadDelegatedByMeAssignments();
                } else {
                    return new ArrayList<>();
                }
            }
        };
        privilegesListModel = new LoadableModel<List<AssignmentsPreviewDto>>(false) {
            @Override
            protected List<AssignmentsPreviewDto> load() {
                return getUserPrivilegesList();
            }
        };

        consentsModel = new CountableLoadableModel<AssignmentDto>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<AssignmentDto> load() {
				return loadConsents();
			}

			@Override
			public int count() {
				return countConsents();
			}

		};
    }

    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel() {
    	return new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel(), this);
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
        redirectBack();

        // }
        // }catch(Exception ex){
        // LoggingUtils.logUnexpectedException(LOGGER, "Could not return to user list",
        // ex);
        // }
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
    public Class getCompileTimeClass() {
		return UserType.class;
	}

	@Override
	protected AbstractObjectMainPanel<UserType> createMainPanel(String id) {
        return new FocusMainPanel<UserType>(id, getObjectModel(), getAssignmentsModel(), getPolicyRulesModel(), getProjectionModel(), this) {
            @Override
            protected void addSpecificTabs(final PageAdminObjectDetails<UserType> parentPage, List<ITab> tabs) {
                FocusTabVisibleBehavior authorization;
                if (WebComponentUtil.isAuthorized(ModelAuthorizationAction.AUDIT_READ.getUrl())){
                    authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_OBJECT_HISTORY_URL);
                    tabs.add(
                            new PanelTab(parentPage.createStringResource("pageAdminFocus.objectHistory"), authorization) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return createObjectHistoryTabPanel(panelId, parentPage);
                                }
                            });
                }

                authorization = new FocusTabVisibleBehavior(unwrapModel(),
                        ComponentConstants.UI_FOCUS_TAB_DELEGATIONS_URL);
                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.delegations"), authorization)
                {
                    private static final long serialVersionUID = 1L;


                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        userDelegationsTabPanel = new UserDelegationsTabPanel<>(panelId, getMainForm(), getObjectModel(),
								delegationsModel, privilegesListModel, PageUser.this);
                        return userDelegationsTabPanel;
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(delegationsModel.getObject() == null ? 0 : delegationsModel.getObject().size());
                    }
                });

                authorization = new FocusTabVisibleBehavior(unwrapModel(),
                        ComponentConstants.UI_FOCUS_TAB_DELEGATED_TO_ME_URL);
                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.delegatedToMe"), authorization)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new AssignmentTablePanel<UserType>(panelId, parentPage.createStringResource("FocusType.delegatedToMe"),
                                getDelegatedToMeModel(), PageUser.this) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void populateAssignmentDetailsPanel(ListItem<AssignmentEditorDto> item) {
                                DelegationEditorPanel editor = new DelegationEditorPanel(ID_ROW, item.getModel(), true,
                                        privilegesListModel, PageUser.this);
                                item.add(editor);
                            }

                            @Override
                            public String getExcludeOid() {
                                return getObject().getOid();
                            }

                            @Override
                            protected List<InlineMenuItem> createAssignmentMenu() {
                                return new ArrayList<>();
                            }

                        };
                    }



                    @Override
                    public String getCount() {
                        return Integer.toString(getDelegatedToMeModel().getObject() == null ?
                                0 : getDelegatedToMeModel().getObject().size());
                    }
                });


                authorization = new FocusTabVisibleBehavior(unwrapModel(),
                        ComponentConstants.UI_FOCUS_TAB_CONSENTS_URL);
                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.consents"), authorization)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusConsentTabPanel<UserType>(panelId, getMainForm(), getObjectModel(), consentsModel, parentPage);
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(consentsModel.count());
                    }
                });
            }

            @Override
            protected boolean getOptionsPanelVisibility() {
                if (isSelfProfile()){
                    return false;
                } else {
                    return super.getOptionsPanelVisibility();
                }
            }

            @Override
            protected boolean areSavePreviewButtonsEnabled(){
                return super.areSavePreviewButtonsEnabled() ||
                        (userDelegationsTabPanel != null ? userDelegationsTabPanel.isDelegationsModelChanged() : false);
            }
        };
    }

    protected boolean isSelfProfile(){
        return false;
    }

    private List<AssignmentEditorDto> loadDelegatedByMeAssignments() {
        OperationResult result = new OperationResult(OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS);
        List<AssignmentEditorDto> list = new ArrayList<>();
        try{

            Task task = createSimpleTask(OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS);

            PrismReferenceValue referenceValue = new PrismReferenceValue(getObjectWrapper().getOid(),
                    UserType.COMPLEX_TYPE);
            referenceValue.setRelation(SchemaConstants.ORG_DEPUTY);

            ObjectFilter refFilter = QueryBuilder.queryFor(UserType.class, getPrismContext())
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(referenceValue)
                    .buildFilter();

            ObjectQuery query = new ObjectQuery();
            query.setFilter(refFilter);

            List<PrismObject<UserType>> usersList = getModelService().searchObjects(UserType.class, query, null, task, result);
            List<String> processedUsersOid = new ArrayList<>();
            if (usersList != null && usersList.size() > 0){
                for (PrismObject<UserType> user : usersList) {
                    if (processedUsersOid.contains(user.getOid())){
                        continue;
                    }
                    List<AssignmentType> assignments = user.asObjectable().getAssignment();
                    for (AssignmentType assignment : assignments) {
                        if (assignment.getTargetRef() != null &&
                                StringUtils.isNotEmpty(assignment.getTargetRef().getOid()) &&
                                assignment.getTargetRef().getOid().equals(getObjectWrapper().getOid())) {
                            AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this,
                                    user.asObjectable());
                            dto.setEditable(false);
                            list.add(dto);
                        }
                    }
                    processedUsersOid.add(user.getOid());
                }
            }

        } catch (Exception ex){
            result.recomputeStatus();
            showResult(result);
        }
        Collections.sort(list);
        return list;
    }

    private List<AssignmentsPreviewDto> getUserPrivilegesList(){
        List<AssignmentsPreviewDto> list = new ArrayList<>();
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST);
        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST);
        for (AssignmentType assignment : getObjectWrapper().getObject().asObjectable().getAssignment()){
            AssignmentsPreviewDto dto = createDelegableAssignmentsPreviewDto(assignment, task, result);
            if (dto != null){
                list.add(dto);
            }
        }
        return list;
    }

   @Override
   protected void prepareObjectDeltaForModify(ObjectDelta<UserType> focusDelta) throws SchemaException {

	super.prepareObjectDeltaForModify(focusDelta);

	PrismContainerDefinition<AssignmentType> def = getObjectDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
	if (consentsModel.isLoaded()) {
		handleAssignmentExperimentalDeltas(focusDelta, consentsModel.getObject(), def, false);
	}
}

    @Override
    protected boolean processDeputyAssignments(boolean previewOnly) {
        boolean isAnythingChanged = false;
        if (!previewOnly) {
            for (AssignmentEditorDto dto : delegationsModel.getObject()) {
                if (!UserDtoStatus.MODIFY.equals(dto.getStatus())) {
                    UserType user = dto.getDelegationOwner();
                    List<AssignmentEditorDto> userAssignmentsDtos = new ArrayList<>();
                    userAssignmentsDtos.add(dto);
                    saveDelegationToUser(user, userAssignmentsDtos);
                    isAnythingChanged = true;
                }
            }
        }
        return isAnythingChanged;
    }

    private void saveDelegationToUser(UserType user, List<AssignmentEditorDto> assignmentEditorDtos) {
        OperationResult result = new OperationResult(OPERATION_SAVE);
        ObjectDelta<UserType> delta;
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        try {
            delta = user.asPrismObject().createModifyDelta();
            deltas.add(delta);
            PrismContainerDefinition def = user.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, assignmentEditorDtos, def, true);
            getModelService().executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE), result);

            result.recordSuccess();


        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
            error("Could not save assignments. Reason: " + e);
        } finally {
            result.recomputeStatus();
        }

        showResult(result);
    }

    public boolean isLoggedInUserPage(){
        return getObjectWrapper() != null && getObjectWrapper().getObject() != null &&
                StringUtils.isNotEmpty(getObjectWrapper().getObject().asObjectable().getOid()) &&
                getObjectWrapper().getObject().asObjectable().getOid().equals(WebModelServiceUtils.getLoggedInUserOid());
    }

    protected int countConsents() {
		int consentCounter = 0;
		PrismObject<UserType> focus = getObjectModel().getObject().getObject();
		List<AssignmentType> assignments = focus.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {
			if (isConsentAssignment(assignment)) {
				consentCounter++;
			}
		}
		return consentCounter;
	}

    private List<AssignmentDto> loadConsents() {
		ObjectWrapper<UserType> focusWrapper = getObjectModel().getObject();
		PrismObject<UserType> focus = focusWrapper.getObject();
		List<AssignmentDto> list = getConsentsList(focus.asObjectable().getAssignment(), StringUtils.isEmpty(focusWrapper.getOid()) ?
				UserDtoStatus.ADD : UserDtoStatus.MODIFY);
		Collections.sort(list);
		return list;
	}

    private boolean isConsentAssignment(AssignmentType assignment) {
    	return assignment.getTargetRef() != null && QNameUtil.match(assignment.getTargetRef().getRelation(), SchemaConstants.ORG_CONSENT);
    }

    protected List<AssignmentDto> getConsentsList(List<AssignmentType> assignments, UserDtoStatus status){
		List<AssignmentDto> list = new ArrayList<AssignmentDto>();
		for (AssignmentType assignment : assignments) {
			if (isConsentAssignment(assignment)) {
				list.add(new AssignmentDto(assignment, status));
			}
		}
		return list;
	}

}
