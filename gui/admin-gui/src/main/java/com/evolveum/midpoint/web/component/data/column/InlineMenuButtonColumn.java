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
import com.evolveum.midpoint.web.component.data.MenuMultiButtonPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * Created by honchar.
 *
 * todo rewrite
 */
public class InlineMenuButtonColumn<T extends Serializable> extends MultiButtonColumn<T>{
    protected List<InlineMenuItem> menuItems;
    private PageBase pageBase;

    public InlineMenuButtonColumn(List<InlineMenuItem> menuItems, int buttonsNumber, PageBase pageBase){
        super(null, menuItems.size() < 2 ? menuItems.size() : buttonsNumber);
        this.menuItems = menuItems;
        this.pageBase = pageBase;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        this.rowModel = rowModel;
        cellItem.add(getPanel(componentId, rowModel, this.numberOfButtons, this.menuItems));
    }

    @Override
    public Component getHeader(String componentId) {

        return getPanel(componentId, null, getHeaderNumberOfButtons(), getHeaderMenuItems());
    }

    private Component getPanel(String componentId, IModel<T> rowModel,
                               int numberOfButtons, List<InlineMenuItem> menuItems){
        panel = new MenuMultiButtonPanel<T>(componentId, numberOfButtons, rowModel, createMenuModel(rowModel, menuItems)) {

            @Override
            public String getCaption(int id) {
                return "";
            }

            @Override
            public String getButtonTitle(int id) {
                return InlineMenuButtonColumn.this.getButtonTitle(id, menuItems);
            }

            @Override
            protected String getButtonCssClass(int id) {
                return InlineMenuButtonColumn.this.getButtonCssClass(id, menuItems);
            }

            @Override
            public String getButtonIconCss(int id) {
                return InlineMenuButtonColumn.this.getButtonIconCss(id, menuItems);
            }

            @Override
            protected int getButtonId(int id){
                return id;
            }

            @Override
            public boolean isButtonVisible(int id, IModel<T> model) {
                return InlineMenuButtonColumn.this.isButtonVisible(id, model);
            }

            @Override
            public String getButtonSizeCssClass(int id) {
                return InlineMenuButtonColumn.this.getButtonSizeCssClass(id);
            }

            @Override
            public String getButtonColorCssClass(int id) {
                return InlineMenuButtonColumn.this.getButtonColorCssClass(id, menuItems);
            }

            @Override
            public void clickPerformed(int id, AjaxRequestTarget target, IModel<T> model) {
                setRowModelToAction(rowModel, menuItems);
                InlineMenuButtonColumn.this.menuItemClickPerformed(id, target, model, menuItems);
            }
        };
        return panel;
    }

    private void setRowModelToAction(IModel<T> rowModel, List<InlineMenuItem> menuItems){
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getAction() != null) {
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }
        }
    }

    private IModel<List<InlineMenuItem>> createMenuModel(final IModel<T> rowModel, List<InlineMenuItem> menuItems) {
        return new LoadableModel<List<InlineMenuItem>>(false) {

            @Override
            public List<InlineMenuItem> load() {
                if (rowModel == null){
                    return menuItems;
                }
                if (rowModel.getObject() == null ||
                        !(rowModel.getObject() instanceof InlineMenuable)) {
                    return new ArrayList<InlineMenuItem>();
                }
                for (InlineMenuItem item : ((InlineMenuable)rowModel.getObject()).getMenuItems()) {
                    if (!(item.getAction() instanceof ColumnMenuAction)) {
                        continue;
                    }

                    ColumnMenuAction action = (ColumnMenuAction) item.getAction();
                    action.setRowModel(rowModel);
                }
                return ((InlineMenuable)rowModel.getObject()).getMenuItems();
            }
        };
    }

    private void menuItemClickPerformed(int id, AjaxRequestTarget target, IModel<T> model, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems) {
            if (menuItem.getId() == id) {
                if (menuItem.getAction() != null) {
                    if (menuItem.isShowConfirmationDialog() && menuItem.getConfirmationMessageModel() != null) {
                        showConfirmationPopup(menuItem, target);
                    } else {
                        menuItem.getAction().onClick(target);
                    }
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
                ModalWindow modalWindow = findParent(ModalWindow.class);
                if (modalWindow != null) {
                    modalWindow.close(target);
                    menuItem.getAction().onClick(target);
                }
            }
        };
        pageBase.showMainPopup(dialog, target);
    }

    @Override
    public boolean isButtonVisible(int id, IModel<T> model) {
        if (model == null || model.getObject() == null){
            return true;
        }
        if (id == InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.ENABLE.getMenuItemId() &&
                model.getObject() instanceof SelectableBean &&
                ((SelectableBean) model.getObject()).getValue() instanceof FocusType){
            FocusType focus = (FocusType)((SelectableBean) model.getObject()).getValue();
            if (focus.getActivation() == null){
                return false;
            }
            return ActivationStatusType.DISABLED.equals(focus.getActivation().getEffectiveStatus());
        } else if (id == InlineMenuItem.FOCUS_LIST_INLINE_MENU_ITEM_ID.DISABLE.getMenuItemId() &&
                model.getObject() instanceof SelectableBean &&
                ((SelectableBean) model.getObject()).getValue() instanceof FocusType){
            FocusType focus = (FocusType)((SelectableBean) model.getObject()).getValue();
            if (focus.getActivation() == null){
                return true;
            }
            return !ActivationStatusType.DISABLED.equals(focus.getActivation().getEffectiveStatus());
        }
        return true;
    }

    public String getButtonColorCssClass(int id, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getId() == id){
                return menuItem.getButtonColorCssClass();
            }
        }
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    @Override
    public String getButtonSizeCssClass(int id) {
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.EXTRA_SMALL.toString();
    }

    protected String getButtonCssClass(int id, List<InlineMenuItem> menuItems) {
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        sb.append(getButtonColorCssClass(id, menuItems)).append(" ");
        sb.append(getButtonSizeCssClass(id)).append(" ");

        return sb.toString();
    }

    protected String getButtonIconCss(int id, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getId() == id){
                return menuItem.getButtonIconCssClass() + " fa-fw"; //temporary size fix, should be moved somewhere...
            }
        }

        return null;
    }

    public String getButtonTitle(int id, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getId() == id){
                return menuItem.getLabel() != null && menuItem.getLabel().getObject() != null ?
                        menuItem.getLabel().getObject() : "";
            }
        }
        return "";
    }

    protected int getHeaderNumberOfButtons(){
        return this.numberOfButtons;
    }

    protected List<InlineMenuItem> getHeaderMenuItems(){
        return menuItems;
    }
}
