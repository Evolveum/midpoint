/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.objectcollection.component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;

@PanelType(name = "objectCollectionOption")
@PanelInstance(identifier = "objectCollectionOption",
        applicableForType = ObjectCollectionType.class,
        display = @PanelDisplay(label = "pageObjectCollection.option.title", order = 70))
public class ObjectCollectionOptionPanel extends AbstractObjectMainPanel<ObjectCollectionType, ObjectDetailsModels<ObjectCollectionType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectCollectionOptionPanel.class);
    private static final String ID_PANEL = "panel";

    private static final String DOT_CLASS = ObjectCollectionOptionPanel.class.getName() + ".";

    public ObjectCollectionOptionPanel(String id, AssignmentHolderDetailsModel<ObjectCollectionType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel =
                new SingleContainerPanel<>(ID_PANEL,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ObjectCollectionType.F_GET_OPTIONS),
                        SelectorQualifiedGetOptionsType.COMPLEX_TYPE);
        add(panel);
    }

}
