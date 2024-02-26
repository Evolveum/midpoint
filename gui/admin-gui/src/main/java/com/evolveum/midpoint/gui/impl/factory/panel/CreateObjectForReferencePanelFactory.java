/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.impl.component.form.CreateObjectForReferencePanel;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;

@Component
public class CreateObjectForReferencePanelFactory
        implements GuiComponentFactory<PrismReferencePanelContext<ObjectReferenceType>> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateObjectForReferencePanelFactory.class);

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 999;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, wrapper.getTypeName()) &&
                ItemPath.equivalent(
                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING,
                                SchemaHandlingType.F_OBJECT_TYPE,
                                ResourceObjectTypeDefinitionType.F_FOCUS,
                                ResourceObjectFocusSpecificationType.F_ARCHETYPE_REF),
                        wrapper.getPath().namedSegmentsOnly());
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismReferencePanelContext<ObjectReferenceType> panelCtx) {
        CreateObjectForReferencePanel panel = new CreateObjectForReferencePanel(
                panelCtx.getComponentId(), panelCtx.getValueWrapperModel(), createContainerConfiguration());
        panel.setFeedback(panelCtx.getFeedback());
        panel.setOutputMarkupId(true);
        return panel;
    }

    private ContainerPanelConfigurationType createContainerConfiguration() {
        return new ContainerPanelConfigurationType()
                .applicableForOperation(OperationTypeType.WIZARD)
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-archetype")
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(ArchetypeType.F_SUPER_ARCHETYPE_REF))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(ArchetypeType.F_NAME))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(ArchetypeType.F_DESCRIPTION))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_ICON,
                                                IconType.F_CSS_CLASS)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_ICON,
                                                IconType.F_COLOR)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                );
    }
}
