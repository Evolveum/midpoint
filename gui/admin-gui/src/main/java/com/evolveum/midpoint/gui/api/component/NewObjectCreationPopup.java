/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.wicket.model.StringResourceModel;

public abstract class NewObjectCreationPopup extends BasePanel<List<CompositedIconButtonDto>> implements Popupable {

    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_NEW_OBJECT_SELECTION_BUTTON_PANEL = "newObjectSelectionButtonPanel";

    public NewObjectCreationPopup(String id, IModel<List<CompositedIconButtonDto>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MultiCompositedButtonPanel buttonPanel =
                new MultiCompositedButtonPanel(ID_NEW_OBJECT_SELECTION_BUTTON_PANEL, getModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                        NewObjectCreationPopup.this.buttonClickPerformed(target, relationSpec, collectionViews, page);
                    }
                };
        buttonPanel.setOutputMarkupId(true);
        add(buttonPanel);

        AjaxButton cancel = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("PageBase.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        add(cancel);
    }

    protected abstract void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec,
            CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page);

    protected void cancelPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return NewObjectCreationPopup.this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("").setDefaultValue("");
    }
}
