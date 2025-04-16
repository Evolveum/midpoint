/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;
import com.evolveum.midpoint.gui.impl.page.admin.certification.component.CertResponseDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationColumnTypeConfigContext;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

@ColumnType(identifier = "certItemDetailsLink",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "", order = 120))
public class CertItemDetailsLinkColumn extends AbstractCertificationItemColumn {

    public CertItemDetailsLinkColumn(GuiObjectColumnType columnConfig, CertificationColumnTypeConfigContext context) {
        super(columnConfig, context);
    }

    @Override
    public IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createColumn() {
        return new AjaxLinkColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                return createStringResource("CertificationItemsPanel.showDetails");
            }


            @Override
            public void onClick(AjaxRequestTarget target,
                    IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                showResponseDetailsPopup(target, rowModel);
            }

        };
    }

    private void showResponseDetailsPopup(AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
        PageBase pageBase = context.getPageBase();
        AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(unwrapRowModel(rowModel));
        if (certCase == null) {
            return;
        }
        PrismContainerValueWrapper<AccessCertificationCaseType> caseWrapper =
                new PrismContainerValueWrapperImpl<>(null, certCase.asPrismContainerValue(), ValueStatus.NOT_CHANGED);

        CertResponseDetailsPanel panel = new CertResponseDetailsPanel(pageBase.getMainPopupBodyId(),
                Model.of(caseWrapper),
                rowModel.getObject().getRealValue(),
                certCase.getStageNumber()) {

            @Serial private static final long serialVersionUID = 1L;

            protected List<AbstractGuiAction<AccessCertificationWorkItemType>> getAvailableActions(
                    AccessCertificationWorkItemType workItem) {
                AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(unwrapRowModel(rowModel));
                AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(certCase);
                String campaignOid = campaign != null ? campaign.getOid() : null;
                List<AccessCertificationResponseType> availableResponses = CertMiscUtil.gatherAvailableResponsesForCampaign(
                        campaignOid, pageBase);
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

    @Override
    public boolean isDefaultColumn() {
        return false;
    }
}
