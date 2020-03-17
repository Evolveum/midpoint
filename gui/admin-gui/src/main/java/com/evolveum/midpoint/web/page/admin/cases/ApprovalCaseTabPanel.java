/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.wf.DecisionsPanel;
import com.evolveum.midpoint.web.component.wf.SwitchableApprovalProcessPreviewsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.DecisionDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class ApprovalCaseTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ApprovalCaseTabPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CONNECTED_TASK = DOT_CLASS + "loadConnectedTask";

    private static final String ID_APPROVAL_CASE_PANEL = "approvalCasePanel";
    private static final String ID_HISTORY_CONTAINER = "historyContainer";
    private static final String ID_HISTORY_PANEL = "historyPanel";
    private static final String ID_HISTORY_HELP = "approvalHistoryHelp";
    private static final String ID_NAVIGATE_TO_TASK_LINK = "navigateToTaskLink";
    private static final String ID_NAVIGATE_TO_TASK_CONTAINER = "navigateToTaskContainer";

    public ApprovalCaseTabPanel(String id, Form<PrismObjectWrapper<CaseType>> mainForm, LoadableModel<PrismObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        SwitchableApprovalProcessPreviewsPanel approvalPanel = new SwitchableApprovalProcessPreviewsPanel(ID_APPROVAL_CASE_PANEL, Model.of(getObjectWrapper().getOid()),
                    Model.of(ApprovalContextUtil.isInStageBeforeLastOne(getObjectWrapper().getObject().asObjectable())), getPageBase());
        approvalPanel.setOutputMarkupId(true);
        approvalPanel.add(new VisibleBehaviour(() -> CaseTypeUtil.approvalSchemaExists(getObjectWrapper().getObject().asObjectable())));
        add(approvalPanel);

        List<DecisionDto> decisionList = calculateDecisionList();
        WebMarkupContainer historyContainer = new WebMarkupContainer(ID_HISTORY_CONTAINER);
        historyContainer.setOutputMarkupId(true);
        historyContainer.add(WebComponentUtil.createHelp(ID_HISTORY_HELP));
        historyContainer.add(new VisibleBehaviour(() -> !CollectionUtils.isEmpty(decisionList)));
        add(historyContainer);

        DecisionsPanel historyPanel = new DecisionsPanel(ID_HISTORY_PANEL,
                Model.ofList(decisionList), UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL,
                        (int) getPageBase().getItemsPerPage(UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL));
        historyPanel.setOutputMarkupId(true);
        historyContainer.add(historyPanel);

        PrismObject<TaskType> executingChangesTask = WebComponentUtil.getCaseExecutingChangesTask(OPERATION_LOAD_CONNECTED_TASK,
                getObjectWrapper().getOid(), ApprovalCaseTabPanel.this.getPageBase());
        WebMarkupContainer taskLinkContainer = new WebMarkupContainer(ID_NAVIGATE_TO_TASK_CONTAINER);
        taskLinkContainer.setOutputMarkupId(true);
        taskLinkContainer.add(new VisibleBehaviour(() -> executingChangesTask != null));
        add(taskLinkContainer);

        AjaxButton redirectToTaskLink = new AjaxButton(ID_NAVIGATE_TO_TASK_LINK,
                Model.of(WebComponentUtil.getDisplayNameOrName(executingChangesTask, true))) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                ObjectReferenceType taskRef = null;
                if (executingChangesTask != null) {
                    taskRef = new ObjectReferenceType();
                    taskRef.setOid(executingChangesTask.getOid());
                    taskRef.setType(TaskType.COMPLEX_TYPE);
                }
                if (StringUtils.isNotEmpty(taskRef.getOid())) {
                    WebComponentUtil.dispatchToObjectDetailsPage(taskRef, ApprovalCaseTabPanel.this, false);
                }
            }
        };
        redirectToTaskLink.setOutputMarkupId(true);
        taskLinkContainer.add(redirectToTaskLink);
    }

    public List<DecisionDto> calculateDecisionList() {
        List<DecisionDto> rv = new ArrayList<>();
        CaseType parentCase = getObjectWrapper().getObject().asObjectable();
        if (parentCase == null) {
            return rv;
        }

        if (parentCase.getEvent() != null && !parentCase.getEvent().isEmpty()) {
            parentCase.getEvent().forEach(e -> CollectionUtils.addIgnoreNull(rv, DecisionDto.create(e, ApprovalCaseTabPanel.this.getPageBase())));
        } else {
            //ItemApprovalProcessStateType instanceState = WfContextUtil.getItemApprovalProcessInfo(parentCase.getApprovalContext());
//            if (instanceState != null) {
//                //todo where we can take decisions now?
////                instanceState.getDecisions().forEach(d -> CollectionUtils.addIgnoreNull(rv, DecisionDto.create(d)));
//            }
        }
        return rv;
    }

}
