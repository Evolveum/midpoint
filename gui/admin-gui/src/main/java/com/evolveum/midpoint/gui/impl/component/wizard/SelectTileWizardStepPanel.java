package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class SelectTileWizardStepPanel<O extends ObjectType, ODM extends ObjectDetailsModels, V extends Containerable>
        extends AbstractWizardStepPanel<ODM> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectTileWizardStepPanel.class);

    private static final String ID_TITLE = "title";
    private static final String ID_ICON = "icon";
    static final String ID_TABLE = "table";

    public SelectTileWizardStepPanel(ODM model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        add(icon);
        add(new Label(ID_TITLE, getTitle()));
    }

    protected String getIcon() {
        return "fa fa-circle";
    }


    protected abstract ItemPath getPathForValueContainer();

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

    @Override
    public String getStepId() {
        return getPanelType();
    }

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
            performSelectedObjects();
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

//            getPageBase().error(text);
//            target.add(getFeedback());
            return false;
        }
        return true;
    }

    protected abstract void performSelectedObjects();

    protected <C extends Containerable> void performSelectedTile(
            String oid, QName typeName, PrismContainerValueWrapper<C> value) {
        try {
            PrismReferenceWrapper<Referencable> resourceRef =
                    value.findReference(getPathForTargetReference());
            resourceRef.getValue().setRealValue(
                    new ObjectReferenceType()
                            .oid(oid)
                            .type(typeName));
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
            performSelectedObjects();
            super.onSubmitPerformed(target);
        }
    }

    @Override
    protected boolean isSubmitEnable() {
        return !isMandatory() || !isNotSelected();
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleEnableBehaviour(
                () -> !isSubmitVisible(),
                () -> isSubmitEnable());
    }

    private boolean isNotSelected() {
        Optional<TemplateTile<SelectableBean<O>>> selectedTile =
                getTable().getTilesModel().getObject().stream().filter(tile -> tile.isSelected()).findFirst();
        return selectedTile.isEmpty();
    }

    protected boolean isMandatory() {
        return false;
    }

    IModel<ViewToggle> getDefaultViewToggle() {
        return isDefaultViewTile() ? Model.of(ViewToggle.TILE) : Model.of(ViewToggle.TABLE);
    }

    protected boolean isDefaultViewTile() {
        return true;
    }

    protected boolean isTogglePanelVisible() {
        return false;
    }

    protected List<IColumn<SelectableBean<O>, String>> createColumns() {
        return List.of();
    }

    protected void refreshSubmitAndNextButton(AjaxRequestTarget target) {
        target.add(getNext());
        target.add(getSubmit());
    }
}
