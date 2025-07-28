package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collection;
import java.util.List;

public abstract class SingleTileWizardStepPanel<O extends ObjectType, ODM extends ObjectDetailsModels, V extends Containerable>
        extends SelectTileWizardStepPanel<O, ODM, V> {

    private static final Trace LOGGER = TraceManager.getTrace(SingleTileWizardStepPanel.class);

    private IModel<PrismContainerValueWrapper<V>> valueModel;

    public SingleTileWizardStepPanel(ODM model, IModel<PrismContainerValueWrapper<V>> valueModel) {
        super(model);
        initValueModel(valueModel);
    }

    public SingleTileWizardStepPanel(ODM model) {
        super(model);
        initValueModel(null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initValueModel(IModel<PrismContainerValueWrapper<V>> valueModel) {
        if (valueModel == null) {
            this.valueModel = createValueModel();
        } else {
            this.valueModel = valueModel;
        }
    }

    protected PrismContainerValue<V> createNewValue(PrismContainerWrapper<V> parent) {
        return parent.getItem().createNewValue();
    }

    protected IModel<PrismContainerValueWrapper<V>> createValueModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerValueWrapper<V> load() {

                ItemPath path = getPathForValueContainer();
                try {
                    PrismContainerWrapper<V> container =
                            getDetailsModel().getObjectWrapper().findContainer(path);
                    if (container == null) {
                        return null;
                    }
                    PrismContainerValue<V> newValue = createNewValue(container);
                    PrismContainerValueWrapper<V> valueWrapper = WebPrismUtil.createNewValueWrapper(
                            container, newValue, getPageBase(), getDetailsModel().createWrapperContext());
                    container.getValues().add(valueWrapper);
                    return valueWrapper;
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find parent container with path " + path + " in " + getDetailsModel().getObjectWrapper());
                }
                return null;
            }
        };
    }

    public void setValueModel(IModel<PrismContainerValueWrapper<V>> valueModel) {
        this.valueModel = valueModel;
    }

    protected void initLayout() {
        SingleSelectTileTablePanel<O> tilesTable =
                new SingleSelectTileTablePanel<>(
                        ID_TABLE,
                        getDefaultViewToggle(),
                        UserProfileStorage.TableId.PANEL_ACCESS_WIZARD_STEP) {

                    @Override
                    protected ObjectQuery getCustomQuery() {
                        return SingleTileWizardStepPanel.this.getCustomQuery();
                    }

                    @Override
                    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
                        return SingleTileWizardStepPanel.this.getSearchOptions();
                    }

                    @Override
                    protected ContainerPanelConfigurationType getContainerConfiguration() {
                        return SingleTileWizardStepPanel.this.getContainerConfiguration(getPanelType());
                    }

                    @Override
                    protected Class<O> getType() {
                        return SingleTileWizardStepPanel.this.getType();
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return SingleTileWizardStepPanel.this.isTogglePanelVisible();
                    }

                    @Override
                    protected List<IColumn<SelectableBean<O>, String>> createColumns() {
                        return SingleTileWizardStepPanel.this.createColumns();
                    }

                    @Override
                    public void refresh(AjaxRequestTarget target) {
                        super.refresh(target);
                        refreshSubmitAndNextButton(target);
                    }
                };
        add(tilesTable);
    }

    protected void performSelectedObjects() {
        if (!((SelectableBeanDataProvider) getTable().getProvider()).getSelected().isEmpty()) {
            O selectedValue = (O) ((SelectableBeanDataProvider) getTable().getProvider()).getSelected().iterator().next();
            performSelectedTile(
                    selectedValue.getOid(),
                    selectedValue.asPrismObject().getDefinition().getTypeName(),
                    getValueModel().getObject());
        } else {
            try {
                if (getValueModel().getObject() != null
                        && getValueModel().getObject().getParent() != null) {
                    getValueModel().getObject().getParent().remove(getValueModel().getObject(), getPageBase());
                }
                getValueModel().detach();
            } catch (SchemaException e) {
                LOGGER.error("Couldn't remove value from inducement container.");
            }
        }
    }

    public IModel<PrismContainerValueWrapper<V>> getValueModel() {
        return valueModel;
    }
}
