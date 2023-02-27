/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.ChooseRelationPanel;
import com.evolveum.midpoint.web.component.*;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

public abstract class ChooseRelationPopup extends SimplePopupable<List<QName>> {

    private static final String ID_PANEL = "panel";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_SELECT_BUTTON = "selectButton";


    public ChooseRelationPopup(String id, IModel<List<QName>> model){
        super(
                id,
                model,
                getWidthBaseOnRelationCount(model),
                400,
                PageBase.createStringResourceStatic("ChooseRelationPopup.title"));
    }

    private static int getWidthBaseOnRelationCount(IModel<List<QName>> model) {
        int min = 600;
        if (model == null || model.getObject() == null || model.getObject().isEmpty()) {
            return min;
        }
        int calculated = model.getObject().size() * 200;

        if (calculated < min) {
            return min;
        }
        return calculated + 100;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        ChooseRelationPanel panel = new ChooseRelationPanel(
                ID_PANEL,
                getModel()) {
            @Override
            protected void onTileClick(AjaxRequestTarget target) {
                target.add(ChooseRelationPopup.this.get(ID_SELECT_BUTTON));
            }

            @Override
            protected void customizeTilePanel(TilePanel tp) {
                ChooseRelationPopup.this.customizeTilePanel(tp);
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ChooseRelationPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        add(cancelButton);

        AjaxButton selectButton = new AjaxButton(ID_SELECT_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSelectRelation(getPanel().getSelectedRelation(), target);
            }
        };
        selectButton.add(new EnableBehaviour(() -> getPanel().isSelectedRelation()));
        add(selectButton);
    }

    protected void customizeTilePanel(TilePanel tp) {
    }

    protected abstract void onSelectRelation(QName selectedRelation, AjaxRequestTarget target);

    private ChooseRelationPanel getPanel() {
        return (ChooseRelationPanel) get(ID_PANEL);
    }
}
