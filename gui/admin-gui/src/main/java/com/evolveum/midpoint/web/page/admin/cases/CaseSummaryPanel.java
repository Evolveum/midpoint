/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CaseSummaryPanel extends ObjectSummaryPanel<CaseType> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CaseSummaryPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_PARENT_CASE_DISPLAY_NAME = DOT_CLASS + "loadParentCaseDisplayName";

    public CaseSummaryPanel(String id, Class type, IModel<CaseType> model, ModelServiceLocator serviceLocator) {
        super(id, type, model, serviceLocator);
    }

    @Override
    protected IModel<String> getTitleModel() {
        return new LoadableModel<String>(){
            @Override
            public String load(){
                ObjectReferenceType parentRef = getModelObject().getParentRef();
                if (parentRef != null && StringUtils.isNotEmpty(parentRef.getOid())) {
                    Task task = getPageBase().createSimpleTask(OPERATION_LOAD_PARENT_CASE_DISPLAY_NAME);
                    OperationResult result = new OperationResult(OPERATION_LOAD_PARENT_CASE_DISPLAY_NAME);
                    PrismObject<CaseType> parentCaseObj = WebModelServiceUtils.loadObject(CaseType.class, parentRef.getOid(), getPageBase(),
                            task, result);
                    return createStringResource("CaseSummaryPanel.parentCase",
                            WebComponentUtil.getDisplayNameOrName(parentCaseObj)).getString();
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    protected List<SummaryTag<CaseType>> getSummaryTagComponentList(){
        List<SummaryTag<CaseType>> summaryTagList = new ArrayList<>();
        SummaryTag<CaseType> tagOutcome = new SummaryTag<CaseType>(ID_SUMMARY_TAG, getModel()) {
            @Override
            protected void initialize(CaseType taskType) {
                String icon, name;
                if (getModelObject().getOutcome() == null) {
                    // shouldn't occur!
                    return;
                }

                if (ApprovalUtils.approvalBooleanValueFromUri(getModelObject().getOutcome())) {
                    icon = ApprovalOutcomeIcon.APPROVED.getIcon();
                    name = "approved";
                } else {
                    icon = ApprovalOutcomeIcon.REJECTED.getIcon();
                    name = "rejected";
                }
                setIconCssClass(icon);
                setLabel(createStringResource("TaskSummaryPanel." + name).getString());
            }
        };
        tagOutcome.add(new VisibleBehaviour(() -> CaseSummaryPanel.this.getModelObject().getOutcome() != null));
        summaryTagList.add(tagOutcome);

        return summaryTagList;
    }

    @Override
    protected String getIconCssClass() {
        return GuiStyleConstants.EVO_CASE_OBJECT_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return null;
    }

   @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
