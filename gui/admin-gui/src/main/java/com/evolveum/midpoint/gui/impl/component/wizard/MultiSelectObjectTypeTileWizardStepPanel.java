/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectObjectTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class MultiSelectObjectTypeTileWizardStepPanel<SI extends Serializable, O extends ObjectType, AHDM extends AssignmentHolderDetailsModel>
        extends SelectTileWizardStepPanel<SelectableBean<O>, AHDM> {

    public MultiSelectObjectTypeTileWizardStepPanel(AHDM model) {
        super(model);
    }

    @Override
    protected SingleSelectTileTablePanel createTable(String idTable) {
        MultiSelectObjectTileTablePanel<SI, O> tilesTable = new MultiSelectObjectTileTablePanel<>(
                ID_TABLE,
                getDefaultViewToggle(),
                UserProfileStorage.TableId.PANEL_ACCESS_WIZARD_STEP) {

            @Override
            protected void deselectItem(SI entry) {
                MultiSelectObjectTypeTileWizardStepPanel.this.deselectItem(entry);
            }

            @Override
            protected IModel<String> getItemLabelModel(SI entry) {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getItemLabelModel(entry);
            }

            @Override
            protected boolean isSelectedItemsPanelVisible() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.isSelectedItemsPanelVisible();
            }

            @Override
            protected IModel<List<SI>> getSelectedItemsModel() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getSelectedItemsModel();
            }

            @Override
            protected void processSelectOrDeselectItem(SelectableBean<O> value, ISortableDataProvider<SelectableBean<O>, String> provider, AjaxRequestTarget target) {
                MultiSelectObjectTypeTileWizardStepPanel.this.processSelectOrDeselectItem(
                        value, (SelectableBeanObjectDataProvider<O>) provider, target);
            }

            @Override
            public ObjectQuery getCustomQuery() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getCustomQuery();
            }

            @Override
            public Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getSearchOptions();
            }

            @Override
            protected SearchContext getAdditionalSearchContext() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getAdditionalSearchContext();
            }

            @Override
            public ContainerPanelConfigurationType getContainerConfiguration() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getContainerConfiguration(getPanelType());
            }

            @Override
            public Class<O> getType() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.getType();
            }

            @Override
            public SelectableBeanObjectDataProvider<O> createProvider() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.createProvider(super.createProvider());
            }

            @Override
            public @Nullable Set<O> initialSelectedObjects() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.initialSelectedObjects();
            }

            @Override
            protected TemplateTile<SelectableBean<O>> createTileObject(SelectableBean<O> object) {
                TemplateTile<SelectableBean<O>> tile = super.createTileObject(object);
                MultiSelectObjectTypeTileWizardStepPanel.this.customizeTile(object, tile);
                return tile;
            }

            @Override
            protected void customizeNewRowItem(SelectableBean<O> value, Item<SelectableBean<O>> item) {
                MultiSelectObjectTypeTileWizardStepPanel.this.customizeTile(value, null);
            }

            @Override
            public void refresh(AjaxRequestTarget target) {
                super.refresh(target);
                refreshSubmitAndNextButton(target);
                target.add(this);
            }

            @Override
            protected boolean isTogglePanelVisible() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.isTogglePanelVisible();
            }

            @Override
            protected List<IColumn<SelectableBean<O>, String>> createColumns() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.createColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                return MultiSelectObjectTypeTileWizardStepPanel.this.createTableButtonToolbar(id);
            }

            @Override
            public boolean skipSearch() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.skipSearch();
            }

            @Override
            protected boolean isFullTextSearchEnabled() {
                return MultiSelectObjectTypeTileWizardStepPanel.this.isFullTextSearchEnabled();
            }
        };
        tilesTable.setOutputMarkupId(true);
        return tilesTable;
    }

    protected boolean isFullTextSearchEnabled() {
        return true;
    }

    protected boolean skipSearch() {
        return false;
    }

    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    protected SearchContext getAdditionalSearchContext() {
        return new SearchContext();
    }

    protected void processSelectOrDeselectItem(SelectableBean<O> value, SelectableBeanObjectDataProvider<O> provider, AjaxRequestTarget target) {

    }

    protected abstract IModel<List<SI>> getSelectedItemsModel();

    protected abstract IModel<String> getItemLabelModel(SI entry);

    protected abstract void deselectItem(SI entry);

    protected boolean isSelectedItemsPanelVisible() {
        return false;
    }

    protected void customizeTile(SelectableBean<O> object, TemplateTile<SelectableBean<O>> tile) {
    }

    protected @Nullable Set<O> initialSelectedObjects() {
        return null;
    }

    protected SelectableBeanObjectDataProvider<O> createProvider(SelectableBeanObjectDataProvider<O> defaultProvider) {
        return defaultProvider;
    }

    protected Class<O> getType() {
        return (Class<O>) ObjectType.class;
    }

    @Override
    protected String userFriendlyNameOfSelectedObject(String key) {
        String typeLabel = WebComponentUtil.getLabelForType(getType(), false);
        String text = LocalizationUtil.translate(key + ".text", new Object[] { typeLabel });
        return "";
    }
}
