/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationColumnTypeConfigContext;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serial;

@ColumnType(identifier = "certItemTargetDisplayName",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "WorkItemsPanel.displayName", order = 50))
public class CertItemTargetDisplayNameColumn extends AbstractCertificationItemColumn {

    public CertItemTargetDisplayNameColumn(GuiObjectColumnType columnConfig, CertificationColumnTypeConfigContext context) {
        super(columnConfig, context);
    }

    @Override
    public IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createColumn() {
        return new AbstractColumn<>(getColumnLabelModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AccessCertificationWorkItemType>>> item,
                    String componentId, IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(unwrapRowModel(rowModel));
                item.add(new Label(componentId, WebComponentUtil.getDisplayName(certCase.getTargetRef(), true)));
            }
        };
    }
}

