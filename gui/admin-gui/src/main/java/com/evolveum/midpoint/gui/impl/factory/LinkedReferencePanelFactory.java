/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismReferencePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapperImpl;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by Kate Honchar.
 */
@Component
public class LinkedReferencePanelFactory implements GuiComponentFactory<PrismReferencePanelContext<ObjectReferenceType>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(LinkedReferencePanelFactory.class);

    @Autowired
    GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        boolean match = QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, wrapper.getTypeName()) &&
                QNameUtil.match(CaseType.F_PARENT_REF, wrapper.getPath().asSingleName());
        if (match){
            try {
                PrismReferenceValueWrapperImpl valueWrapper = (PrismReferenceValueWrapperImpl)
                        ((PrismReferenceWrapperImpl) wrapper).getValue();
                valueWrapper.setLink(true);
            } catch (SchemaException e){
                LOGGER.warn("Unable to set isLink status for PrismReferenceValueWrapper: {}", e.getLocalizedMessage());
            }
        }
        return match;
    }

    @Override
    public Panel createPanel(PrismReferencePanelContext<ObjectReferenceType> panelCtx) {
        LinkedReferencePanel panel = new LinkedReferencePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        panel.setOutputMarkupId(true);
        return panel;
    }

}
