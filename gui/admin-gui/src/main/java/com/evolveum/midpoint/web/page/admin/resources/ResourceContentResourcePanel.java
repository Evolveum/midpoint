/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class ResourceContentResourcePanel extends ResourceContentPanel {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ResourceContentResourcePanel.class.getName() + ".";

    public ResourceContentResourcePanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
            QName objectClass, ShadowKindType kind, String intent, String searchMode, ContainerPanelConfigurationType config) {
        super(id, resourceModel, objectClass, kind, intent, searchMode, config);
    }

    @Override
    protected GetOperationOptionsBuilder addAdditionalOptions(GetOperationOptionsBuilder builder) {
        return builder;
    }

    @Override
    protected boolean isUseObjectCounting() {
        return ResourceTypeUtil.isCountObjectsCapabilityEnabled(getResourceModel().getObject().asObjectable());
    }


    //TODO//        //TODO compiled object collection view

    @Override
    protected SearchBoxConfigurationType getDefaultSearchBoxConfiguration() {
        return null;
    }

    private <T extends ObjectType> List<FilterableSearchItemWrapper> createAttributeSearchItemWrappers() {

        List<FilterableSearchItemWrapper> itemsList = new ArrayList<>();

        ResourceObjectDefinition ocDef = null;
        try {

            if (getKind() != null) {

                ocDef = getDefinitionByKind();

            } else if (getObjectClass() != null) {
                ocDef = getDefinitionByObjectClass();

            }
        } catch (SchemaException | ConfigurationException e) {
            warn("Could not get determine object definition");
            return itemsList;
        }

        if (ocDef == null) {
            return itemsList;
        }

        for (ResourceAttributeDefinition def : ocDef.getAttributeDefinitions()) {
//            itemsList.add(SearchFactory.createPropertySearchItemWrapper(ShadowType.class,
//                    new SearchItemType().path(new ItemPathType(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)))), //TODO visible by default
//                    def, null, getPageBase()));
        }

        return itemsList;
    }

    private ItemName getAttributeName(ResourceAttributeDefinition def) {
        return def.getItemName();
    }

    @Override
    protected ModelExecuteOptions createModelOptions() {
        return null;
    }

    @Override
    protected void initShadowStatistics(WebMarkupContainer totals) {
        totals.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return false;
            }
        });
    }

}
