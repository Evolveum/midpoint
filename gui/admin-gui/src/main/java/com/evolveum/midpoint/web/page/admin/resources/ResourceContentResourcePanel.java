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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinitionImpl;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.wicket.model.StringResourceModel;

public class ResourceContentResourcePanel extends ResourceContentPanel {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ResourceContentResourcePanel.class.getName() + ".";

    public ResourceContentResourcePanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
            QName objectClass, ShadowKindType kind, String intent, String searchMode, PageBase pageBase) {
        super(id, resourceModel, objectClass, kind, intent, searchMode, pageBase);
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
        List<SearchItemDefinition> availableDefs = new ArrayList<>();
        availableDefs.addAll(createAttributeDefinitionList());
        return new Search(ShadowType.class, availableDefs);
    }

    @Override
    protected String getTargetOperationIndication() {
        return "";
    }

    private <T extends ObjectType> List<SearchItemDefinition> createAttributeDefinitionList() {

        List<SearchItemDefinition> map = new ArrayList<>();

        RefinedObjectClassDefinition ocDef = null;
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
            map.add(new SearchItemDefinition(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)), def, null));
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
