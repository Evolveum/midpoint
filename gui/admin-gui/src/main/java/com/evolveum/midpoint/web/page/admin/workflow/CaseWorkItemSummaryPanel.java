/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.workflow;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AbstractSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.wf.WfGuiUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class CaseWorkItemSummaryPanel extends AbstractSummaryPanel<CaseWorkItemType> {
    private static final long serialVersionUID = -5077637168906420769L;

    private static final String DOT_CLASS = CaseWorkItemSummaryPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REQUESTOR_REFERENCE = DOT_CLASS + "loadRequestorReference";

    public CaseWorkItemSummaryPanel(String id, IModel<CaseWorkItemType> model) {
        super(id, model, null);
    }

    @Override
    protected List<SummaryTag<CaseWorkItemType>> getSummaryTagComponentList() {
        List<SummaryTag<CaseWorkItemType>> summaryTagList = new ArrayList<>();
        SummaryTag<CaseWorkItemType> isAssignedTag = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
            @Override
            protected void initialize(CaseWorkItemType workItem) {
                if (workItem.getAssigneeRef() != null) {
                    setIconCssClass("fa fa-fw fa-lock");
                    setLabel(getString("WorkItemSummaryPanel.allocated"));
                } else {
                    setIconCssClass("fa fa-fw fa-unlock");
                    setLabel(getString("WorkItemSummaryPanel.notAllocated"));
                }
            }
        };
        summaryTagList.add(isAssignedTag);
        return summaryTagList;
    }

    @Override
    protected IModel<String> getDisplayNameModel() {
        return () -> {
            CaseWorkItemType caseWorkItemType = CaseWorkItemSummaryPanel.this.getModelObject();
            CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
            return defaultIfNull(
                    WfGuiUtil.getLocalizedProcessName(caseType != null ? caseType.getApprovalContext() : null, CaseWorkItemSummaryPanel.this),
                    caseWorkItemType != null ? WebComponentUtil.getTranslatedPolyString(caseWorkItemType.getName()) : null);
        };
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() { // TODO
        return "summary-panel-task"; // TODO
    }

    @Override
    protected String getBoxAdditionalCssClass() { // TODO
        return "summary-panel-task"; // TODO
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }

    @Override
    protected String getTagBoxCssClass() {
        return "summary-tag-box";
    }

    @Override
    protected IModel<String> getTitleModel() {
        return () -> {
            CaseWorkItemType caseWorkItemType = CaseWorkItemSummaryPanel.this.getModelObject();
            CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
            Task task = CaseWorkItemSummaryPanel.this.getPageBase().createSimpleTask(OPERATION_LOAD_REQUESTOR_REFERENCE);
            OperationResult result = new OperationResult(OPERATION_LOAD_REQUESTOR_REFERENCE);
            PrismObject<UserType> requester = caseType != null ? WebModelServiceUtils.resolveReferenceNoFetch(caseType.getRequestorRef(),
                    CaseWorkItemSummaryPanel.this.getPageBase(), task, result) : null;
            if (requester == null) {
                // MID-4539 if we don't have authorization to see requester
                return getString("TaskSummaryPanel.requestedBy", getString("TaskSummaryPanel.unknown"));
            }

            String displayName = WebComponentUtil.getDisplayName(requester, true);
            String name = WebComponentUtil.getName(requester, true);
            if (displayName != null) {
                return getString("TaskSummaryPanel.requestedByWithFullName", displayName, name);
            } else {
                return getString("TaskSummaryPanel.requestedBy", name);
            }
        };
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return () -> {
            CaseWorkItemType workItem = getModelObject();
            return getString("TaskSummaryPanel.requestedOn",
                    WebComponentUtil.getLongDateTimeFormattedValue(CaseTypeUtil.getStartTimestamp(CaseTypeUtil.getCase(workItem)),
                            CaseWorkItemSummaryPanel.this.getPageBase()));
        };
    }
}
