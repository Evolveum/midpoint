/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */

@Experimental
@PanelInstance(identifier = "rw-credentials",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.ADD,
        display = @PanelDisplay(label = "PageResource.wizard.step.credentials", icon = "fa fa-key"),
        expanded = true)
public class PasswordStepPanel extends AbstractResourceWizardStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(PasswordStepPanel.class);

    protected static final String ID_PANEL = "panel";

    public static final String PANEL_TYPE = "rw-credentials";

    private final IModel<PrismContainerWrapper<ResourcePasswordDefinitionType>> containerModel;
    private final ResourceDetailsModel resourceModel;

    public PasswordStepPanel(ResourceDetailsModel model,
                             IModel<PrismContainerWrapper<ResourcePasswordDefinitionType>> containerModel) {
        super(model);
        this.containerModel = containerModel;
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        SingleContainerPanel panel;
        if (getContainerConfiguration() == null) {
            panel = new SingleContainerPanel(ID_PANEL, getContainerFormModel(), ResourcePasswordDefinitionType.COMPLEX_TYPE){

                @Override
                protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                    if (isItemHidden(itemWrapper)) {
                        return ItemVisibility.HIDDEN;
                    }
                    return super.getVisibility(itemWrapper);
                }
            };
        } else {
            panel = new SingleContainerPanel(ID_PANEL, getContainerFormModel(), getContainerConfiguration()) {

                @Override
                protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                    if (isItemHidden(itemWrapper)) {
                        return ItemVisibility.HIDDEN;
                    }
                    return super.getVisibility(itemWrapper);
                }
            };
        }
        panel.setOutputMarkupId(true);
        panel.add(AttributeAppender.append("class", "card col-12"));
        add(panel);
    }

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return WebComponentUtil.getContainerConfiguration(resourceModel.getObjectDetailsPageConfiguration().getObject(), getPanelType());
    }

    private boolean isItemHidden(ItemWrapper itemWrapper) {
        return itemWrapper.getPath().namedSegmentsOnly().equivalent(
                ItemPath.create(ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_CREDENTIALS,
                        ResourceCredentialsDefinitionType.F_PASSWORD,
                        ResourcePasswordDefinitionType.F_CACHING));
    }

    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return containerModel;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

//    private String getIcon() {
//        return "fa fa-key";
//    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.credentials");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.credentials.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.credentials.subText");
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }
}
