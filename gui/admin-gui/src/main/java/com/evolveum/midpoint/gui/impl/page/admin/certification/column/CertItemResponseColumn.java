/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconWithLabelColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.ColumnTypeConfigContext;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;

@ColumnType(identifier = "certItemResponse",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertCampaign.statistics.response", order = 70))
public class CertItemResponseColumn extends AbstractCertificationItemColumn {

    public CertItemResponseColumn(GuiObjectColumnType columnConfig, ColumnTypeConfigContext context) {
        super(columnConfig, context);
    }

    @Override
    public IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createColumn() {
        return new CompositedIconWithLabelColumn<>(getColumnLabelModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected CompositedIcon getCompositedIcon(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                AccessCertificationResponseType response = getResponse(rowModel);
                if (response == null) {
                    return null;
                }
                DisplayType responseDisplayType = new CertificationItemResponseHelper(response).getResponseDisplayType();
                return new CompositedIconBuilder()
                        .setBasicIcon(responseDisplayType.getIcon(), IconCssStyle.IN_ROW_STYLE)
                        .build();
            }

            @Override
            public IModel<DisplayType> getLabelDisplayModel(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                AccessCertificationResponseType response = getResponse(rowModel);
                if (response == null) {
                    return Model.of(new DisplayType());
                }
                return Model.of(new CertificationItemResponseHelper(response).getResponseDisplayType());
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                AccessCertificationResponseType response = getResponse(rowModel);
                if (response == null) {
                    return Model.of("");
                }
                DisplayType responseDisplayType = new CertificationItemResponseHelper(response).getResponseDisplayType();
                return Model.of(LocalizationUtil.translatePolyString(responseDisplayType.getLabel()));
            }

            private AccessCertificationResponseType getResponse(
                    IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                String outcome = WorkItemTypeUtil.getOutcome(unwrapRowModel(rowModel));
                return OutcomeUtils.fromUri(outcome);
            }

        };
    }
}

