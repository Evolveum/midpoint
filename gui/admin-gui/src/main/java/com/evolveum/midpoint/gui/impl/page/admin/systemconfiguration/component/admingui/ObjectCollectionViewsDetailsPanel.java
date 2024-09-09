/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.admingui;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectCollectionViewsDetailsPanel extends MultivalueContainerDetailsPanel<GuiObjectListViewType> {

    public ObjectCollectionViewsDetailsPanel(String id, IModel<PrismContainerValueWrapper<GuiObjectListViewType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public ObjectCollectionViewsDetailsPanel(String id, IModel<PrismContainerValueWrapper<GuiObjectListViewType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<GuiObjectListViewType> createDisplayNamePanel(String displayNamePanelId) {
        // so far no display name panel needed
        DisplayNamePanel d = new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return createStringResource("GuiObjectListViewType.label");
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> getModelObject().getIdentifier();
            }

            @Override
            protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
                WebMarkupContainer c = new WebMarkupContainer(idTypeImage);
                c.setVisible(false);
                return c;
            }
        };

        return d;
    }

    @Override
    protected @NotNull List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(createTab(
                "UserInterfaceFeatureType.display",
                GuiObjectListViewType.F_DISPLAY,
                DisplayType.COMPLEX_TYPE));

        tabs.add(createTab(
                "GuiObjectListViewType.searchBoxConfiguration",
                GuiObjectListViewType.F_SEARCH_BOX_CONFIGURATION,
                SearchBoxConfigurationType.COMPLEX_TYPE));

        tabs.add(createTab(
                "GuiObjectListViewType.column",
                GuiObjectListViewType.F_COLUMN,
                GuiObjectColumnType.COMPLEX_TYPE));

        tabs.add(createTab(
                "GuiObjectListViewType.collection",
                GuiObjectListViewType.F_COLLECTION,
                CollectionRefSpecificationType.COMPLEX_TYPE));

        tabs.add(createTab(
                "GuiObjectListViewType.action",
                GuiObjectListViewType.F_ACTION,
                GuiActionType.COMPLEX_TYPE));

        tabs.add(createTab(
                "GuiObjectListViewType.additionalPanels",
                GuiObjectListViewType.F_ADDITIONAL_PANELS,
                GuiObjectListViewAdditionalPanelsType.COMPLEX_TYPE));


        return tabs;
    }

    private ITab createTab(String key, ItemName path, QName type) {
        return new PanelTab(createStringResource(key)) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<>(
                        panelId,
                        PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), path),
                        type);
            }
        };
    }

    @Override
    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper instanceof PrismContainerWrapper) {
            PrismContainerWrapper pcw = (PrismContainerWrapper) itemWrapper;
            if (pcw.isVirtual()) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        }
        return ItemVisibility.AUTO;
    }
}
