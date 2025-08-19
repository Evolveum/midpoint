/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.SearchFilterConfigurationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;

@Component
public class ResourceAttributesSearchFilterPanelFactory extends SearchFilterPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAttributesSearchFilterPanelFactory.class);

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return super.match(wrapper, valueWrapper)
                && ItemPath.create(
                        ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_DELINEATION,
                        ResourceObjectTypeDelineationType.F_FILTER)
                .equivalent(wrapper.getPath().namedSegmentsOnly());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<SearchFilterType> panelCtx) {
        return new SearchFilterConfigurationPanel(
                panelCtx.getComponentId(), panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel(), null) {
            @Override
            protected SearchFilterTypeForQueryModel createQueryModel(IModel model, LoadableModel filterTypeModel, boolean useParsing) {
                ItemRealValueModel<QName> objectClassModel = new ItemRealValueModel<>((IModel<? extends PrismValueWrapper<QName>>) () -> {
                    try {
                        PrismPropertyWrapper<QName> objectClass =
                                getItemModelObject().getParent().findProperty(ResourceObjectTypeDelineationType.F_OBJECT_CLASS);
                        return objectClass.getValue();

                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't find object class property.");
                        return null;
                    }
                });
                return new ResourceAttributeSearchFilterTypeForQueryModel(model, getPageBase(), useParsing, objectClassModel);
            }
        };
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 10;
    }
}
