/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.MenuMultiButtonPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * @author honchar
 * @author Viliam Repan (lazyman)
 * <p>
 */
public class InlineMenuButtonColumn<T extends Serializable> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 1L;

    protected List<InlineMenuItem> menuItems = new ArrayList<>();

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
        for (InlineMenuItem menuItem : (rowModel != null && rowModel.getObject() instanceof InlineMenuable ?
                ((InlineMenuable)rowModel.getObject()).getMenuItems() : menuItems)){
            if (isHeaderPanel && !menuItem.isHeaderMenuItem()){
                continue;
            }
            if (rowModel != null && menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction){
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }
            filteredMenuItems.add(menuItem);
        }
        if (rowModel != null && rowModel.getObject() instanceof InlineMenuable &&
                ((InlineMenuable)rowModel.getObject()) != null){
            ((InlineMenuable) rowModel.getObject()).getMenuItems().clear();
            ((InlineMenuable) rowModel.getObject()).getMenuItems().addAll(filteredMenuItems);
        }

        List<ButtonInlineMenuItem> buttonMenuItems = new ArrayList<>();
        menuItems.forEach(menuItem -> {
            if (menuItem instanceof ButtonInlineMenuItem){
                if (isHeaderPanel && !menuItem.isHeaderMenuItem()){
                    return;
                }
                buttonMenuItems.add((ButtonInlineMenuItem) menuItem);
            }
        });

        return new MenuMultiButtonPanel<T>(componentId, rowModel, numberOfButtons, Model.ofList(filteredMenuItems)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected AjaxIconButton createButton(int index, String componentId, IModel<T> model) {
                AjaxIconButton btn = buildDefaultButton(componentId,
                        new Model<>(getButtonIconCss(index, buttonMenuItems)),
                        new Model<>(getButtonTitle(index, buttonMenuItems)),
                        new Model<>(getButtonCssClass()),
                        target -> {
                            setRowModelToButtonAction(rowModel, buttonMenuItems);
                            buttonMenuItemClickPerformed(index, buttonMenuItems, target);
                        });
                btn.showTitleAsLabel(false);
//                btn.add(new VisibleBehaviour(() -> isButtonVisible(index, model)));

                return btn;
            }
        };
    }

    private void setRowModelToButtonAction(IModel<T> rowModel, List<ButtonInlineMenuItem> buttonMenuItems) {
        for (InlineMenuItem menuItem : buttonMenuItems) {
            if (menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction) {
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }
        }
    }

    private void buttonMenuItemClickPerformed(int id, List<ButtonInlineMenuItem> buttonMenuItems, AjaxRequestTarget target) {
        if (id >= buttonMenuItems.size()){
            return;
        }
        ButtonInlineMenuItem menuItem = buttonMenuItems.get(id);
        if (menuItem.getAction() != null) {
            if (menuItem.showConfirmationDialog() && menuItem.getConfirmationMessageModel() != null) {
                showConfirmationPopup(menuItem, target);
            } else {
                if (menuItem.isSubmit()){
                    menuItem.getAction().onSubmit(target, null);
                } else {
                    menuItem.getAction().onClick(target);
                }
            }
        }
    }

    private void showConfirmationPopup(InlineMenuItem menuItem, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(pageBase.getMainPopupBodyId(),
                menuItem.getConfirmationMessageModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("pageUsers.message.confirmActionPopupTitle");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
            	menuItem.getAction().onClick(target);
            }
        };
        pageBase.showMainPopup(dialog, target);
    }



//    public boolean isButtonVisible(int id, IModel<T> model) {
//        return true;
//    }

    public String getButtonSizeCssClass() {
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.EXTRA_SMALL.toString();
    }

    private String getButtonCssClass() {
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        // Do not add color. It attracts too much attention
//        sb.append(getButtonColorCssClass(id, menuItems)).append(" ");
        sb.append("btn-default ");
        sb.append(getButtonSizeCssClass()).append(" ");

        return sb.toString();
    }

    private String getButtonIconCss(int id, List<ButtonInlineMenuItem> buttonMenuItems) {
        if (id >= buttonMenuItems.size()){
            return null;
        }
        return buttonMenuItems.get(id).getButtonIconCssClass() + " fa-fw";
    }

    private String getButtonTitle(int id, List<ButtonInlineMenuItem> buttonMenuItems) {
        if (id >= buttonMenuItems.size()){
            return null;
        }
        return buttonMenuItems.get(id).getLabel() != null && buttonMenuItems.get(id).getLabel().getObject() != null ?
                buttonMenuItems.get(id).getLabel().getObject() : "";
    }

    protected int getNumberOfButtons(boolean isHeaderPanel) {
        int numberOfHeaderButtons = 0;
        for (InlineMenuItem inlineMenuItem : menuItems){
            if (isHeaderPanel && !inlineMenuItem.isHeaderMenuItem()){
                continue;
            }
            if (inlineMenuItem instanceof ButtonInlineMenuItem){
                numberOfHeaderButtons++;
            }
        }
        return numberOfHeaderButtons;
    }

    private boolean isPanelVisible(boolean isHeaderPanel){
        for (InlineMenuItem item : menuItems){
            if (isHeaderPanel && (item.isHeaderMenuItem() || item.getAction() instanceof HeaderMenuAction)){
                return true;
            }
            if (!isHeaderPanel && !(item.getAction() instanceof HeaderMenuAction)){
                return true;
            }
        }
        return false;
    }
}
