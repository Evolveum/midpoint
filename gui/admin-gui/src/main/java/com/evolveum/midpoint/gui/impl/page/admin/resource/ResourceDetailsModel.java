package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ResourceDetailsModel extends AssignmentHolderDetailsModel<ResourceType> {

    public ResourceDetailsModel(LoadableModel<PrismObject<ResourceType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

//    @Override
//    protected GuiObjectDetailsPageType loadDetailsPageConfiguration(PrismObject<ResourceType> resource) {
//        GuiObjectDetailsPageType defaultPageConfig = super.loadDetailsPageConfiguration(resource);
//
//        Optional<ContainerPanelConfigurationType> schemaHandlingConfig = defaultPageConfig.getPanel().stream().filter(p -> "schemaHandling".equals(p.getPanelType())).findFirst();
//        if (!schemaHandlingConfig.isPresent()) {
//            return defaultPageConfig;
//        }
//
//
//    }


}
