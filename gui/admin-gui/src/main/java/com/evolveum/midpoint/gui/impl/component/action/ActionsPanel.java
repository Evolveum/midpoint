/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.List;

public abstract class ActionsPanel<C extends Containerable> extends BasePanel<List<AbstractGuiAction<C>>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_BUTTON = "button";
    private static final String ID_ACTIONS_DROPDOWN_PANEL = "actionsDropdownPanel";

    private C rowObject = null;

    public ActionsPanel(String id, IModel<List<AbstractGuiAction<C>>> model) {
        super(id, model);
    }

    public ActionsPanel(String id, IModel<List<AbstractGuiAction<C>>> model, C rowObject) {
        super(id, model);
        this.rowObject = rowObject;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<List<AbstractGuiAction<C>>> buttonsListModel = getButtonsListModel();
        ListView<AbstractGuiAction<C>> buttonsPanel = new ListView<AbstractGuiAction<C>>(ID_BUTTONS, buttonsListModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AbstractGuiAction<C>> listItem) {
                Component buttonPanel = createButtonComponent(ID_BUTTON, listItem.getModel());
                listItem.add(buttonPanel);
                listItem.add(new VisibleBehaviour(() -> listItem.getModelObject().isVisible(rowObject)));
            }
        } ;
        buttonsPanel.setOutputMarkupId(true);
        add(buttonsPanel);

        IModel<List<AbstractGuiAction<C>>> dropdownActionsListModel = getDropdownActionsListModel();
        ActionDropdownButtonPanel<C> actionsDropdownPanel = new ActionDropdownButtonPanel<>(ID_ACTIONS_DROPDOWN_PANEL,
                null, dropdownActionsListModel, rowObject) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return getButtonPanelClass();
            }

            @Override
            protected List<C> getObjectsToProcess() {
                return ActionsPanel.this.getObjectsToProcess();
            }
        };
        actionsDropdownPanel.add(new VisibleBehaviour(() -> !dropdownActionsListModel.getObject().isEmpty()));
        add(actionsDropdownPanel);
    }

    private IModel<List<AbstractGuiAction<C>>> getButtonsListModel() {
        return () -> getModelObject()
                .stream()
                .filter(action -> action.isButton() && action.isVisible(rowObject))
                .toList();
    }

    private IModel<List<AbstractGuiAction<C>>> getDropdownActionsListModel() {
        return () -> getModelObject()
                .stream()
                .filter(action -> !action.isButton() && action.isVisible(rowObject))
                .toList();
    }

    protected Component createButtonComponent(String componentId, IModel<AbstractGuiAction<C>> model) {
        CompositedIconBuilder builder = getIconCompositedBuilder(model.getObject());
        AjaxCompositedIconButton btn = new AjaxCompositedIconButton(componentId, builder.build(),
                () -> getButtonTitle(model.getObject())) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractGuiAction<C> action = model.getObject();
                action.onActionPerformed(getObjectsToProcess(), getPageBase(), target);
            }

            @Override
            protected boolean isHorizontalLayout() {
                return true;
            }
        };

        btn.add(AttributeAppender.append("class", this::getButtonPanelClass));
        btn.titleAsLabel(true);
        return btn;
    }

    protected String getButtonPanelClass() {
        return "btn btn-default btn-xs";
    }

    private CompositedIconBuilder getIconCompositedBuilder(AbstractGuiAction<C> action) {
        CompositedIconBuilder builder = new CompositedIconBuilder();
        String iconCss = GuiDisplayTypeUtil.getIconCssClass(action.getActionDisplayType());
        builder.setBasicIcon(iconCss, IconCssStyle.IN_ROW_STYLE);
        return builder;
    }

    private String getButtonTitle(AbstractGuiAction<C> action) {
        //todo in this case button title is used as button label; therefore return label value instead of tooltip
        return GuiDisplayTypeUtil.getTranslatedLabel(action.getActionDisplayType());
    }

    protected abstract List<C> getObjectsToProcess();
}
