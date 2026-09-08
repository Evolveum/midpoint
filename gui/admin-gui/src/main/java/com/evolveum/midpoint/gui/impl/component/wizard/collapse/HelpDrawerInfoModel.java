/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class HelpDrawerInfoModel
        implements DrawerDescriptor<HelpDrawerInfoModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ICON = "fa fa-info-circle text-primary";

    private final IModel<List<CollapsedItem<HelpDrawerInfoModel>>> itemsModel;

    boolean isCollapsed = true;

    /**
     * Creates empty help drawer model with no title and no content
     */
    public HelpDrawerInfoModel(){
        this(Model.of(""), Model.of(""));
        this.isCollapsed = false;
    }

    public HelpDrawerInfoModel(
            IModel<String> titleModel,
            IModel<String> helpContentModel) {

        List<HelpTab> tabs = List.of(new HelpTab(List.of(new HelpChapter(helpContentModel))));

        HelpCollapsedItem helpItem = new HelpCollapsedItem(titleModel, tabs);
        helpItem.setSelected(true);
        itemsModel = Model.ofList(List.of(helpItem));
    }

    @Override
    public boolean isShowedCollapsedMenu() {
        return false;
    }

    @Override
    public IModel<List<CollapsedItem<HelpDrawerInfoModel>>> getCollapsedItems() {
        return itemsModel;
    }

    @Override
    public Optional<CollapsedItem<HelpDrawerInfoModel>> getSelectedCollapsedItem() {
        return itemsModel.getObject().stream()
                .filter(CollapsedItem::isSelected)
                .findFirst();
    }

    @Override
    public boolean isCollapsedItemsVisible() {
        return this.isCollapsed;
    }

    private static class HelpCollapsedItem extends CollapsedItem<HelpDrawerInfoModel> {

        @Serial private static final long serialVersionUID = 1L;

        private final IModel<String> titleModel;
        private final HelpContentModel content;

        private HelpCollapsedItem(
                IModel<String> titleModel,
                List<HelpTab> tabs) {
            this.titleModel = titleModel;
            this.content = new HelpContentModel(tabs);
        }

        @Override
        public @NotNull IModel<String> getIcon() {
            return Model.of(ICON);
        }

        @Override
        @NotNull
        IModel<String> getTitleIconCss() {
            return Model.of(ICON);
        }

        @Override
        public IModel<String> getTitle() {
            return titleModel;
        }

        @Override
        public @NotNull Component getPanel(String id, HelpDrawerInfoModel drawerModel) {
            return new HelpContentPanel(id, Model.of(content));
        }
    }
}
