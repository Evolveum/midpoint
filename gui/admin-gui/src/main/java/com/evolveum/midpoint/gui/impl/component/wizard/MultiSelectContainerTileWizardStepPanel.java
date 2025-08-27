/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;

import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectTileTablePanel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.Containerable;

public abstract class MultiSelectContainerTileWizardStepPanel<E extends Serializable, C extends Containerable, ODM extends ObjectDetailsModels>
        extends SelectTileWizardStepPanel<PrismContainerValueWrapper<C>, ODM> {

    public MultiSelectContainerTileWizardStepPanel(ODM model) {
        super(model);
    }

    @Override
    protected SingleSelectTileTablePanel createTable(String idTable) {
        MultiSelectContainerTileTablePanel<E, C> tilesTable =
                new MultiSelectContainerTileTablePanel<>(
                        idTable,
                        UserProfileStorage.TableId.PANEL_ACCESS_WIZARD_STEP,
                        getDefaultViewToggle(),
                        createValuesModel()) {

                    @Override
                    protected void deselectItem(E entry) {
                        MultiSelectContainerTileWizardStepPanel.this.deselectItem(entry);
                    }

                    @Override
                    protected IModel<String> getItemLabelModel(E entry) {
                        return MultiSelectContainerTileWizardStepPanel.this.getItemLabelModel(entry);
                    }

                    @Override
                    protected boolean isSelectedItemsPanelVisible() {
                        return MultiSelectContainerTileWizardStepPanel.this.isSelectedItemsPanelVisible();
                    }

                    @Override
                    protected IModel<List<E>> getSelectedItemsModel() {
                        return MultiSelectContainerTileWizardStepPanel.this.getSelectedItemsModel();
                    }

                    @Override
                    protected void processSelectOrDeselectItem(PrismContainerValueWrapper<C> value, ISortableDataProvider<PrismContainerValueWrapper<C>, String> provider, AjaxRequestTarget target) {
                        MultiSelectContainerTileWizardStepPanel.this.processSelectOrDeselectItem(
                                value, (MultivalueContainerListDataProvider<C>) provider, target);
                    }

                    @Override
                    public ObjectQuery getCustomQuery() {
                        return MultiSelectContainerTileWizardStepPanel.this.getCustomQuery();
                    }

                    @Override
                    public Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
                        return MultiSelectContainerTileWizardStepPanel.this.getSearchOptions();
                    }

                    @Override
                    protected SearchContext getAdditionalSearchContext() {
                        return MultiSelectContainerTileWizardStepPanel.this.getAdditionalSearchContext();
                    }

                    @Override
                    public ContainerPanelConfigurationType getContainerConfiguration() {
                        return MultiSelectContainerTileWizardStepPanel.this.getContainerConfiguration(getPanelType());
                    }

                    @Override
                    protected Class<C> getType() {
                        return MultiSelectContainerTileWizardStepPanel.this.getType();
                    }

                    @Override
                    protected MultivalueContainerListDataProvider<C> createProvider() {
                        return MultiSelectContainerTileWizardStepPanel.this.createProvider(super.createProvider());
                    }

                    @Override
                    protected TemplateTile<PrismContainerValueWrapper<C>> createTileObject(PrismContainerValueWrapper<C> object) {
                        return MultiSelectContainerTileWizardStepPanel.this.createTileObject(object);
                    }

                    @Override
                    protected void customizeNewRowItem(PrismContainerValueWrapper<C> value, Item<PrismContainerValueWrapper<C>> item) {
                        MultiSelectContainerTileWizardStepPanel.this.customizeRow(value);
                    }

                    @Override
                    public void refresh(AjaxRequestTarget target) {
                        super.refresh(target);
                        refreshSubmitAndNextButton(target);
                        target.add(this);
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return MultiSelectContainerTileWizardStepPanel.this.isTogglePanelVisible();
                    }

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns() {
                        return MultiSelectContainerTileWizardStepPanel.this.createColumns();
                    }

                    @Override
                    protected WebMarkupContainer createTableButtonToolbar(String id) {
                        return MultiSelectContainerTileWizardStepPanel.this.createTableButtonToolbar(id);
                    }

                    @Override
                    protected boolean skipSearch() {
                        return MultiSelectContainerTileWizardStepPanel.this.skipSearch();
                    }

                    @Override
                    protected Component createHeader(String id) {
                        return MultiSelectContainerTileWizardStepPanel.this.createTableHeader(id, super.createHeader(id));
                    }

                    @Override
                    protected VisibleEnableBehaviour getHeaderFragmentVisibility() {
                        return MultiSelectContainerTileWizardStepPanel.this.getHeaderFragmentVisibility();
                    }
                };
        tilesTable.setOutputMarkupId(true);
        return tilesTable;
    }

    protected VisibleEnableBehaviour getHeaderFragmentVisibility() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected Component createTableHeader(String id, Component header) {
        return header;
    }

    protected void customizeRow(PrismContainerValueWrapper<C> value) {
    }

    protected abstract TemplateTile<PrismContainerValueWrapper<C>> createTileObject(PrismContainerValueWrapper<C> object);

    protected abstract IModel<List<PrismContainerValueWrapper<C>>> createValuesModel();

    protected boolean skipSearch() {
        return false;
    }

    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    protected SearchContext getAdditionalSearchContext() {
        return new SearchContext();
    }

    protected void processSelectOrDeselectItem(PrismContainerValueWrapper<C> value, MultivalueContainerListDataProvider<C> provider, AjaxRequestTarget target) {

    }

    protected abstract IModel<List<E>> getSelectedItemsModel();

    protected abstract IModel<String> getItemLabelModel(E entry);

    protected abstract void deselectItem(E entry);

    protected boolean isSelectedItemsPanelVisible() {
        return false;
    }

    protected MultivalueContainerListDataProvider<C> createProvider(MultivalueContainerListDataProvider<C> defaultProvider) {
        return defaultProvider;
    }

    protected abstract Class<C> getType();
}
