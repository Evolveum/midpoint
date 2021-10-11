/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.model.RelationModel;
import com.evolveum.midpoint.gui.impl.validator.RelationValidator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@Component
public class RelationPanelFactory extends AbstractGuiComponentFactory<QName> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        TextPanel<String> relationPanel = new TextPanel<>(panelCtx.getComponentId(), new RelationModel(panelCtx.getRealValueModel()));
        relationPanel.getBaseFormComponent().add(new RelationValidator());
        return relationPanel;
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        ItemPath relationRefPath = ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_RELATIONS, RelationsDefinitionType.F_RELATION, RelationDefinitionType.F_REF);
        return relationRefPath.equivalent(wrapper.getPath().removeIds());
    }

    @Override
    public Integer getOrder() {
        return 9999;
    }
}
