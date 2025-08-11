/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import java.io.Serial;
import java.io.Serializable;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.Model;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class SmartObjectClassTilePanel<T extends Serializable> extends BasePanel<SmartObjectClassTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_NAME = "name";
    private static final String IDD_DESCRIPTION = "description";
    private static final String ID_COUNT_TITLE = "countTitle";
    private static final String ID_COUNT = "count";
    private static final String ID_VIEW_SCHEMA_LINK = "viewSchemaLink";
    private static final String ID_SELECT_CHECKBOX = "selectCheckbox";

    IModel<SelectableBean<ObjectClassWrapper>> selectedTileModel;

    public SmartObjectClassTilePanel(
            String id,
            IModel<SmartObjectClassTileModel<T>> model,
            IModel<SelectableBean<ObjectClassWrapper>> selectedTileModel) {
        super(id, model);
        this.selectedTileModel = selectedTileModel;
        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        if (atLeastOneSelected()) {
            selectIfNoneSelected();
        }

        applySelectionStyling();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initName();
        initDescription();
        initCountTitle();
        initCount();
        initViewSchemaLink();
        initSelectRadio();
    }

    private void initSelectRadio() {
        Radio<SelectableBean<ObjectClassWrapper>> radio = new Radio<>(ID_SELECT_CHECKBOX, Model.of(getModelObject().getValue()));
        radio.setOutputMarkupId(true);
        add(radio);
    }

    private void initName() {
        Label title = new Label(ID_NAME, getModelObject().getName());
        title.setOutputMarkupId(true);
        add(title);
    }

    private void initDescription() {
        Label description = new Label(IDD_DESCRIPTION, getModelObject().getDescription());
        description.setOutputMarkupId(true);
        add(description);
    }

    private void initCountTitle() {
        Label countTitle = new Label(ID_COUNT_TITLE,
                createStringResource("SuggestTilePanel.count.title"));
        countTitle.setOutputMarkupId(true);
        add(countTitle);
    }

    private void initCount() {
        Label count = new Label(ID_COUNT, buildCountValueLabel());
        count.setOutputMarkupId(true);
        add(count);
    }

    private void initViewSchemaLink() {
        AjaxLinkPanel viewSchemaLink = new AjaxLinkPanel(ID_VIEW_SCHEMA_LINK,
                createStringResource("SuggestTilePanel.view.schema")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onViewSchema(target);
            }
        };
        viewSchemaLink.setOutputMarkupId(true);
        add(viewSchemaLink);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeModifier.append(CLASS_CSS, "bg-white "
                + "d-flex flex-column align-items-center"
                + " rounded w-100 h-100 p-3 card-shadow"));

        add(AttributeModifier.append(STYLE_CSS, ""));
    }

    private void applySelectionStyling() {
        SelectableBean<ObjectClassWrapper> currentSelectionValue = selectedTileModel.getObject();
        SelectableBean<ObjectClassWrapper> thisTileValue = getModelObject().getValue();

        if (currentSelectionValue == null || thisTileValue == null) {
            return;
        }

        ObjectClassWrapper selectedValue = currentSelectionValue.getValue();
        ObjectClassWrapper tileValue = thisTileValue.getValue();

        if (selectedValue == null || tileValue == null) {
            return;
        }

        String defaultTileCss = getDefaultTileCss();
        String cssClass = selectedValue.equals(tileValue) ? defaultTileCss + " active" : defaultTileCss;
        add(AttributeModifier.replace(CLASS_CSS, cssClass));
    }

    private String buildCountValueLabel() {
        String count = getModelObject().getCount();
        if (count == null || count.isEmpty()) {
            return createStringResource("SuggestTilePanel.count.unknown").getString();
        }

        return "~" + count;
    }

    protected void onViewSchema(AjaxRequestTarget target) {
        // This method can be overridden to handle the view schema action
    }

    /**
     * Selects the tile if no other tile is currently selected.
     * This is useful for ensuring that at least one tile is selected
     * when the user interacts with the UI.
     */
    private void selectIfNoneSelected() {
        SelectableBean<ObjectClassWrapper> currentSelection = selectedTileModel.getObject();
        SelectableBean<ObjectClassWrapper> thisTile = getModelObject().getValue();

        if (currentSelection == null && thisTile != null) {
            selectedTileModel.setObject(thisTile);
        }
    }

    protected boolean atLeastOneSelected() {
        return true;
    }

    protected String getDefaultTileCss() {
        return "simple-tile selectable clickable-by-enter tile-panel d-flex flex-column align-items-center "
                + "rounded p-3 justify-content-center";
    }
}
