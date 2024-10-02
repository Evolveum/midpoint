/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationGuiConfigContext;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;

@ColumnType(identifier = "certItemCommentIcon",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "", order = 90))
public class CertItemCommentIconColumn extends AbstractCertificationItemColumn {

    public CertItemCommentIconColumn(GuiObjectColumnType columnConfig, CertificationGuiConfigContext context) {
        super(columnConfig, context);
    }

    @Override
    public IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createColumn() {
        return new IconColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AccessCertificationWorkItemType>>> cellItem,
                    String componentId, IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                ImagePanel imagePanel = new ImagePanel(componentId, () -> getIconDisplayType(rowModel));
                imagePanel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(
                        LocalizationUtil.translatePolyString(getIconDisplayType(rowModel).getTooltip()))));
                cellItem.add(imagePanel);
            }

            @Override
            public DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                AccessCertificationWorkItemType wi = unwrapRowModel(rowModel);
                String comment = WorkItemTypeUtil.getComment(wi);
                return new DisplayType()
                        .tooltip(comment)
                        .icon(new IconType()
                                .cssClass("fa fa-comment")
                                .color("blue"));
            }
        };
    }
}

