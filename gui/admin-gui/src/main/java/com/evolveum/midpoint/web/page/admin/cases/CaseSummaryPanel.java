/*
 * Copyright (Cs) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by honchar
 */
public class CaseSummaryPanel extends ObjectSummaryPanel<CaseType> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CaseSummaryPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_PARENT_CASE_DISPLAY_NAME = DOT_CLASS + "loadParentCaseDisplayName";

    public CaseSummaryPanel(String id, IModel<CaseType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, model, summaryPanelSpecificationType);
    }

    @Override
    protected IModel<String> getTitleModel() {
        return new LoadableModel<String>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String load() {
                ObjectReferenceType parentRef = getModelObject().getParentRef();
                if (parentRef != null && StringUtils.isNotEmpty(parentRef.getOid())) {
                    Date startDate = XmlTypeConverter.toDate(CaseTypeUtil.getStartTimestamp(getModelObject()));
                    if (startDate != null) {
                        return createStringResource("CaseSummaryPanel.started",
                                WebComponentUtil.getLocalizedDate(startDate, DateLabelComponent.MEDIUM_MEDIUM_STYLE)).getString();
                    }
                }
                return null;
            }
        };
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return new LoadableModel<String>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String load() {
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
    protected List<SummaryTag<CaseType>> getSummaryTagComponentList() {
        List<SummaryTag<CaseType>> summaryTagList = new ArrayList<>();
        SummaryTag<CaseType> tagOutcome = new SummaryTag<CaseType>(ID_SUMMARY_TAG, getModel()) {
            @Override
            protected void initialize(CaseType taskType) {

                CaseType aCase = getModelObject();
                if (!CaseTypeUtil.isApprovalCase(aCase) && !CaseTypeUtil.isManualProvisioningCase(aCase)) {
                    // TEMPORARY: avoiding problems with parsing of the outcome for correlation cases
                    return;
                }

                if (aCase.getOutcome() == null) {
                    // shouldn't occur!
                    return;
                }

                String icon, name;
                if (ApprovalUtils.approvalBooleanValueFromUri(aCase.getOutcome())) {
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
    protected String getDefaultIconCssClass() {
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
