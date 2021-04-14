package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

public class ResourceAttributePanel extends MultivalueContainerListPanelWithDetailsPanel<ResourceAttributeDefinitionType> {

    public ResourceAttributePanel(String id, Class<ResourceAttributeDefinitionType> type) {
        super(id, type);
    }

    @Override
    protected MultivalueContainerDetailsPanel<ResourceAttributeDefinitionType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> item) {
        return null;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return false;
    }

    @Override
    protected IModel<PrismContainerWrapper<ResourceAttributeDefinitionType>> getContainerModel() {
        return null;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }
}
