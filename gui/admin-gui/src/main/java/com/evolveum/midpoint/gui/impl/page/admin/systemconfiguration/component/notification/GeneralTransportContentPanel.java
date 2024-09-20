/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class GeneralTransportContentPanel<T extends GeneralTransportConfigurationType>
        extends MultivalueContainerListPanelWithDetailsPanel<T> {

    private IModel<PrismContainerWrapper<T>> model;

    public GeneralTransportContentPanel(String id, AssignmentHolderDetailsModel<?> model,
            ContainerPanelConfigurationType configurationType, Class<T> clazz, ItemName messageTransportItem) {

        super(id, clazz, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(),
                ItemPath.create(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION, messageTransportItem),
                () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<T>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<T>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), GeneralTransportConfigurationType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<T>> model) {
                        GeneralTransportContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), GeneralTransportConfigurationType.F_REDIRECT_TO_FILE,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), GeneralTransportConfigurationType.F_LOG_TO_FILE,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), GeneralTransportConfigurationType.F_DEBUG,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<T>> getContainerModel() {
        return model;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
