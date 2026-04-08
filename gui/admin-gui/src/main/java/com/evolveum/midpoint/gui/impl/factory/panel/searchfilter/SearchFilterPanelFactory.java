/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.searchfilter;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SearchFilterConfigurationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Component
public class SearchFilterPanelFactory extends AbstractInputGuiComponentFactory<SearchFilterType> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterPanelFactory.class);

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
                String errorMessage = "";
                try {
                    Class<?> collectionType = WebComponentUtil.qnameToClass(getFilterObjectType(panelCtx));
                    panelCtx.getPageBase().getQueryConverter().createObjectFilter(collectionType, panelCtx.getRealValueModel().getObject());
                } catch (Exception ex) {
                    errorMessage = StringUtils.isNotEmpty(ex.getLocalizedMessage()) ? ex.getLocalizedMessage() : ex.getMessage();
                }
                if (StringUtils.isNotEmpty(errorMessage)) {
                    sfcp.getBaseFormComponent().error(errorMessage);
                }
            }
        }
        super.configure(panelCtx, component);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        return new SearchFilterConfigurationPanel(
                panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel(), getFilterObjectType(panelCtx)
        );
    }

    private boolean isCollectionObjectParentContainer(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
        PrismContainerValueWrapper<?> containerWrapper = searchFilterItemWrapper.getParent();
        return containerWrapper != null && containerWrapper.getRealValue() instanceof ObjectCollectionType;
    }

    private QName getFilterObjectType(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        PrismPropertyWrapper<SearchFilterType> searchFilterItemWrapper = panelCtx.unwrapWrapperModel();
        PrismContainerValueWrapper<?> containerWrapper = searchFilterItemWrapper.getParent();
        if (containerWrapper == null) {
            return null;
        }
        var parentContainerValue = containerWrapper.getRealValue();
        if (parentContainerValue instanceof ObjectCollectionType oct) {
            return oct.getType();
        } else if (parentContainerValue instanceof CollectionRefSpecificationType collectionRefSpecificationType) {
            try {
                Task task = panelCtx.getPageBase().createSimpleTask("compileObjectCollectionView");
                CompiledObjectCollectionView compiledView = panelCtx.getPageBase().getModelInteractionService()
                        .compileObjectCollectionView(collectionRefSpecificationType, null, task, task.getResult());
                return compiledView.getContainerType();
            } catch (Exception e) {
                LOGGER.debug("Cannot get filter object type", e);
            }
        }
        return null;
    }
}
