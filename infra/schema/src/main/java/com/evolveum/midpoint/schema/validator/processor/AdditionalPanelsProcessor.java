/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class AdditionalPanelsProcessor implements UpgradeObjectProcessor<ObjectType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.PREVIEW;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(object, path, GuiObjectListViewType.class, GuiObjectListViewType.F_ADDITIONAL_PANELS);
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        GuiObjectListViewType view = getItemParent(object, path);

        GuiObjectListViewAdditionalPanelsType additionalPanels = view.getAdditionalPanels();
        if (additionalPanels.getMemberPanel() == null) {
            view.setAdditionalPanels(null);
            return true;
        }

        GuiObjectListPanelConfigurationType memberPanel = additionalPanels.getMemberPanel();

        GuiObjectDetailsPageType objectDetailsPage = new GuiObjectDetailsPageType();
        objectDetailsPage.setType(view.getType());

        ContainerPanelConfigurationType panel = new ContainerPanelConfigurationType();
        panel.setIdentifier("orgMembers");  //view.getIdentifier();
        objectDetailsPage.getPanel().add(panel);

        GuiObjectListViewType listView = new GuiObjectListViewType();
        panel.setListView(listView);

        listView.setSearchBoxConfiguration(memberPanel.getSearchBoxConfiguration().clone());

        ItemPath objectCollectionViewsPath = path.allUpToIncluding(path.size() - 4);
        AdminGuiConfigurationType adminGuiConfiguration = getItemParent(object, objectCollectionViewsPath);
        GuiObjectDetailsSetType objectDetails = adminGuiConfiguration.getObjectDetails();
        if (objectDetails == null) {
            objectDetails = new GuiObjectDetailsSetType();
            adminGuiConfiguration.setObjectDetails(objectDetails);
        }

        objectDetails.getObjectDetailsPage().add(objectDetailsPage);


        view.setAdditionalPanels(null);
        return true;
    }
}
