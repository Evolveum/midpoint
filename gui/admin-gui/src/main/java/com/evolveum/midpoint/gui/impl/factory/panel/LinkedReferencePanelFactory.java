/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import org.apache.wicket.behavior.AttributeAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Kate Honchar.
 */
@Component
public class LinkedReferencePanelFactory
        implements GuiComponentFactory<PrismReferencePanelContext<ObjectReferenceType>> {

    private static final Trace LOGGER = TraceManager.getTrace(LinkedReferencePanelFactory.class);

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        boolean match = QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, wrapper.getTypeName()) &&
                QNameUtil.match(CaseType.F_PARENT_REF, wrapper.getPath().asSingleName());

        //TODO match method must not change the state of the wrapper
        if (match) {
            ((PrismReferenceValueWrapperImpl<?>)valueWrapper).setLink(true);
        }
        return wrapper instanceof PrismReferenceWrapper && (match || wrapper.isReadOnly() || wrapper.isMetadata());
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismReferencePanelContext<ObjectReferenceType> panelCtx) {
        LinkedReferencePanel<?> panel = new LinkedReferencePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        panel.setOutputMarkupId(true);
        return panel;
    }
}
