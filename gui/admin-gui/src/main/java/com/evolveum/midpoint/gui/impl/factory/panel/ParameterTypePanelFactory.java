/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * @author katkav
 */
@Component
public class ParameterTypePanelFactory extends DropDownChoicePanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
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
                if (StringUtils.isNotEmpty(object)){
                    QName objectQName = getQName(object);
                    if (!QNameUtil.match(objectQName, panelCtx.getRealValueModel().getObject())) {
                        panelCtx.getRealValueModel().setObject(objectQName);
                    }
                } else {
                    panelCtx.getRealValueModel().setObject(null);
                }
            }
        };
        return new TextPanel(panelCtx.getComponentId(), qNameModel, String.class, false);
    }

    private QName getQName(String object){
        if (QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, new QName(object))) {
            return ObjectReferenceType.COMPLEX_TYPE;
        }
        if (QNameUtil.isUriQName(object)) {
            return QNameUtil.uriToQName(object);
        }
        return new QName(object);
    }

    @Override
    public Integer getOrder() {
        return 9000;
    }

}
