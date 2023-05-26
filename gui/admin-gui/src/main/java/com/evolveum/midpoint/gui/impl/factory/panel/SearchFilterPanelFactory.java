/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import jakarta.annotation.PostConstruct;

import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SearchFilterConfigurationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Component
public class SearchFilterPanelFactory extends AbstractGuiComponentFactory<SearchFilterType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return SearchFilterType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
        PrismContainerValueWrapper<?> containerWrapper = searchFilterItemWrapper.getParent();
        if (containerWrapper != null && containerWrapper.getRealValue() instanceof ObjectCollectionType) {
            return new SearchFilterConfigurationPanel(
                    panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel(), containerWrapper);
        }
        return new SearchFilterConfigurationPanel(
                panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel(), null);
    }

    @Override
    public void configure(PrismPropertyPanelContext<SearchFilterType> panelCtx, org.apache.wicket.Component component) {
        if (!(component instanceof AceEditorPanel)) {
            super.configure(panelCtx, component);
            return;
        }
        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(((AceEditorPanel) component).getEditor()));
    }
}
