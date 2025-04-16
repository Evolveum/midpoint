/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationColumnTypeConfigContext;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.or0;

@ColumnType(identifier = "certItemReviewers",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertCampaign.table.reviewers", order = 60))
public class CertItemReviewersColumn extends AbstractCertificationItemColumn {

    public CertItemReviewersColumn(GuiObjectColumnType columnConfig, CertificationColumnTypeConfigContext context) {
        super(columnConfig, context);
    }

    @Override
    public IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createColumn() {
        return new ObjectReferenceColumn<>(getColumnLabelModel(), "") {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<List<ObjectReferenceType>> extractDataModel(
                    IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                PrismContainerValueWrapper<AccessCertificationWorkItemType> rowObj = rowModel.getObject();
                AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(rowObj.getRealValue());
                return () -> CertCampaignTypeUtil.getCurrentlyAssignedReviewers(unwrapRowModel(rowModel), or0(certCase.getStageNumber()));
            }
        };
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && context != null && context.isViewAllItems();
    }
}

