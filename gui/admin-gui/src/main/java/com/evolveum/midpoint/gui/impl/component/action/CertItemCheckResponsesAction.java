/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertResponseDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.or0;

@ActionType(
        identifier = "certItemHistory",
        applicableForType = AccessCertificationWorkItemType.class,
        bulkAction = false,
        display = @PanelDisplay(label = "CertItemCheckResponsesAction.label", icon = "fa fa-check text-success", order = 10))
public class CertItemCheckResponsesAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    public CertItemCheckResponsesAction() {
        super();
    }

    public CertItemCheckResponsesAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    protected void executeAction(List<AccessCertificationWorkItemType> workItems, PageBase pageBase, AjaxRequestTarget target) {
        if (workItems.size() != 1) {
            return;
        }

        AccessCertificationWorkItemType workItem = workItems.get(0);
        AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(workItem);

        if (certCase == null) {
            return;
        }
        AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(certCase);
        if (campaign == null) {
            return;
        }

        OperationResult result = new OperationResult("loadCertCase");
        Task task = pageBase.createSimpleTask("loadCertCase");
        int currentStageNumber = or0(campaign.getStageNumber());

        ObjectQuery query = pageBase.getPrismContext().queryFor(AccessCertificationCaseType.class)
                .ownerId(campaign.getOid())
                .build();
        query.addFilter(createIdFilter(pageBase, certCase.getId()));
        query.addFilter(createStageFilter(pageBase, currentStageNumber));
        query.addFilter(createIterationFilter(pageBase, campaign.getIteration() == null ? 0 : campaign.getIteration()));

        List<AccessCertificationCaseType> loadedCases;
        PrismContainerValue<AccessCertificationCaseType> certCaseValue = null;
        try {
            loadedCases = WebModelServiceUtils.searchContainers(AccessCertificationCaseType.class, query, null, result, pageBase);
            if (!loadedCases.isEmpty()) {
                certCaseValue = loadedCases.get(0).asPrismContainerValue();
            } else {
                certCaseValue = certCase.asPrismContainerValue();
            }
        } catch (Exception e) {
            certCaseValue = certCase.asPrismContainerValue();
        }
        PrismContainerValueWrapper<AccessCertificationCaseType> caseWrapper =
                new PrismContainerValueWrapperImpl<>(null, certCaseValue, ValueStatus.NOT_CHANGED);
        CertResponseDetailsPanel panel = new CertResponseDetailsPanel(pageBase.getMainPopupBodyId(),
                Model.of(caseWrapper), workItem, currentStageNumber) {

            @Serial private static final long serialVersionUID = 1L;

            protected List<AbstractGuiAction<AccessCertificationWorkItemType>> getAvailableActions(
                    AccessCertificationWorkItemType workItem) {
                List<AccessCertificationResponseType> availableResponses = CertMiscUtil.gatherAvailableResponsesForCampaign(
                        campaign.getOid(), pageBase);
                if (workItem.getOutput() != null && workItem.getOutput().getOutcome() != null) {
                    AccessCertificationResponseType outcome = OutcomeUtils.fromUri(workItem.getOutput().getOutcome());
                    availableResponses.remove(outcome);
                }
                return availableResponses.stream()
                                .map(response -> CertMiscUtil.createAction(response, pageBase))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
            }
        };
        pageBase.showMainPopup(panel, target);
    }

    private ObjectFilter createIdFilter(PageBase pageBase, long caseId) {
        return pageBase.getPrismContext().queryFor(AccessCertificationCaseType.class)
                .id(caseId)
                .build()
                .getFilter();
    }

    private ObjectFilter createStageFilter(PageBase pageBase, int stageNumber) {
        return pageBase.getPrismContext().queryFor(AccessCertificationCaseType.class)
                .item(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_STAGE_NUMBER)
                .eq(stageNumber)
                .build()
                .getFilter();
    }

    private ObjectFilter createIterationFilter(PageBase pageBase, int iteration) {
        return pageBase.getPrismContext().queryFor(AccessCertificationCaseType.class)
                .item(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_ITERATION)
                .eq(iteration)
                .build()
                .getFilter();
    }
}
