/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchFilterParameterType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

/**
 * @author katkav
 */
@Component
public class ParameterTypePanelFactory extends DropDownChoicePanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())
                && SearchFilterParameterType.F_TYPE.equals(wrapper.getItemName())
                && wrapper.getParent().getDefinition() != null
                && ParameterType.class.isAssignableFrom(wrapper.getParent().getDefinition().getTypeClass());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        IModel<String> qNameModel = new IModel<String>() {
            @Override
            public String getObject() {
                if (panelCtx.getRealValueModel().getObject() != null) {
                    return panelCtx.getRealValueModel().getObject().getLocalPart();
                }
                return null;
            }

            @Override
            public void setObject(String object) {
                if (StringUtils.isNotEmpty(object)
                        && !QNameUtil.match(new QName(object), panelCtx.getRealValueModel().getObject())) {
                    panelCtx.getRealValueModel().setObject(new QName(object));
                }
            }
        };
        return new TextPanel(panelCtx.getComponentId(), qNameModel, String.class, false);
    }

    @Override
    public Integer getOrder() {
        return 9000;
    }

}
