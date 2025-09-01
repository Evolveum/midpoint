/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.web.component.data.MenuMultiButtonPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author honchar
 * @author Viliam Repan (lazyman)
 * <p>
 */
public class InlineMenuButtonColumn<T extends Serializable> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 1L;

    protected List<InlineMenuItem> menuItems;

    private PageBase pageBase;

    public InlineMenuButtonColumn(List<InlineMenuItem> menuItems, PageBase pageBase) {
        super(null);
        this.menuItems = menuItems;
        this.pageBase = pageBase;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
            final IModel<T> rowModel) {
        Component panel = getPanel(componentId, rowModel, getNumberOfButtons(false), false);
        panel.add(new VisibleBehaviour(() -> isPanelVisible(false)));
        cellItem.add(panel);
    }

    @Override
    public Component getHeader(String componentId) {

        Component headerPanel = getPanel(componentId, null, getNumberOfButtons(true), true);
        headerPanel.add(new VisibleBehaviour(() -> isPanelVisible(true)));
        return headerPanel;
    }

    private Component getPanel(String componentId, IModel<T> rowModel,
            int numberOfButtons, boolean isHeaderPanel) {
        List<InlineMenuItem> filteredMenuItems = new ArrayList<>();
        for (InlineMenuItem menuItem : menuItems) {

            if (isHeaderPanel && !menuItem.isHeaderMenuItem()) {
                continue;
            }
            if (rowModel != null && menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction) {
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }

            if (menuItem.getVisibilityChecker() != null && !menuItem.getVisibilityChecker().isVisible(rowModel, isHeaderPanel)) {
                continue;
            }

            filteredMenuItems.add(menuItem);
        }

        List<ButtonInlineMenuItem> buttonMenuItems = new ArrayList<>();
        menuItems.forEach(menuItem -> {
            if (menuItem instanceof ButtonInlineMenuItem) {
                if (isHeaderPanel && !menuItem.isHeaderMenuItem() || !menuItem.getVisible().getObject()) {
                    return;
                }
                if (menuItem.getVisibilityChecker() != null && !menuItem.getVisibilityChecker().isVisible(rowModel, isHeaderPanel)) {
                    return;
                }
                buttonMenuItems.add((ButtonInlineMenuItem) menuItem);
            }
        });

        if (filteredMenuItems.isEmpty()) {
            Label label = new Label(componentId, pageBase.createStringResource("InlineMenuButtonColumn.header")); //this is hack, TODO: cleanup and refactor soif there aren't any inline (row) actions, nothing is displayed
            label.add(AttributeAppender.append("class", "sr-only"));
            return label;
        }

        return new MenuMultiButtonPanel<T>(componentId, rowModel, buttonMenuItems.size(), Model.ofList(filteredMenuItems)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component createButton(int index, String componentId, IModel<T> model) {
                CompositedIconBuilder builder = getIconCompositedBuilder(index, buttonMenuItems);
                AjaxCompositedIconButton btn = new AjaxCompositedIconButton(componentId, builder.build(), () -> getButtonTitle(index, buttonMenuItems)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setRowModelToButtonAction(rowModel, buttonMenuItems);
                        buttonMenuItemClickPerformed(index, buttonMenuItems, target);
                    }

                    @Override
                    protected boolean isHorizontalLayout() {
                        return true;
                    }

                    @Override
                    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);
                        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
                    }
                };

                btn.add(AttributeAppender.append("class", getInlineMenuItemCssClass(rowModel)));
                btn.add(new EnableBehaviour(() -> isButtonMenuItemEnabled(model)));
                btn.titleAsLabel(showButtonLabel(index, buttonMenuItems));
                btn.add(AttributeAppender.append("aria-label", getButtonTitle(index, buttonMenuItems)));
                btn.add(AttributeAppender.append("aria-pressed", "false"));
                return btn;
            }

            @Override
            protected String getAdditionalMultiButtonPanelCssClass() {
                return InlineMenuButtonColumn.this.getAdditionalMultiButtonPanelCssClass();
            }

            @Override
            protected String getDropDownButtonIcon() {
                return InlineMenuButtonColumn.this.getDropDownButtonIcon();
            }

            @Override
            protected String getSpecialButtonClass() {
                if (InlineMenuButtonColumn.this.getSpecialButtonClass() != null) {
                    return InlineMenuButtonColumn.this.getSpecialButtonClass();
                }
                return super.getSpecialButtonClass();
            }

            @Override
            protected void onBeforeClickMenuItem(AjaxRequestTarget target, InlineMenuItemAction action, IModel<? extends InlineMenuItem> item) {
                if (action instanceof ColumnMenuAction) {
                    if (!isHeaderPanel) {
                        ((ColumnMenuAction) action).setRowModel(rowModel);
                    } else {
                        ((ColumnMenuAction) action).setRowModel(null);
                    }
                }
            }
        };
    }

    protected String getAdditionalMultiButtonPanelCssClass() {
        return null;
    }

    protected boolean isButtonMenuItemEnabled(IModel<T> rowModel) {
        return true;
    }

    private void setRowModelToButtonAction(IModel<T> rowModel, List<ButtonInlineMenuItem> buttonMenuItems) {
        for (InlineMenuItem menuItem : buttonMenuItems) {
            if (menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction) {
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }
        }
    }

    private void buttonMenuItemClickPerformed(int id, List<ButtonInlineMenuItem> buttonMenuItems, AjaxRequestTarget target) {
        if (id >= buttonMenuItems.size()) {
            return;
        }

        ButtonInlineMenuItem menuItem = buttonMenuItems.get(id);
        InlineMenuItemAction action = menuItem.getAction();
        if (action == null) {
            return;
        }

        // TODO: getConfirmationMessageModel is called here and again in showConfirmationPopup, but these are not getters,
        //  but both create model (perhaps not very expensive, but it still seems like waste.
        if (menuItem.showConfirmationDialog() && menuItem.getConfirmationMessageModel() != null) {
            showConfirmationPopup(menuItem, target);
            return;
        }

        if (menuItem.isSubmit()) {
            action.onSubmit(target);
        } else {
            action.onClick(target);
        }
    }

    private void showConfirmationPopup(InlineMenuItem menuItem, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(pageBase.getMainPopupBodyId(),
                menuItem.getConfirmationMessageModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                menuItem.getAction().onClick(target);
            }

            @Override
            public String getCssClassForDialog() {
                return "mt-popup-under-header";
            }
        };
        pageBase.showMainPopup(dialog, target);
    }

    public String getButtonSizeCssClass() {
        return DoubleButtonColumn.ButtonSizeClass.EXTRA_SMALL.toString();
    }

    private CompositedIconBuilder getIconCompositedBuilder(int id, List<ButtonInlineMenuItem> buttonMenuItems) {
        if (id >= buttonMenuItems.size()) {
            return null;
        }
        return buttonMenuItems.get(id).getIconCompositedBuilder(); // + " fa-fw";
    }

    private String getButtonTitle(int id, List<ButtonInlineMenuItem> buttonMenuItems) {
        if (id >= buttonMenuItems.size()) {
            return null;
        }
        IModel<String> label = buttonMenuItems.get(id).getLabel();
        return label != null && label.getObject() != null ?
                label.getObject() : "";
    }

    protected int getNumberOfButtons(boolean isHeaderPanel) {
        int numberOfHeaderButtons = 0;
        for (InlineMenuItem inlineMenuItem : menuItems) {
            if (isHeaderPanel && !inlineMenuItem.isHeaderMenuItem()) {
                continue;
            }
            if (inlineMenuItem instanceof ButtonInlineMenuItem) {
                numberOfHeaderButtons++;
            }
        }
        return numberOfHeaderButtons;
    }

    private boolean isPanelVisible(boolean isHeaderPanel) {
        for (InlineMenuItem item : menuItems) {
            if (isHeaderPanel && (item.isHeaderMenuItem() || item.getAction() instanceof HeaderMenuAction)) {
                return true;
            }
            if (!isHeaderPanel && !(item.getAction() instanceof HeaderMenuAction)) {
                return true;
            }

        }
        return false;
    }

    private boolean showButtonLabel(int id, List<ButtonInlineMenuItem> buttonMenuItems) {
        if (id >= buttonMenuItems.size()) {
            return false;
        }
        return buttonMenuItems.get(id).isLabelVisible();
    }

    protected String getInlineMenuItemCssClass(IModel<T> rowModel) {
        return "btn btn-default btn-xs";
    }

    protected String getDropDownButtonIcon() {
        return null;
    }

    protected String getSpecialButtonClass() {
        return null;
    }
}
