/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.admingui;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class RichHyperlinkListContentPanel extends MultivalueContainerListPanelWithDetailsPanel<RichHyperlinkType> {

    private IModel<PrismContainerWrapper<RichHyperlinkType>> model;

    public RichHyperlinkListContentPanel(String id, AssignmentHolderDetailsModel<?> model,
            ContainerPanelConfigurationType configurationType, ItemPath containerRealPath) {
        super(id, RichHyperlinkType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), containerRealPath, () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<RichHyperlinkType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<RichHyperlinkType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), RichHyperlinkType.F_LABEL,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<RichHyperlinkType>> model) {
                        RichHyperlinkListContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), RichHyperlinkType.F_TARGET_URL, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), ItemPath.create(RichHyperlinkType.F_ICON, IconType.F_CSS_CLASS), AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), RichHyperlinkType.F_COLOR, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<RichHyperlinkType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<RichHyperlinkType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<RichHyperlinkType>> item) {

        return new RichHyperlinkDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }
}
