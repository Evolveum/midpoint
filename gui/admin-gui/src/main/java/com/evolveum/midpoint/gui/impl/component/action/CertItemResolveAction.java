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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.ResolveItemPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ActionType(
        identifier = "certItemResolve",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "CertificationItemsPanel.action.resolve", order = 6))
public class CertItemResolveAction extends AbstractCertItemDecisionAction {

    private static final String DOT_CLASS = CertItemResolveAction.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordCertItemAction";

    public CertItemResolveAction() {
        super();
    }

    public CertItemResolveAction(GuiActionType actionDto) {
        super(actionDto);
    }

    //todo should be unified with parent in future
    @Override
    protected void showActionConfigurationPanel(ContainerPanelConfigurationType panelConfig,
            List<AccessCertificationWorkItemType> workItems, PageBase pageBase, AjaxRequestTarget target) {
        ResolveItemPanel resolveItemPanel = new ResolveItemPanel(pageBase.getMainPopupBodyId(), Model.of(panelConfig)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void confirmPerformedWithDeltas(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
                confirmActionPerformed(target, workItems, deltas, pageBase);
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

    @Override
    protected AccessCertificationResponseType getResponse(AccessCertificationWorkItemType certItem) {
        return certItem != null && certItem.getOutput() != null ?
                OutcomeUtils.fromUri(certItem.getOutput().getOutcome()) : null;
    }

    @Override
    protected boolean isVisibleForRow(AccessCertificationWorkItemType certItem) {
        String itemResponse = certItem != null && certItem.getOutput() != null ? certItem.getOutput().getOutcome() : null;
        return certItem == null || itemResponse == null;
    }
}
