/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public abstract class MultiCompositedButtonPanel extends BasePanel<List<CompositedIconButtonDto>> {

    private static final String ID_BUTTON_PANEL = "additionalButton";
    private static final String ID_COMPOSITED_BUTTON = "compositedButton";


    public MultiCompositedButtonPanel(String id, IModel<List<CompositedIconButtonDto>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<CompositedIconButtonDto> buttonsPanel = new ListView<>(ID_BUTTON_PANEL, getModel()) {

            @Override
            protected void populateItem(ListItem<CompositedIconButtonDto> item) {

                AjaxCompositedIconButton additionalButton = new AjaxCompositedIconButton(ID_COMPOSITED_BUTTON, item.getModel()) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        buttonClickPerformed(target, item.getModelObject().getAssignmentObjectRelation(), item.getModelObject().getCollectionView());
                    }
                };
                item.add(additionalButton);
            }
        };
        buttonsPanel.add(new VisibleBehaviour(() -> getModelObject() != null));
        add(buttonsPanel);

    }

    /**
     * this method should return the display properties for the last button on the dropdown  panel with additional buttons.
     * The last button is supposed to produce a default action (an action with no additional objects to process)
     */
//    protected abstract DisplayType getDefaultObjectButtonDisplayType();


    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews) {
    }

}
