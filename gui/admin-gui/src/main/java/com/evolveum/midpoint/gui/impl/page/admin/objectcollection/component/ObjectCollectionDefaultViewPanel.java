/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.objectcollection.component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

@PanelType(name = "defaultView")
@PanelInstance(identifier = "defaultView",
        applicableFor = ObjectCollectionType.class,
        status = ItemStatus.NOT_CHANGED,
        display = @PanelDisplay(label = "pageObjectCollection.defaultView.title", order = 50))
public class ObjectCollectionDefaultViewPanel extends AbstractObjectMainPanel<ObjectCollectionType, ObjectDetailsModels<ObjectCollectionType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectCollectionDefaultViewPanel.class);
    private static final String ID_PANEL = "panel";

    private static final String DOT_CLASS = ObjectCollectionDefaultViewPanel.class.getName() + ".";

    public ObjectCollectionDefaultViewPanel(String id, AssignmentHolderDetailsModel<ObjectCollectionType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel<>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ObjectCollectionType.F_DEFAULT_VIEW),
                        GuiObjectListViewType.COMPLEX_TYPE);
        add(panel);
    }
}
