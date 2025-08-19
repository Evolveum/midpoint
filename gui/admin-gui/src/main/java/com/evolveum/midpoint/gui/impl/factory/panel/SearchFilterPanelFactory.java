/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
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
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return SearchFilterType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    public void configure(PrismPropertyPanelContext<SearchFilterType> panelCtx, org.apache.wicket.Component component) {
        if (component instanceof AceEditorPanel) {
            panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(((AceEditorPanel) component).getEditor()));
            return;
        }
        //in case object collection object has incorrectly defined filter, we want to warn user about it
        //covers #10800
        if (component instanceof SearchFilterConfigurationPanel sfcp) {
            if (isCollectionObjectParentContainer(panelCtx)) {
                PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
                PrismContainerValueWrapper<ObjectCollectionType> containerWrapper =
                        (PrismContainerValueWrapper<ObjectCollectionType>) searchFilterItemWrapper.getParent();
                ObjectCollectionType col = containerWrapper.getRealValue();
                String errorMessage = "";
                try {
                    Class<?> collectionType = WebComponentUtil.qnameToClass(panelCtx.getPrismContext(), col.getType());
                    panelCtx.getPageBase().getQueryConverter().createObjectFilter(collectionType, panelCtx.getRealValueModel().getObject());
                } catch (Exception ex) {
                    errorMessage = StringUtils.isNotEmpty(ex.getLocalizedMessage()) ? ex.getLocalizedMessage() : ex.getMessage();
                }
                if (StringUtils.isNotEmpty(errorMessage)) {
                    panelCtx.getFeedback().getFeedbackMessages().add(new FeedbackMessage(sfcp, errorMessage, 400));
                }
            }
        }
        super.configure(panelCtx, component);
    }

    private boolean isCollectionObjectParentContainer(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
        PrismContainerValueWrapper<?> containerWrapper = searchFilterItemWrapper.getParent();
        return containerWrapper != null && containerWrapper.getRealValue() instanceof ObjectCollectionType;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
        PrismContainerValueWrapper<?> containerWrapper = searchFilterItemWrapper.getParent();
        if (isCollectionObjectParentContainer(panelCtx)) {
            return new SearchFilterConfigurationPanel(
                    panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel(), containerWrapper);
        }
        return new SearchFilterConfigurationPanel(
                panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel(), null);
    }
}
