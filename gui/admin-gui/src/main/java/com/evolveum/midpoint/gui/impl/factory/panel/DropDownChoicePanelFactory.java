/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author katkav
 *
 */
@Component
public class DropDownChoicePanelFactory extends AbstractInputGuiComponentFactory<QName> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return AssignmentType.F_FOCUS_TYPE.equals(wrapper.getItemName()) || DOMUtil.XSD_QNAME.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        List<QName> typesList = WebComponentUtil.createObjectTypeList();
        if (AssignmentType.F_FOCUS_TYPE.equals(panelCtx.getDefinitionName())){
            typesList = WebComponentUtil.createFocusTypeList();
        }

        DropDownChoicePanel<QName> typePanel = new DropDownChoicePanel<QName>(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                Model.ofList(typesList), new QNameObjectTypeChoiceRenderer(), true);
        typePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        typePanel.setOutputMarkupId(true);
        return typePanel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
