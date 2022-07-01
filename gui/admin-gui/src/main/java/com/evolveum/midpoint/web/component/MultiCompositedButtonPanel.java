/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.model.StringResourceModel;

public abstract class MultiCompositedButtonPanel extends BasePanel<List<CompositedIconButtonDto>> {

    private static final String ID_BUTTONS_PANEL = "additionalButtons";
    private static final String ID_BUTTON_DESCRIPTION = "buttonDescription";
    private static final String ID_ADDITIONAL_BUTTON = "additionalButton";

    public MultiCompositedButtonPanel(String id, IModel<List<CompositedIconButtonDto>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<CompositedIconButtonDto> buttonsPanel = new ListView<>(ID_BUTTONS_PANEL, getModel()) {

            @Override
            protected void populateItem(ListItem<CompositedIconButtonDto> item) {
                CompositedButtonPanel additionalButton = new CompositedButtonPanel(ID_ADDITIONAL_BUTTON, item.getModel()) {

                    @Override
                    protected void onButtonClicked(AjaxRequestTarget target, CompositedIconButtonDto buttonDescription) {
                        buttonClickPerformed(target, buttonDescription.getAssignmentObjectRelation(), buttonDescription.getCollectionView(), buttonDescription.getPage());
                    }
                };

//                AjaxCompositedIconButton additionalButton = new AjaxCompositedIconButton(ID_COMPOSITED_BUTTON, item.getModelService()) {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        buttonClickPerformed(target, item.getModelObject().getAssignmentObjectRelation(), item.getModelObject().getCollectionView(), item.getModelObject().getPage());
//                    }
//                };
                item.add(additionalButton);
//
//                item.add(new Label(ID_BUTTON_DESCRIPTION, getButtonDescription(item.getModelObject())));
            }
        };
        buttonsPanel.add(new VisibleBehaviour(() -> getModelObject() != null));
        add(buttonsPanel);
    }

    private String getButtonDescription(CompositedIconButtonDto button) {
        DisplayType displayType = button.getAdditionalButtonDisplayType();
        if (displayType.getSingularLabel() != null) {
            return WebComponentUtil.getTranslatedPolyString(displayType.getSingularLabel());
        }
        return WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
    }
    /**
     * this method should return the display properties for the last button on the dropdown  panel with additional buttons.
     * The last button is supposed to produce a default action (an action with no additional objects to process)
     */
//    protected abstract DisplayType getDefaultObjectButtonDisplayType();


    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
    }
}
