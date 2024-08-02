/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

//copy of DropdownButtonPanel, just applied for AbstractGuiAction
public abstract class ActionDropdownButtonPanel<C extends Containerable> extends BasePanel<List<AbstractGuiAction<C>>> {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_BUTTON_CONTAINER = "buttonContainer";
    private static final String ID_INFO = "info";
    private static final String ID_ICON = "icon";
    private static final String ID_CARET = "caret";
    private static final String ID_LABEL = "label";

    private static final String ID_DROPDOWN_MENU = "dropDownMenu";
    private static final String ID_ACTION_ITEM = "actionItem";
    private static final String ID_ACTION_ITEM_BODY = "actionItemBody";

    IModel<DisplayType> buttonDisplayModel;
    private C rowObject;

    public ActionDropdownButtonPanel(String id, IModel<DisplayType> buttonDisplayModel,
            IModel<List<AbstractGuiAction<C>>> itemsModel, C rowObject) {
        super(id, itemsModel);
        this.buttonDisplayModel = buttonDisplayModel;
        this.rowObject = rowObject;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
        buttonContainer.setOutputMarkupId(true);
        buttonContainer.add(AttributeAppender.append("class", getSpecialButtonClass()));
        buttonContainer.add(AttributeAppender.append("class", () -> hasToggleIcon() ? " dropdown-toggle " : ""));
        add(buttonContainer);

        IModel<String> infoModel = getButtonInfoModel();
        Label info = new Label(ID_INFO, infoModel);
        info.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(infoModel.getObject())));
        buttonContainer.add(info);

        IModel<String> labelModel = getButtonInfoModel();
        Label label = new Label(ID_LABEL, labelModel);
        label.setRenderBodyOnly(true);
        label.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(labelModel.getObject())));
        buttonContainer.add(label);

        IModel<String> iconCssModel = getButtonIconModel();
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeModifier.append("class", iconCssModel.getObject()));
        icon.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(iconCssModel.getObject())));
        buttonContainer.add(icon);

        WebMarkupContainer caret = new WebMarkupContainer(ID_CARET);
        buttonContainer.add(caret);

        WebMarkupContainer dropdownMenuContainer = new WebMarkupContainer(ID_DROPDOWN_MENU);
        dropdownMenuContainer.setOutputMarkupId(true);
        dropdownMenuContainer.add(AttributeAppender.append("class", getSpecialDropdownMenuClass()));
        add(dropdownMenuContainer);

        ListView<AbstractGuiAction<C>> li = new ListView<>(ID_ACTION_ITEM, getModel()) {

            @Override
            protected void populateItem(ListItem<AbstractGuiAction<C>> action) {
                populateActionItem(ID_ACTION_ITEM_BODY, action);
            }
        };

        dropdownMenuContainer.add(li);
    }

    protected boolean hasToggleIcon() {
        return true;
    }

    private IModel<String> getButtonInfoModel() {
        if (buttonDisplayModel == null) {
            return Model.of();
        }
        return Model.of(GuiDisplayTypeUtil.getHelp(buttonDisplayModel.getObject()));
    }

    private IModel<String> getButtonIconModel() {
        if (buttonDisplayModel == null) {
            return Model.of();
        }
        return Model.of(GuiDisplayTypeUtil.getIconCssClass(buttonDisplayModel.getObject()));
    }

    public WebMarkupContainer getButtonContainer() {
        return (WebMarkupContainer)get(ID_BUTTON_CONTAINER);
    }

    protected void populateActionItem(String componentId, ListItem<AbstractGuiAction<C>> actionItem) {
        actionItem.setRenderBodyOnly(true);

        ActionItemLinkPanel<C> actionItemPanel = new ActionItemLinkPanel<>(componentId, actionItem.getModel()) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected List<C> getObjectsToProcess() {
                    return ActionDropdownButtonPanel.this.getObjectsToProcess();
                }
        };
        actionItemPanel.setRenderBodyOnly(true);
        actionItem.add(actionItemPanel);
        actionItem.add(new VisibleEnableBehaviour(() -> actionItem.getModelObject().isVisible(rowObject)));
    }

    protected String getSpecialButtonClass() {
        return "btn-app";
    }

    protected String getSpecialDropdownMenuClass() {
        return "dropdown-menu-right";
    }

    protected abstract List<C> getObjectsToProcess();
}
