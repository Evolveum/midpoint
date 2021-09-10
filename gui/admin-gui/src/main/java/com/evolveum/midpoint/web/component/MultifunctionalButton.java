/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by honchar
 */
public abstract class MultifunctionalButton extends BasePanel<MultiFunctinalButtonDto> {

    private static final String ID_MAIN_BUTTON = "mainButton";
    private static final String ID_BUTTON = "additionalButton";

    public MultifunctionalButton(String id, IModel<MultiFunctinalButtonDto> buttonDtos) {
        super(id, buttonDtos);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        AjaxCompositedIconButton mainButton = new AjaxCompositedIconButton(ID_MAIN_BUTTON, new PropertyModel<>(getModel(), MultiFunctinalButtonDto.F_MAIN_BUTTON)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!additionalButtonsExist()) {
                    buttonClickPerformed(target, null, null);
                }
            }
        };
        mainButton.add(AttributeAppender.append(" data-toggle", additionalButtonsExist() ? "dropdown" : ""));
//        if (!additionalButtonsExist()) {
            mainButton.add(new VisibleBehaviour(this::isMainButtonVisible));
//        }
        add(mainButton);

        MultiCompositedButtonPanel buttonsPanel = new MultiCompositedButtonPanel(ID_BUTTON, new PropertyModel<>(getModel(), MultiFunctinalButtonDto.F_ADDITIONAL_BUTTONS)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                MultifunctionalButton.this.buttonClickPerformed(target, relationSpec, collectionViews);
            }

        };
        buttonsPanel.setOutputMarkupId(true);
        buttonsPanel.add(new VisibleBehaviour(this::additionalButtonsExist));
        add(buttonsPanel);
    }

    /**
     * this method should return the display properties for the last button on the dropdown  panel with additional buttons.
     * The last button is supposed to produce a default action (an action with no additional objects to process)
     */
//    protected abstract DisplayType getDefaultObjectButtonDisplayType();

    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews) {
    }

    private boolean additionalButtonsExist() {
        return CollectionUtils.isNotEmpty(getModelObject().getAdditionalButtons());
    }

    protected boolean isMainButtonVisible() {
        return true;
    }

}
