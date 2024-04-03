/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for kind of secrets providers panel that contains table for multivalue container
 */
public abstract class SecretProvidersContentPanel<SPT extends SecretsProviderType> extends MultivalueContainerListPanelWithDetailsPanel<SPT> {

    private IModel<PrismContainerWrapper<SPT>> model;

    public SecretProvidersContentPanel(
            String id,
            AssignmentHolderDetailsModel model,
            ContainerPanelConfigurationType configurationType,
            Class<SPT> clazz,
            ItemName secretProviderSuffixPath) {
        super(id, clazz, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(
                model.getObjectWrapperModel(),
                ItemPath.create(SystemConfigurationType.F_SECRETS_PROVIDERS, secretProviderSuffixPath),
                (SerializableSupplier<PageBase>) () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<SPT>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<SPT>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<SPT>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SPT, String>(getContainerModel(), FileSecretsProviderType.F_IDENTIFIER,
                AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

            @Override
            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<SPT>> model) {
                SecretProvidersContentPanel.this.itemDetailsPerformed(target, model);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getContainerModel(),
                ItemPath.create(FileSecretsProviderType.F_DISPLAY, DisplayType.F_LABEL),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        addCustomColumns(columns);
        return columns;
    }

    protected void addCustomColumns(List<IColumn<PrismContainerValueWrapper<SPT>, String>> columns) {
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<SPT>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<SPT> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<SPT>> item) {

        return new SecretProviderDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
