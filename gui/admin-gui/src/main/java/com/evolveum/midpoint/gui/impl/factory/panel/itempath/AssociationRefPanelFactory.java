/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.itempath;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.gui.impl.validator.AssociationRefAttributeValidator;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

/**
 * @author katka
 */
@Component
public class AssociationRefPanelFactory extends ItemPathPanelFactory {

    private static final long serialVersionUID = 1L;

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return super.match(wrapper, valueWrapper)
                && wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                ShadowAssociationDefinitionType.F_REF));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        IModel<String> model = new IModel<>() {
            @Override
            public String getObject() {
                return GuiDisplayNameUtil.getDisplayName(panelCtx.getRealValueModel().getObject());
            }

            @Override
            public void setObject(String object) {
                QName qName = WebPrismUtil.convertStringWithPrefixToQName(object);

                if (qName == null) {
                    return;
                }

                ItemPathType itemPathType = new ItemPathType(ItemName.fromQName(qName));
                panelCtx.getRealValueModel().setObject(itemPathType);
            }
        };

        TextPanel<String> panel = new TextPanel<>(panelCtx.getComponentId(), model, String.class, false);
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        return panel;
    }

    @Override
    public void configure(PrismPropertyPanelContext<ItemPathType> panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);

        InputPanel panel = (InputPanel) component;
        panel.getValidatableComponent().add(new AssociationRefAttributeValidator(panelCtx.getItemWrapperModel()));
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }
}
