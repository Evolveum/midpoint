/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class SelectTileWizardStepPanel<O extends SelectableRow, AHDM extends AssignmentHolderDetailsModel>
        extends AbstractWizardStepPanel<AHDM> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectTileWizardStepPanel.class);

    private static final String ID_TITLE = "title";
    private static final String ID_ICON = "icon";

    private static final String ID_BODY_FRAGMENT = "bodyFragment";

    private static final String ID_BODY = "body";

    static final String ID_TABLE = "table";

    public SelectTileWizardStepPanel(AHDM model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(createFragment(ID_BODY));
    }

    protected Fragment createFragment(String id) {
        Fragment body = new Fragment(id, ID_BODY_FRAGMENT, SelectTileWizardStepPanel.this);
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        body.add(icon);
        body.add(new Label(ID_TITLE, getTitle()));
        body.add(createTable(ID_TABLE));
        return body;
    }

    protected abstract SingleSelectTileTablePanel createTable(String idTable);

    protected String getIcon() {
        return "fa fa-circle";
    }


    protected abstract ItemPath getPathForValueContainer();

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

    protected TileTablePanel<? extends Tile<O>, O> getTable() {
        return (TileTablePanel) get(getPageBase().createComponentPath(ID_BODY, ID_TABLE));
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-12";
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
            String text = userFriendlyNameOfSelectedObject(key);
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

    protected abstract String userFriendlyNameOfSelectedObject(String key);

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
        Optional<? extends Tile<O>> selectedTile =
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

    protected List<IColumn<O, String>> createColumns() {
        return List.of();
    }

    protected void refreshSubmitAndNextButton(AjaxRequestTarget target) {
        target.add(getNext());
        target.add(getSubmit());
    }
}
