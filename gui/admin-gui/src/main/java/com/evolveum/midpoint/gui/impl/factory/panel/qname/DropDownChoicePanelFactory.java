/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

import jakarta.annotation.PostConstruct;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

/**
 * @author katkav
 */
@Component
public class DropDownChoicePanelFactory extends AbstractInputGuiComponentFactory<QName> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return AssignmentType.F_FOCUS_TYPE.equals(wrapper.getItemName()) || DOMUtil.XSD_QNAME.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        List<QName> typesList = getTypesList(panelCtx);
        WebComponentUtil.sortObjectTypeList(typesList);

        DropDownChoicePanel<QName> typePanel = new DropDownChoicePanel<QName>(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                Model.ofList(typesList), new QNameObjectTypeChoiceRenderer(), true);
        typePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        typePanel.setOutputMarkupId(true);
        return typePanel;
    }

    protected List<QName> getTypesList(PrismPropertyPanelContext<QName> panelCtx) {
        if (AssignmentType.F_FOCUS_TYPE.equals(panelCtx.getDefinitionName())) {
            return ObjectTypeListUtil.createFocusTypeList();
        } else if ((ObjectCollectionType.F_TYPE.equals(panelCtx.getDefinitionName()) || GuiObjectListViewType.F_TYPE.equals(panelCtx.getDefinitionName()))
                && panelCtx.unwrapWrapperModel().getParent().getDefinition() != null &&
                (ObjectCollectionType.class.equals(panelCtx.unwrapWrapperModel().getParent().getDefinition().getTypeClass())
                        || GuiObjectListViewType.class.equals(panelCtx.unwrapWrapperModel().getParent().getDefinition().getTypeClass()))) {
            return ObjectTypeListUtil.createContainerableTypesQnameList();
        }
        return ObjectTypeListUtil.createObjectTypeList();
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
