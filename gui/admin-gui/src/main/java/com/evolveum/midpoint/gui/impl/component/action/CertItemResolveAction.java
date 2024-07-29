/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.ResolveItemPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

@ActionType(
        identifier = "certItemResolve",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "CertificationItemsPanel.action.resolve", order = 6))
public class CertItemResolveAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    private static final String DOT_CLASS = CertItemResolveAction.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordCertItemAction";

    public CertItemResolveAction() {
        super();
    }

    public CertItemResolveAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    protected void executeAction(List<AccessCertificationWorkItemType> workItems, PageBase pageBase, AjaxRequestTarget target) {
        ResolveItemPanel resolveItemPanel = new ResolveItemPanel(pageBase.getMainPopupBodyId()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void savePerformed(AjaxRequestTarget target, AccessCertificationResponseType response,
                    String comment) {
                OperationResult result = new OperationResult(OPERATION_RECORD_ACTION + "." + response.value());
                Task task = pageBase.createSimpleTask(OPERATION_RECORD_ACTION + "." + response.value());

                workItems.forEach(workItem -> {
                    OperationResult oneActionResult = result
                            .subresult(result.getOperation() + ".workItemId:" + workItem.getId())
                            .build();
                    CertMiscUtil.recordCertItemResponse(workItem, response, comment, oneActionResult, task, pageBase);
                });
                result.computeStatus();
                target.add(pageBase);
            }

            @Override
            protected List<AccessCertificationResponseType> getResponses() {
                return CertItemResolveAction.this.getResponses(workItems, pageBase);
            }
        };
        pageBase.showMainPopup(resolveItemPanel, target);
    }

    private List<AccessCertificationResponseType> getResponses(List<AccessCertificationWorkItemType> certItems, PageBase pageBase) {
        if (certItems != null && certItems.size() == 1) {
            AccessCertificationWorkItemType certItem = certItems.get(0);
            return getAvailableResponsesForCertItem(certItem, pageBase);
        }
        return loadAvailableResponses(pageBase);
    }

    protected List<AccessCertificationResponseType> getAvailableResponsesForCertItem(AccessCertificationWorkItemType certItem,
            PageBase pageBase) {
        AccessCertificationResponseType certItemResponse = getCertItemResponse(certItem);
        return loadAvailableResponses(pageBase)
                .stream()
                .filter(response -> certItemResponse != response)
                .collect(Collectors.toList());
    }

    private List<AccessCertificationResponseType> loadAvailableResponses(PageBase pageBase) {
        return new AvailableResponses(pageBase).getResponseValues();
    }

    private AccessCertificationResponseType getCertItemResponse(AccessCertificationWorkItemType certItem) {
        return certItem != null && certItem.getOutput() != null ?
                OutcomeUtils.fromUri(certItem.getOutput().getOutcome()) : null;
    }

}
