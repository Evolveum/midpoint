package com.evolveum.midpoint.gui.impl.component.wizard;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.search.SearchContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class MultiSelectTileWizardStepPanel<SI extends Serializable, O extends ObjectType, ODM extends ObjectDetailsModels, V extends Containerable>
        extends SelectTileWizardStepPanel<O, ODM, V> {

    private static final String ID_TABLE = "table";

    public MultiSelectTileWizardStepPanel(ODM model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MultiSelectTileTablePanel<SI, O> tilesTable =
                new MultiSelectTileTablePanel<>(
                        ID_TABLE,
                        getDefaultViewToggle(),
                        UserProfileStorage.TableId.PANEL_ACCESS_WIZARD_STEP) {

                    @Override
                    protected void deselectItem(SI entry) {
                        MultiSelectTileWizardStepPanel.this.deselectItem(entry);
                    }

                    @Override
                    protected IModel<String> getItemLabelModel(SI entry) {
                        return MultiSelectTileWizardStepPanel.this.getItemLabelModel(entry);
                    }

                    @Override
                    protected boolean isSelectedItemsPanelVisible() {
                        return MultiSelectTileWizardStepPanel.this.isSelectedItemsPanelVisible();
                    }

                    @Override
                    protected IModel<List<SI>> getSelectedItemsModel() {
                        return MultiSelectTileWizardStepPanel.this.getSelectedItemsModel();
                    }

                    @Override
                    protected void processSelectOrDeselectItem(SelectableBean<O> value, AjaxRequestTarget target) {
                        MultiSelectTileWizardStepPanel.this.processSelectOrDeselectItem(value, target);
                    }

                    @Override
                    protected ObjectQuery getCustomQuery() {
                        return MultiSelectTileWizardStepPanel.this.getCustomQuery();
                    }

                    @Override
                    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
                        return MultiSelectTileWizardStepPanel.this.getSearchOptions();
                    }

                    @Override
                    protected SearchContext getAdditionalSearchContext() {
                        return MultiSelectTileWizardStepPanel.this.getAdditionalSearchContext();
                    }

                    @Override
                    protected boolean isFullTextSearchEnabled() {
                        return MultiSelectTileWizardStepPanel.this.isFullTextSearchEnabled();
                    }

                    @Override
                    protected ContainerPanelConfigurationType getContainerConfiguration() {
                        return MultiSelectTileWizardStepPanel.this.getContainerConfiguration(getPanelType());
                    }

                    @Override
                    protected Class<O> getType() {
                        return MultiSelectTileWizardStepPanel.this.getType();
                    }

                    @Override
                    protected SelectableBeanObjectDataProvider<O> createProvider() {
                        return MultiSelectTileWizardStepPanel.this.createProvider(super.createProvider());
                    }

                    @Override
                    protected TemplateTile<SelectableBean<O>> createTileObject(SelectableBean<O> object) {
                        TemplateTile<SelectableBean<O>> tile = super.createTileObject(object);
                        MultiSelectTileWizardStepPanel.this.customizeTile(object, tile);
                        return tile;
                    }

                    @Override
                    protected void customizeNewRowItem(SelectableBean<O> value) {
                        MultiSelectTileWizardStepPanel.this.customizeTile(value, null);
                    }

                    @Override
                    public void refresh(AjaxRequestTarget target) {
                        super.refresh(target);
                        refreshSubmitAndNextButton(target);
                        target.add(this);
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return MultiSelectTileWizardStepPanel.this.isTogglePanelVisible();
                    }

                    @Override
                    protected List<IColumn<SelectableBean<O>, String>> createColumns() {
                        return MultiSelectTileWizardStepPanel.this.createColumns();
                    }

                    @Override
                    protected WebMarkupContainer createTableButtonToolbar(String id) {
                        return MultiSelectTileWizardStepPanel.this.createTableButtonToolbar(id);
                    }

                    @Override
                    protected boolean skipSearch() {
                        return MultiSelectTileWizardStepPanel.this.skipSearch();
                    }
                };
        tilesTable.setOutputMarkupId(true);
        add(tilesTable);
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

    protected void processSelectOrDeselectItem(SelectableBean<O> value, AjaxRequestTarget target) {

    }

    protected abstract IModel<List<SI>> getSelectedItemsModel();

    protected abstract IModel<String> getItemLabelModel(SI entry);

    protected abstract void deselectItem(SI entry);

    protected boolean isSelectedItemsPanelVisible() {
        return false;
    }

    protected void customizeTile(SelectableBean<O> object, TemplateTile<SelectableBean<O>> tile) {
    }

    protected SelectableBeanObjectDataProvider<O> createProvider(SelectableBeanObjectDataProvider<O> defaultProvider) {
        return defaultProvider;
    }
}
