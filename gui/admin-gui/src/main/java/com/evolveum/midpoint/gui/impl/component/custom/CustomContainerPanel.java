package com.evolveum.midpoint.gui.impl.component.custom;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import java.util.List;

public class CustomContainerPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private static final String ID_PANEL = "containerPanel";

    private ItemPath path;
    private ContainerPanelConfigurationType panelConfig;

    public CustomContainerPanel(String id, IModel<PrismObjectWrapper<O>> model, ContainerPanelConfigurationType panel) {
        super(id, model);
        this.panelConfig = panel;
        this.path = path;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

//        List<VirtualContainersSpecificationType> container = panelConfig.getContainer();
//        String identifier = container.getIdentifier();
//
//        List<VirtualContainerItemSpecificationType> items = container.getItem();
//
//
//        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
//                .visibilityHandler(wrapper -> {
//                    for (VirtualContainerItemSpecificationType i : items) {
//                        if (i.getPath().getItemPath().equivalent(wrapper.getPath().namedSegmentsOnly())) {
//                            UserInterfaceElementVisibilityType visibilityType = i.getVisibility();
//                            if (visibilityType == null) {
//                                return ItemVisibility.AUTO;
//                            }
//                            if (UserInterfaceElementVisibilityType.HIDDEN == visibilityType) {
//                                return ItemVisibility.HIDDEN;
//                            }
//                            return ItemVisibility.AUTO;
//                        }
//                    }
//                    return ItemVisibility.AUTO;
//                }).build();
//
//        try {
//            Panel panel = getPageBase().initItemPanel(ID_PANEL, UserType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getModelService(), path), settings);
//            add(panel);
//        } catch (SchemaException e) {
//            //TODO:
//            throw new SystemException(e);
//        }
    }
}
