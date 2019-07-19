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
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.List;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(url = "/admin/case", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = "PageAdminCases.auth.casesAll.label",
                description = "PageAdminCases.auth.casesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_URL,
                label = "PageCase.auth.case.label",
                description = "PageCase.auth.case.description")})
public class PageCase  extends PageAdminObjectDetails<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageCase.class);
    private static final String DOT_CLASS = PageCase.class.getName() + ".";
    private static final String OPERATION_LOAD_CASE = DOT_CLASS + "loadCase";

    private static final String ID_SUMMARY_PANEL = "summaryPanel";

    public PageCase() {
        this(null, true);
    }

    public PageCase(PrismObject<CaseType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject, true);
    }

    public PageCase(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null, true, true);
    }


    @Override
    protected AbstractObjectMainPanel<CaseType> createMainPanel(String id) {
        return new AssignmentHolderTypeMainPanel<CaseType>(id, getObjectModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(final PageAdminObjectDetails<CaseType> parentPage) {
                List<ITab> tabs = super.createTabs(parentPage);

                if (matchCaseType(SystemObjectsType.ARCHETYPE_APPROVAL_CASE)) {
                    tabs.add(0,
                            new PanelTab(parentPage.createStringResource("PageCase.approvalTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_APPROVAL_URL, true, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new ApprovalCaseTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                                }
                            });
                } else if (matchCaseType(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST)) {
                    tabs.add(0,
                            new PanelTab(parentPage.createStringResource("PageCase.operationRequestTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_APPROVAL_URL, true, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new OperationRequestCaseTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                                }

                            });
                } else if (matchCaseType(SystemObjectsType.ARCHETYPE_MANUAL_CASE)) {
                    //todo manual case tab
                }
                tabs.add(
                        new CountablePanelTab(parentPage.createStringResource("PageCase.workitemsTab"),
                                getTabVisibility(ComponentConstants.UI_CASE_TAB_WORKITEMS_URL, false, parentPage)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return new CaseWorkitemsTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                            }

                            @Override
                            public String getCount() {
                                return Integer.toString(countWorkItems());
                            }
                        });

                if (matchCaseType(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST)){
                    tabs.add(
                            new PanelTab(parentPage.createStringResource("PageCase.childCasesTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_CHILD_CASES_URL, false, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new ChildCasesTabPanel(panelId, getMainForm(), getObjectModel());
                                }
                            });
                }

                tabs.add(
                        new CountablePanelTab(parentPage.createStringResource("PageCase.events"),
                                getTabVisibility(ComponentConstants.UI_CASE_TAB_EVENTS_URL, false, parentPage)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return new CaseEventsTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                            }

                            @Override
                            public String getCount() {
                                return Integer.toString(countEvents());
                            }
                        });
                return tabs;
            }

            @Override
            protected boolean getOptionsPanelVisibility() {
                return false;
            }

            @Override
            protected boolean isReadonly(){
                return true;
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageCase.title");
    }

//    private void savePerformed(AjaxRequestTarget target) {
//        LOGGER.debug("Saving case changes.");
//
//        OperationResult result = new OperationResult(OPERATION_SAVE_CASE);
//        Task task = createSimpleTask(OPERATION_SAVE_CASE);
//        try {
//            WebComponentUtil.revive(caseModel, getPrismContext());
//            ObjectWrapper<CaseType> wrapper = caseModel.getObject();
//            ObjectDelta<CaseType> delta = wrapper.getObjectDelta();
//            if (delta == null) {
//                return;
//            }
//            if (delta.isAdd()) {
//                CaseType object = delta.getObjectToAdd().asObjectable();
//                if (object.getName() == null || object.getName().getOrig().isEmpty()) {
//                    object.setName(new PolyStringType(OidUtil.generateOid()));
//                }
//                if (object.getState() == null || object.getState().isEmpty()) {
//                    object.setState("open");
//                }
//                createCaseWorkItems(object, task, result);
//            }
//            if (delta.getPrismContext() == null) {
//                getPrismContext().adopt(delta);
//            }
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Case delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
//            }
//
//            if (delta.isEmpty()) {
//                return;
//            }
//            WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
//
//            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//            deltas.add(delta);
//
//            getModelService().executeChanges(deltas, null, task, result);
//            result.recomputeStatus();
//        } catch (Exception ex) {
//            result.recordFatalError("Couldn't save case.", ex);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save case", ex);
//        }
//
//        if (!result.isSuccess()) {
//            showResult(result);
//            target.add(getFeedbackPanel());
//        } else {
//            showResult(result);
//
//            redirectBack();
//        }
//    }

//    private void createCaseWorkItems(CaseType caseInstance, Task task, OperationResult result) {
//        PrismObject<ResourceType> resource;
//        ObjectReferenceType resourceRef = caseInstance.getObjectRefOrClone();
//        if (resourceRef != null) {
//            String resourceOid = resourceRef.getOid();
//            resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceOid, PageCase.this, task, result);
//
//            if (resource != null) {
//                // If resource exists, create work items for each resource business operator
//                ResourceBusinessConfigurationType businessConfiguration = resource.asObjectable().getBusiness();
//                List<ObjectReferenceType> operators = new ArrayList<>();
//                if (businessConfiguration != null) {
//                    operators.addAll(businessConfiguration.getOperatorRef());
//                }
//                if (operators.isEmpty()) {
//                    operators.add(new ObjectReferenceType().oid(DEFAULT_OPERATOR_OID).type(UserType.COMPLEX_TYPE));
//                }
//                for (ObjectReferenceType operator : operators) {
//                    CaseWorkItemType workItem = new CaseWorkItemType(getPrismContext())
//                            .originalAssigneeRef(operator.clone())
//                            .assigneeRef(operator.clone())
//                            .name(caseInstance.getName().getOrig());
//                    caseInstance.getWorkItem().add(workItem);
//                    // TODO deadline and maybe other fields
//                }
//            }
//        }
//    }
//

    @Override
    protected ObjectSummaryPanel<CaseType> createSummaryPanel() {
        return new CaseSummaryPanel(ID_SUMMARY_PANEL, CaseType.class, Model.of(getObjectModel().getObject().getObject().asObjectable()), this);
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {

    }

    @Override
    public Class<CaseType> getCompileTimeClass() {
        return CaseType.class;
    }

    @Override
    protected CaseType createNewObject(){
        return new CaseType();
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageCases.class;
    }

    private boolean matchCaseType(SystemObjectsType archetypeType){
        CaseType caseObject = getObjectWrapper().getObject().asObjectable();
        if (caseObject == null || caseObject.getAssignment() == null){
            return false;
        }
        for (AssignmentType assignment : caseObject.getAssignment()){
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null && archetypeType.value().equals(targetRef.getOid())){
                return true;
            }
        }
        return false;
    }

   private int countWorkItems(){
        List<CaseWorkItemType> workItemsList = getObjectModel().getObject().getObject().asObjectable().getWorkItem();
        return workItemsList == null ? 0 : workItemsList.size();
    }

    private int countEvents(){
        List<CaseEventType> eventsList = getObjectModel().getObject().getObject().asObjectable().getEvent();
        return eventsList == null ? 0 : eventsList.size();
    }
}
