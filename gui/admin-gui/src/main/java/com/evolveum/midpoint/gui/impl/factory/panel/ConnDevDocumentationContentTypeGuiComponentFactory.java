/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validation.DocumentationContentTypeValidator;
import com.evolveum.midpoint.smart.api.conndev.DocumentationContentTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDocumentationSourceType;

/**
 * Renders documentationSource/contentType with validation of the content type,
 * offering the supported content types to choose from.
 */
@org.springframework.stereotype.Component
public class ConnDevDocumentationContentTypeGuiComponentFactory extends AbstractInputGuiComponentFactory<String>
        implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getParentContainerValue(ConnDevDocumentationSourceType.class) == null) {
            return false;
        }
        return QNameUtil.match(wrapper.getItemName(), ConnDevDocumentationSourceType.F_CONTENT_TYPE);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new AutoCompleteTextPanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), String.class,
                StringAutoCompleteRenderer.INSTANCE) {

            @Override
            public Iterator<String> getIterator(String input) {
                String filter = StringUtils.trimToEmpty(input).toLowerCase(Locale.ROOT);
                return DocumentationContentTypes.getSupportedContentTypes().stream()
                        .filter(contentType -> contentType.contains(filter))
                        .sorted()
                        .iterator();
            }
        };
    }

    @Override
    public void configure(PrismPropertyPanelContext<String> panelCtx, Component component) {
        super.configure(panelCtx, component);
        if (component instanceof InputPanel panel) {
            panel.getBaseFormComponent().add(new DocumentationContentTypeValidator());
        }
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
