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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.search.ContainerTypeSearchItem;

import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.refactored.AbstractSearchItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.search.refactored.Search;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
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

    @Override
    protected Search createSearch() {
//        List<SearchItemDefinition> availableDefs = new ArrayList<>();
//        availableDefs.addAll(createAttributeDefinitionList());
//        return new Search(new ContainerTypeSearchItem(ShadowType.class), availableDefs);
        return SearchFactory.createSearchNew(ShadowType.class, null, createAttributeSearchItemWrappers(), getPageBase());
    }

    //    private <T extends ObjectType> List<SearchItemDefinition> createAttributeDefinitionList() {
//
//        List<SearchItemDefinition> map = new ArrayList<>();
//
//        RefinedObjectClassDefinition ocDef = null;
//        try {
//
//            if (getKind() != null) {
//
//                ocDef = getDefinitionByKind();
//
//            } else if (getObjectClass() != null) {
//                ocDef = getDefinitionByObjectClass();
//
//            }
//        } catch (SchemaException e) {
//            warn("Could not get determine object class definition");
//            return map;
//        }
//
//        if (ocDef == null) {
//            return map;
//        }
//
//        for (ResourceAttributeDefinition def : ocDef.getAttributeDefinitions()) {
//            map.add(new SearchItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)), def, null));
//        }
//
//        return map;
//    }
    
    private <T extends ObjectType> List<? super AbstractSearchItemWrapper> createAttributeSearchItemWrappers() {

        List<? super AbstractSearchItemWrapper> map = new ArrayList<>();

        ResourceObjectDefinition ocDef = null;
        try {

            if (getKind() != null) {

                ocDef = getDefinitionByKind();

            } else if (getObjectClass() != null) {
                ocDef = getDefinitionByObjectClass();

            }
        } catch (SchemaException e) {
            warn("Could not get determine object class definition");
            return map;
        }

        if (ocDef == null) {
            return map;
        }

        for (ResourceAttributeDefinition def : ocDef.getAttributeDefinitions()) {
            SearchItemType searchItem = new SearchItemType()
                    .path(new ItemPathType(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def))))
                    .displayName(WebComponentUtil.getItemDefinitionDisplayNameOrName(def, ResourceContentResourcePanel.this));
            map.add(SearchFactory.createPropertySearchItemWrapper(ShadowType.class, def, searchItem, null));
        }

        return map;
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
