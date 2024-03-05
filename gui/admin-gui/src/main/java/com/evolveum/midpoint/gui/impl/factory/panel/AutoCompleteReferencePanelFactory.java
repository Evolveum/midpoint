/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.form.ReferenceAutocompletePanel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lskublik
 */
@Component
public class AutoCompleteReferencePanelFactory
        implements GuiComponentFactory<PrismReferencePanelContext<ObjectReferenceType>> {

    private static final Trace LOGGER = TraceManager.getTrace(AutoCompleteReferencePanelFactory.class);

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 100;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, wrapper.getTypeName())
                && SchemaConstants.NS_REPORT_EXTENSION.equals(wrapper.getItemName().getNamespaceURI())
                && wrapper.getParent() == null;
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismReferencePanelContext<ObjectReferenceType> panelCtx) {
        ReferenceAutocompletePanel<ObjectReferenceType> panel = new ReferenceAutocompletePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {
            @Override
            public List<QName> getSupportedTypes() {
                List<QName> targetTypeList = panelCtx.getItemWrapperModel().getObject().getTargetTypes();
                if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
                    return super.getSupportedTypes();
                }
                return targetTypeList;
            }
        };
        panel.setOutputMarkupId(true);
        return panel;
    }
}
