package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
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
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.Optional;

public abstract class SelectTileWizardStepPanel<O extends ObjectType, ODM extends ObjectDetailsModels, V extends Containerable>
        extends AbstractWizardStepPanel<ODM> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectTileWizardStepPanel.class);

    private static final String ID_TABLE = "table";

    private IModel<PrismContainerValueWrapper<V>> valueModel;

    public SelectTileWizardStepPanel(ODM model, IModel<PrismContainerValueWrapper<V>> valueModel) {
        super(model);
        initValueModel(valueModel);
    }

    public SelectTileWizardStepPanel(ODM model) {
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

    public void setValueModel(IModel<PrismContainerValueWrapper<V>> valueModel) {
        this.valueModel = valueModel;
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
            performSelectedTiles();
            return super.onNextPerformed(target);
        }
        return false;
    }

    private boolean isValid(AjaxRequestTarget target) {
        if (isMandatory() && isNotSelected()) {
            String key = "SelectTileWizardStepPanel.isMandatory";
            String typeLabel = WebComponentUtil.getLabelForType(getType(), false);
            String text = PageBase.createStringResourceStatic(key + ".text", typeLabel).getString();
            new Toast()
                    .error()
                    .title(PageBase.createStringResourceStatic(key).getString())
                    .icon("fas fa-circle-exclamation")
                    .autohide(true)
                    .delay(5_000)
                    .body(text)
                    .show(target);

            getPageBase().error(text);
            target.add(getFeedback());
            return false;
        }
        return true;
    }

    protected void performSelectedTiles() {
        Optional<TemplateTile<SelectableBean<O>>> selectedTile =
                getTable().getTilesModel().getObject().stream().filter(tile -> tile.isSelected()).findFirst();
        if (selectedTile.isPresent()) {
            performSelectedTile(selectedTile.get());
        } else {
            try {
                getValueModel().getObject().getParent().remove(getValueModel().getObject(), getPageBase());
                getValueModel().detach();
            } catch (SchemaException e) {
                LOGGER.error("Couldn't remove value from inducement container.");
            }
        }
    }

    protected void performSelectedTile(TemplateTile<SelectableBean<O>> selectedTile) {
        try {
            PrismReferenceWrapper<Referencable> resourceRef =
                    getValueModel().getObject().findReference(getPathForTargetReference());
            resourceRef.getValue().setRealValue(
                    new ObjectReferenceType()
                            .oid(selectedTile.getValue().getValue().getOid())
                            .type(selectedTile.getValue().getValue().asPrismObject().getDefinition().getTypeName()));
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find target reference.");
        }
    }

    protected ItemPath getPathForTargetReference(){
        return ItemPath.EMPTY_PATH;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            performSelectedTiles();
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
