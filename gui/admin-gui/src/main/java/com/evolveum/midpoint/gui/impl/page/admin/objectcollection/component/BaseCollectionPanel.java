/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.objectcollection.component;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

@PanelType(name = "baseCollection")
@PanelInstance(identifier = "baseCollection",
        applicableForType = ObjectCollectionType.class,
        display = @PanelDisplay(label = "pageObjectCollection.baseCollection.title", order = 40))
public class BaseCollectionPanel  extends AbstractObjectMainPanel<ObjectCollectionType, ObjectDetailsModels<ObjectCollectionType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(BaseCollectionPanel.class);
    private static final String ID_PANEL = "panel";

    private static final String DOT_CLASS = BaseCollectionPanel.class.getName() + ".";

    public BaseCollectionPanel(String id, AssignmentHolderDetailsModel<ObjectCollectionType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel<CollectionRefSpecificationType>(ID_PANEL,
                        createModel(getObjectWrapperModel(), ObjectCollectionType.F_BASE_COLLECTION),
                        CollectionRefSpecificationType.COMPLEX_TYPE) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        if (ItemPath.create(ObjectCollectionType.F_BASE_COLLECTION, CollectionRefSpecificationType.F_BASE_COLLECTION_REF)
                                .isSuperPathOrEquivalent(itemWrapper.getPath())) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
        add(panel);
    }

    private <C extends Containerable> PrismContainerWrapperModel<ObjectCollectionType, C> createModel(IModel<PrismObjectWrapper<ObjectCollectionType>> model, ItemName itemName) {
        return PrismContainerWrapperModel.fromContainerWrapper(model, itemName);
    }
}
