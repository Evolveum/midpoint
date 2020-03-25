/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    private static final String OPERATION_LOAD_CONNECTED_TASK = DOT_CLASS + "loadConnectedTask";

    private static final String ID_SUMMARY_PANEL = "summaryPanel";

    public PageCase() {
        this(null, true);
    }

    public PageCase(PrismObject<CaseType> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject, true);
    }

    public PageCase(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null, false, true);
    }


    @Override
    protected AbstractObjectMainPanel<CaseType> createMainPanel(String id) {
        return new AssignmentHolderTypeMainPanel<CaseType>(id, getObjectModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(final PageAdminObjectDetails<CaseType> parentPage) {
                List<ITab> tabs = super.createTabs(parentPage);

                if (WebComponentUtil.hasArchetypeAssignment(getCase(), SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value())
                        && CaseTypeUtil.approvalSchemaExists(getObject() != null ? getObject().asObjectable() : null)) {
                    tabs.add(0,
                            new PanelTab(parentPage.createStringResource("PageCase.approvalTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_APPROVAL_URL, true, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new ApprovalCaseTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                                }
                            });
                } else if (WebComponentUtil.hasArchetypeAssignment(getCase(), SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())) {
                    tabs.add(0,
                            new PanelTab(parentPage.createStringResource("PageCase.operationRequestTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_APPROVAL_URL, true, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new OperationRequestCaseTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                                }

                            });
                } else if (WebComponentUtil.hasArchetypeAssignment(getCase(), SystemObjectsType.ARCHETYPE_MANUAL_CASE.value())) {
                    tabs.add(0,
                            new PanelTab(parentPage.createStringResource("PageCase.manualOperationDetailsTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_MANUAL_OPERATION_DETAILS_URL, true, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new ManualCaseTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                                }

                            });
                }
                if (!WebComponentUtil.hasArchetypeAssignment(getCase(), SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())) {

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
                }
                if (WebComponentUtil.hasArchetypeAssignment(getCase(), SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())){
                    tabs.add(
                            new CountablePanelTab(parentPage.createStringResource("PageCase.childCasesTab"),
                                    getTabVisibility(ComponentConstants.UI_CASE_TAB_CHILD_CASES_URL, false, parentPage)) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public WebMarkupContainer createPanel(String panelId) {
                                    return new ChildCasesTabPanel(panelId, getMainForm(), getObjectModel());
                                }

                                @Override
                                public String getCount() {
                                    return Integer.toString(countChildrenCases());
                                }
                            });
                }

                // commented now as it doesn't display informative data
//                tabs.add(
//                        new CountablePanelTab(parentPage.createStringResource("PageCase.events"),
//                                getTabVisibility(ComponentConstants.UI_CASE_TAB_EVENTS_URL, false, parentPage)) {
//
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            public WebMarkupContainer createPanel(String panelId) {
//                                return new CaseEventsTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
//                            }
//
//                            @Override
//                            public String getCount() {
//                                return Integer.toString(countEvents());
//                            }
//                        });
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

    @Override
    protected ObjectSummaryPanel<CaseType> createSummaryPanel(IModel<CaseType> summaryModel) {
        return new CaseSummaryPanel(ID_SUMMARY_PANEL, CaseType.class, summaryModel, this);
    }

    protected void initOperationalButtons(RepeatingView repeatingView){
        OperationResult result = new OperationResult(OPERATION_LOAD_CONNECTED_TASK);
        ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF)
                .ref(getObjectWrapper().getOid())
                .build();
        List<PrismObject<TaskType>> connectedTasks = WebModelServiceUtils.searchObjects(TaskType.class, query, result, PageCase.this);
        final ObjectReferenceType taskRef = new ObjectReferenceType();
        if (CollectionUtils.isNotEmpty(connectedTasks)) {
            taskRef.setOid(connectedTasks.get(0).getOid());
            taskRef.setType(TaskType.COMPLEX_TYPE);
        }
        if (StringUtils.isNotEmpty(taskRef.getOid())) {
            AjaxButton navigateToTask = new AjaxButton(repeatingView.newChildId(), createStringResource("PageCase.navigateToTask")) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    WebComponentUtil.dispatchToObjectDetailsPage(taskRef, PageCase.this, false);
                }
            };
            navigateToTask.add(AttributeAppender.append("class", "btn-default"));
            repeatingView.add(navigateToTask);
        }
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
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

   private int countWorkItems(){
        List<CaseWorkItemType> workItemsList = getObjectModel().getObject().getObject().asObjectable().getWorkItem();
        return workItemsList == null ? 0 : workItemsList.size();
    }

    private int countChildrenCases(){
        CaseType currentCase = getObjectModel().getObject().getObject().asObjectable();
        ObjectQuery childrenCasesQuery = getPrismContext().queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF).ref(currentCase.getOid())
                .build();
        return WebModelServiceUtils.countObjects(CaseType.class, childrenCasesQuery, PageCase.this);
    }

    private int countEvents(){
        List<CaseEventType> eventsList = getObjectModel().getObject().getObject().asObjectable().getEvent();
        return eventsList == null ? 0 : eventsList.size();
    }

    private CaseType getCase() {
        return getObjectWrapper().getObject().asObjectable();
    }
}
