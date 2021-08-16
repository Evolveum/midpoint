package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

@PanelType(name = "resourceGenerics")
@PanelInstance(identifier = "resourceGenerics", status = ItemStatus.NOT_CHANGED, applicableFor = ResourceType.class)
@PanelDisplay(label = "Generics", order = 50)
public class ResourceGenericsPanel extends ResourceContentTabPanel {

    public ResourceGenericsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> model, ContainerPanelConfigurationType config) {
        super(id, ShadowKindType.ENTITLEMENT, model, config);
    }
}
