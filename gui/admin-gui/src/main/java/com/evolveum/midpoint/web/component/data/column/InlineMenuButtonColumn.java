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
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
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

    private int defaultButtonsNumber = 2;
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
        cellItem.add(getPanel(componentId, rowModel, defaultButtonsNumber));
    }

    @Override
    public Component getHeader(String componentId) {

        return getPanel(componentId, null, getHeaderNumberOfButtons());
    }

    private Component getPanel(String componentId, IModel<T> rowModel,
                               int numberOfButtons) {
        return new MenuMultiButtonPanel<T>(componentId, rowModel, numberOfButtons, createMenuModel(rowModel, menuItems)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected AjaxIconButton createButton(int index, String componentId, IModel<T> model) {
                AjaxIconButton btn = buildDefaultButton(componentId,
                        new Model<>(getButtonIconCss(index, menuItems)),
                        new Model<>(getButtonTitle(index, menuItems)),
                        new Model<>(getButtonCssClass(index, menuItems)),
                        target -> {
                            setRowModelToAction(rowModel, menuItems);
                            menuItemClickPerformed(index, target, model, menuItems);
                        });
                btn.showTitleAsLabel(false);
                btn.add(new VisibleBehaviour(() -> isButtonVisible(index, model)));

                return btn;
            }
        };
    }

    private void setRowModelToAction(IModel<T> rowModel, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems) {
            if (menuItem.getAction() != null) {
                ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
            }
        }
    }

    private IModel<List<InlineMenuItem>> createMenuModel(final IModel<T> rowModel, List<InlineMenuItem> menuItems) {
        return new LoadableModel<List<InlineMenuItem>>(false) {

            @Override
            public List<InlineMenuItem> load() {
                if (rowModel == null) {
                    return menuItems;
                }
                if (rowModel.getObject() == null ||
                        !(rowModel.getObject() instanceof InlineMenuable)) {
                    return new ArrayList<>();
                }
                for (InlineMenuItem item : ((InlineMenuable) rowModel.getObject()).getMenuItems()) {
                    if (!(item.getAction() instanceof ColumnMenuAction)) {
                        continue;
                    }

                    ColumnMenuAction action = (ColumnMenuAction) item.getAction();
                    action.setRowModel(rowModel);
                }
                return ((InlineMenuable) rowModel.getObject()).getMenuItems();
            }
        };
    }

    private void menuItemClickPerformed(int id, AjaxRequestTarget target, IModel<T> model, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems) {
            if (menuItem.getId() == id) {
                if (menuItem.getAction() != null) {
                    if (menuItem.showConfirmationDialog() && menuItem.getConfirmationMessageModel() != null) {
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
            	menuItem.getAction().onClick(target);
            }
        };
        pageBase.showMainPopup(dialog, target);
    }

    public boolean isButtonVisible(int id, IModel<T> model) {
        return true;
    }

    public String getButtonSizeCssClass(int id) {
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.EXTRA_SMALL.toString();
    }

    protected String getButtonCssClass(int id, List<InlineMenuItem> menuItems) {
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        // Do not add color. It attracts too much attention
//        sb.append(getButtonColorCssClass(id, menuItems)).append(" ");
        sb.append("btn-default ");
        sb.append(getButtonSizeCssClass(id)).append(" ");

        return sb.toString();
    }

    protected String getButtonIconCss(int id, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems) {
            if (menuItem.getId() == id && menuItem instanceof ButtonInlineMenuItem) {
                return ((ButtonInlineMenuItem)menuItem).getButtonIconCssClass() + " fa-fw"; //temporary size fix, should be moved somewhere...
            }
        }

        return null;
    }

    public String getButtonTitle(int id, List<InlineMenuItem> menuItems) {
        for (InlineMenuItem menuItem : menuItems) {
            if (menuItem.getId() == id) {
                return menuItem.getLabel() != null && menuItem.getLabel().getObject() != null ?
                        menuItem.getLabel().getObject() : "";
            }
        }
        return "";
    }

    protected int getHeaderNumberOfButtons() {
        return this.defaultButtonsNumber;
    }

//    protected List<InlineMenuItem> getHeaderMenuItems() {
//        return menuItems;
//    }
}
