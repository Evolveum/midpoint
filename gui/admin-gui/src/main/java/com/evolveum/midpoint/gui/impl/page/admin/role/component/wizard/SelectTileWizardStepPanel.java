package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchConfigurationWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.Optional;

public abstract class SelectTileWizardStepPanel<O extends ObjectType, ODM extends ObjectDetailsModels, V extends Containerable>
        extends AbstractWizardStepPanel<ODM> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectTileWizardStepPanel.class);

    private static final String ID_TABLE = "table";

    private IModel<PrismContainerValueWrapper<V>> valueModel;

    public SelectTileWizardStepPanel(ODM model) {
        super(model);
        initModels();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initModels() {
        valueModel = createValueModel();
    }

    protected IModel<PrismContainerValueWrapper<V>> createValueModel() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerValueWrapper<V> load() {

                ItemPath path = getPathForValueContainer();
                try {
                    PrismContainerWrapper<V> container =
                            getDetailsModel().getObjectWrapper().findContainer(path);
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

    protected PrismContainerValue<V> createNewValue(PrismContainerWrapper<V> parent) {
        return parent.getItem().createNewValue();
    }

    protected abstract ItemPath getPathForValueContainer();

    protected void initLayout() {
        SingleSelectTileTablePanel<O> tilesTable =
                new SingleSelectTileTablePanel<>(
                        ID_TABLE,
                        UserProfileStorage.TableId.PANEL_ACCESS_WIZARD_STEP) {

                    @Override
                    protected ObjectQuery getCustomQuery() {
                        return SelectTileWizardStepPanel.this.getCustomQuery();
                    }

                    @Override
                    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
                        return SelectTileWizardStepPanel.this.getSearchOptions();
                    }

                    @Override
                    protected ContainerPanelConfigurationType getContainerConfiguration() {
                        return SelectTileWizardStepPanel.this.getContainerConfiguration(getPanelType());
                    }

                    @Override
                    protected Class<O> getType() {
                        return SelectTileWizardStepPanel.this.getType();
                    }
                };
        add(tilesTable);
    }

    protected Class<O> getType() {
        return (Class<O>) ObjectType.class;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return null;
    }

    protected ObjectQuery getCustomQuery() {
        return null;
    }

    protected abstract String getPanelType();

    protected TileTablePanel<TemplateTile<SelectableBean<O>>, SelectableBean<O>> getTable() {
        return (TileTablePanel) get(ID_TABLE);
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-11";
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        if (isValid(target)){
            onSelectPerformed();
            return super.onNextPerformed(target);
        }
        return false;
    }

    private boolean isValid(AjaxRequestTarget target) {
        if (isMandatory() && isNotSelected()) {
            getPageBase().error("Selecting of object is mandatory");
            target.add(getFeedback());
            return false;
        }
        return true;
    }

    protected void onSelectPerformed() {
        Optional<TemplateTile<SelectableBean<O>>> selectedTile =
                getTable().getTilesModel().getObject().stream().filter(tile -> tile.isSelected()).findFirst();
        if (selectedTile.isPresent()) {
            try {
                PrismReferenceWrapper<Referencable> resourceRef =
                        getValueModel().getObject().findReference(getPathForTargetReference());
                resourceRef.getValue().setRealValue(
                        new ObjectReferenceType()
                                .oid(selectedTile.get().getValue().getValue().getOid())
                                .type(selectedTile.get().getValue().getValue().asPrismObject().getDefinition().getTypeName()));
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find target reference.");
            }
        } else {
            try {
                getValueModel().getObject().getParent().remove(getValueModel().getObject(), getPageBase());
                getValueModel().detach();
            } catch (SchemaException e) {
                LOGGER.error("Couldn't remove value from inducement container.");
            }
        }
    }

    protected ItemPath getPathForTargetReference(){
        return ItemPath.EMPTY_PATH;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            onSelectPerformed();
            super.onSubmitPerformed(target);
        }
    }

    public IModel<PrismContainerValueWrapper<V>> getValueModel() {
        return valueModel;
    }

    private boolean isNotSelected() {
        Optional<TemplateTile<SelectableBean<O>>> selectedTile =
                getTable().getTilesModel().getObject().stream().filter(tile -> tile.isSelected()).findFirst();
        return selectedTile.isEmpty();
    }

    protected boolean isMandatory() {
        return false;
    }
}
