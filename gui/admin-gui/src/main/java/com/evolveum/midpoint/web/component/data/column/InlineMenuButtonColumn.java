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
 * TODO Still needs more cleanup. Why button column has to understand activation, focus objects or handle modal windows? That should not be here definitely.
 */
public class InlineMenuButtonColumn<T extends Serializable> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 1L;

    protected List<InlineMenuItem> menuItems = new ArrayList<>();
    protected List<ButtonInlineMenuItem> buttonMenuItems = new ArrayList<>();

    private PageBase pageBase;

    public InlineMenuButtonColumn(List<InlineMenuItem> menuItems, PageBase pageBase) {
        super(null);
        this.menuItems = menuItems;
        this.pageBase = pageBase;
        initButtonMenuItems();
    }

    private void initButtonMenuItems(){
        menuItems.forEach(menuItem -> {
            if (menuItem instanceof ButtonInlineMenuItem){
                buttonMenuItems.add((ButtonInlineMenuItem) menuItem);
            }
        });
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        cellItem.add(getPanel(componentId, rowModel, getNumberOfButtons(), false));
    }

    @Override
    public Component getHeader(String componentId) {

        return getPanel(componentId, null, getHeaderNumberOfButtons(), true);
    }

    private Component getPanel(String componentId, IModel<T> rowModel,
                               int numberOfButtons, boolean isHeader) {
        if (rowModel != null && rowModel.getObject() instanceof InlineMenuable){
            for (InlineMenuItem mm : ((InlineMenuable) rowModel.getObject()).getMenuItems()){
                if (mm.getAction() instanceof ColumnMenuAction) {
                    ((ColumnMenuAction) mm.getAction()).setRowModel(rowModel);
                }
            }
        }




        List<InlineMenuItem> filteredMenuItems = new ArrayList<InlineMenuItem>();
        menuItems.forEach(menuItem -> {
            if (menuItem instanceof ButtonInlineMenuItem){
                return;
            }
            if (isHeader && !menuItem.isMenuHeader()){
                return;
            }
            if (menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction){
//                ((ColumnMenuAction)menuItem.getAction()).setRowModel(rowModel);
            }
            filteredMenuItems.add(menuItem);
        });
//                return filteredMenuItems;

//        if (rowModel != null) {
//            ((InlineMenuable) rowModel.getObject()).getMenuItems().clear();
//            ((InlineMenuable) rowModel.getObject()).getMenuItems().addAll(filteredMenuItems);
//        }


        //TODO when rowModel.getModel == null??
//                if (rowModel.getObject() == null ||
//                        !(rowModel.getObject() instanceof InlineMenuable)) {
//                    return new ArrayList<>();
//                }
//                for (InlineMenuItem item : ((InlineMenuable) rowModel.getObject()).getMenuItems()) {
//                    if (!(item.getAction() instanceof ColumnMenuAction)) {
//                        continue;
//                    }
//
//                    ColumnMenuAction action = (ColumnMenuAction) item.getAction();
//                    action.setRowModel(rowModel);
//                }






        return new MenuMultiButtonPanel<T>(componentId, rowModel, numberOfButtons,
                rowModel != null ? Model.ofList(((InlineMenuable)rowModel.getObject()).getMenuItems()) : Model.ofList(menuItems)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected AjaxIconButton createButton(int index, String componentId, IModel<T> model) {
                AjaxIconButton btn = buildDefaultButton(componentId,
                        new Model<>(getButtonIconCss(index)),
                        new Model<>(getButtonTitle(index)),
                        new Model<>(getButtonCssClass()),
                        target -> {
                            setRowModelToButtonAction(rowModel);
                            buttonMenuItemClickPerformed(index, target);
                        });
                btn.showTitleAsLabel(false);
//                btn.add(new VisibleBehaviour(() -> isButtonVisible(index, model)));

                return btn;
            }
        };
    }

    private void setRowModelToButtonAction(IModel<T> rowModel) {
        for (InlineMenuItem menuItem : buttonMenuItems) {
            if (menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction) {
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }
        }
    }


    private IModel<List<InlineMenuItem>> createMenuModel(final IModel<T> rowModel, List<InlineMenuItem> menuItems, boolean isHeader) {
        return new LoadableModel<List<InlineMenuItem>>(false) {

            @Override
            public List<InlineMenuItem> load() {
                List<InlineMenuItem> filteredMenuItems = new ArrayList<>();
                menuItems.forEach(menuItem -> {
                    if (menuItem instanceof ButtonInlineMenuItem){
                        return;
                    }
                    if (isHeader && !menuItem.isMenuHeader()){
                        return;
                    }
                    if (menuItem.getAction() != null && menuItem.getAction() instanceof ColumnMenuAction){
                        ((ColumnMenuAction)menuItem.getAction()).setRowModel(rowModel);
                    }
                    filteredMenuItems.add(menuItem);
                });
//                return filteredMenuItems;

                if (rowModel == null) {
                    return filteredMenuItems;
                }

//                ((InlineMenuable) rowModel.getObject()).getMenuItems().clear();
//                ((InlineMenuable) rowModel.getObject()).getMenuItems().addAll(filteredMenuItems);

                //TODO when rowModel.getModel == null??
//                if (rowModel.getObject() == null ||
//                        !(rowModel.getObject() instanceof InlineMenuable)) {
//                    return new ArrayList<>();
//                }
//                for (InlineMenuItem item : ((InlineMenuable) rowModel.getObject()).getMenuItems()) {
//                    if (!(item.getAction() instanceof ColumnMenuAction)) {
//                        continue;
//                    }
//
//                    ColumnMenuAction action = (ColumnMenuAction) item.getAction();
//                    action.setRowModel(rowModel);
//                }
                return filteredMenuItems;
            }
        };
    }

    private void buttonMenuItemClickPerformed(int id, AjaxRequestTarget target) {
        if (id >= buttonMenuItems.size()){
            return;
        }
        ButtonInlineMenuItem menuItem = buttonMenuItems.get(id);
        if (menuItem.getAction() != null) {
            if (menuItem.showConfirmationDialog() && menuItem.getConfirmationMessageModel() != null) {
                showConfirmationPopup(menuItem, target);
            } else {
                menuItem.getAction().onClick(target);
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

    private String getButtonIconCss(int id) {
        if (id >= buttonMenuItems.size()){
            return null;
        }
        return buttonMenuItems.get(id).getButtonIconCssClass() + " fa-fw";
    }

    private String getButtonTitle(int id) {
        if (id >= buttonMenuItems.size()){
            return null;
        }
        return buttonMenuItems.get(id).getLabel() != null && buttonMenuItems.get(id).getLabel().getObject() != null ?
                buttonMenuItems.get(id).getLabel().getObject() : "";
    }

    protected int getHeaderNumberOfButtons() {
        int numberOfHeaderButtons = 0;
        for (InlineMenuItem inlineMenuItem : menuItems){
            if (inlineMenuItem instanceof ButtonInlineMenuItem && inlineMenuItem.isMenuHeader()){
                numberOfHeaderButtons++;
            }
        }
        return numberOfHeaderButtons;
    }

    private int getNumberOfButtons() {
        return buttonMenuItems.size();
    }

//    protected List<InlineMenuItem> getHeaderMenuItems() {
//        return menuItems;
//    }
}
