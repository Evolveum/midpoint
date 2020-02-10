/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusPersonasTabPanel;
import com.evolveum.midpoint.web.component.objectdetails.UserDelegationsTabPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.*;

import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

/**
 * @author lazyman
 * @author semancik
 */

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/user", matchUrlForSecurity = "/admin/user")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageUser extends PageAdminFocus<UserType> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageUser.class.getName() + ".";
    private static final String OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS = DOT_CLASS + "loadDelegatedByMeAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST = DOT_CLASS + "createAssignmentPreviewDtoList";

    private LoadableModel<List<AssignmentEditorDto>> delegationsModel;
    private LoadableModel<List<AssignmentInfoDto>> privilegesListModel;
    private UserDelegationsTabPanel<?> userDelegationsTabPanel = null;

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    public PageUser() {
        super();
    }

    public PageUser(PageParameters parameters) {
        super(parameters);
    }

    public PageUser(final PrismObject<UserType> userToEdit) {
        super(userToEdit);
    }

    public PageUser(final PrismObject<UserType> userToEdit, boolean isNewObject)  {
        super(userToEdit, isNewObject);
    }

    @Override
    protected void initializeModel(final PrismObject<UserType> objectToEdit, boolean isNewObject, boolean isReadonly) {
        super.initializeModel(objectToEdit, isNewObject, isReadonly);

        delegationsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentEditorDto> load() {
                if (StringUtils.isNotEmpty(getObjectWrapper().getOid())) {
                    return loadDelegatedByMeAssignments();
                } else {
                    return new ArrayList<>();
                }
            }
        };
        privilegesListModel = new LoadableModel<List<AssignmentInfoDto>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentInfoDto> load() {
                return getUserPrivilegesList();
            }
        };
    }

    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel() {
        return new UserSummaryPanel(ID_SUMMARY_PANEL, isEditingFocus() ?
                Model.of(getObjectModel().getObject().getObject().asObjectable()) : Model.of(), this);
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    @Override
    protected UserType createNewObject() {
        return new UserType();
    }

    @Override
    protected Class<PageUsers> getRestartResponsePage() {
        return PageUsers.class;
    }

    @Override
    public Class<UserType> getCompileTimeClass() {
        return UserType.class;
    }

    @Override
    protected AbstractObjectMainPanel<UserType> createMainPanel(String id) {
        return new FocusMainPanel<UserType>(id, getObjectModel(), getProjectionModel(), this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void addSpecificTabs(final PageAdminObjectDetails<UserType> parentPage, List<ITab> tabs) {
                tabs.add(
                        new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.personas"),
                                getTabVisibility(ComponentConstants.UI_FOCUS_TAB_PERSONAS_URL, false, parentPage)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return new FocusPersonasTabPanel<>(panelId, getMainForm(), getObjectModel());
                            }

                            @Override
                            public String getCount() {
                                if (getObjectWrapper() == null || getObjectWrapper().getObject() == null) {
                                    return Integer.toString(0);
                                }
                                List<ObjectReferenceType> personasRefList = getObjectWrapper().getObject().asObjectable().getPersonaRef();
                                int count = 0;
                                for (ObjectReferenceType object : personasRefList) {
                                    if (object != null && !object.asReferenceValue().isEmpty()) {
                                        count++;
                                    }
                                }
                                return Integer.toString(count);
                            }

                        });

                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.delegations"),
                        getTabVisibility(ComponentConstants.UI_FOCUS_TAB_DELEGATIONS_URL, false, parentPage)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        userDelegationsTabPanel = new UserDelegationsTabPanel<>(panelId, getMainForm(), getObjectModel(),
                                delegationsModel, privilegesListModel);
                        return userDelegationsTabPanel;
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(delegationsModel.getObject() == null ? 0 : delegationsModel.getObject().size());
                    }
                });

                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.delegatedToMe"),
                        getTabVisibility(ComponentConstants.UI_FOCUS_TAB_DELEGATED_TO_ME_URL, true, parentPage)){

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new AssignmentTablePanel<UserType>(panelId,
                                getDelegatedToMeModel()) {
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
                            public IModel<String> getLabel() {
                                return parentPage.createStringResource("FocusType.delegatedToMe");
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

            @Override
            protected boolean isFocusHistoryPage(){
                return PageUser.this.isFocusHistoryPage();
            }

            @Override
            protected void viewObjectHistoricalDataPerformed(AjaxRequestTarget target, PrismObject<UserType> object, String date){
                PageUser.this.navigateToNext(new PageUserHistory(object, date));
            }
        };
    }

    private List<AssignmentEditorDto> loadDelegatedByMeAssignments() {
        OperationResult result = new OperationResult(OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS);
        List<AssignmentEditorDto> list = new ArrayList<>();
        try{

            Task task = createSimpleTask(OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS);

            PrismReferenceValue referenceValue = getPrismContext().itemFactory().createReferenceValue(getObjectWrapper().getOid(),
                    UserType.COMPLEX_TYPE);
            referenceValue.setRelation(WebComponentUtil.getDefaultRelationOrFail(RelationKindType.DELEGATION));

            ObjectFilter refFilter = getPrismContext().queryFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(referenceValue)
                    .buildFilter();

            ObjectQuery query = getPrismContext().queryFactory().createQuery(refFilter);

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

    private List<AssignmentInfoDto> getUserPrivilegesList(){
        List<AssignmentInfoDto> list = new ArrayList<>();
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST);
        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST);
        for (AssignmentType assignment : getObjectWrapper().getObject().asObjectable().getAssignment()) {
            addIgnoreNull(list, createDelegableAssignmentsPreviewDto(assignment, task, result));
        }
        return list;
    }

    @Override
    protected boolean processDeputyAssignments(boolean previewOnly) {
        boolean isAnythingChanged = false;
        for (AssignmentEditorDto dto : delegationsModel.getObject()) {
            if (!UserDtoStatus.MODIFY.equals(dto.getStatus())) {
                if (!previewOnly) {
                    UserType user = dto.getDelegationOwner();
                    saveDelegationToUser(user.asPrismObject(), dto);
                }
                isAnythingChanged = true;
            }
        }
        return isAnythingChanged;
    }

    /**
     * for now used only for delegation changes
     * @param modelContextMap
     */
    @Override
    protected void processAdditionalFocalObjectsForPreview(Map<PrismObject<UserType>, ModelContext<? extends ObjectType>> modelContextMap){
        for (AssignmentEditorDto dto : delegationsModel.getObject()) {
            if (!UserDtoStatus.MODIFY.equals(dto.getStatus())) {
                UserType user = dto.getDelegationOwner();

                OperationResult result = new OperationResult(OPERATION_PREVIEW_CHANGES);
                Task task = createSimpleTask(OPERATION_PREVIEW_CHANGES);
                try {

                Collection<ObjectDelta<? extends ObjectType>> deltas = prepareDelegationDelta(user.asPrismObject(), dto);

                ModelContext<UserType> modelContext = getModelInteractionService().previewChanges(deltas, getOptions(true), task, result);
                modelContextMap.put(user.asPrismObject(), modelContext);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Could not save delegation ", e);
                    error("Could not save delegation. Reason: " + e);
                } finally {
                    result.recomputeStatus();
                }
            }
        }
    }


    private void saveDelegationToUser(PrismObject<UserType> user, AssignmentEditorDto assignmentDto) {
        OperationResult result = new OperationResult(OPERATION_SAVE);
        try {
            getPrismContext().adopt(user);
            Collection<ObjectDelta<? extends ObjectType>> deltas = prepareDelegationDelta(user, assignmentDto);
            getModelService().executeChanges(deltas, getOptions(false), createSimpleTask(OPERATION_SAVE), result);

            result.recordSuccess();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
            error("Could not save assignments. Reason: " + e);
        } finally {
            result.recomputeStatus();
        }

        showResult(result);
    }

    private Collection<ObjectDelta<? extends ObjectType>> prepareDelegationDelta(PrismObject<UserType> user, AssignmentEditorDto dto)
            throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> delta = user.createModifyDelta();
        List<AssignmentEditorDto> userAssignmentsDtos = new ArrayList<>();
        userAssignmentsDtos.add(dto);

        deltas.add(delta);
        PrismContainerDefinition<?> def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
        handleAssignmentDeltas(delta, userAssignmentsDtos, def, true);
        return deltas;
    }

    public boolean isLoggedInUserPage(){
        return getObjectWrapper() != null && getObjectWrapper().getObject() != null &&
                StringUtils.isNotEmpty(getObjectWrapper().getObject().asObjectable().getOid()) &&
                getObjectWrapper().getObject().asObjectable().getOid().equals(WebModelServiceUtils.getLoggedInFocusOid());
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

    private boolean isConsentAssignment(AssignmentType assignment) {
        return assignment.getTargetRef() != null && QNameUtil.match(assignment.getTargetRef().getRelation(), SchemaConstants.ORG_CONSENT);
    }

    protected List<AssignmentType> getConsentsList(List<AssignmentType> assignments, UserDtoStatus status){
        List<AssignmentType> list = new ArrayList<>();
        for (AssignmentType assignment : assignments) {
            if (isConsentAssignment(assignment)) {
                //TODO set status
                list.add(assignment);
            }
        }
        return list;
    }

}
