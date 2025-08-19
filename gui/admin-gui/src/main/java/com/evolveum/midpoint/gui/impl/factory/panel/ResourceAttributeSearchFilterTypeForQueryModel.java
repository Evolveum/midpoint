/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

public class ResourceAttributeSearchFilterTypeForQueryModel extends SearchFilterTypeForQueryModel<ShadowType> {

    private static final long serialVersionUID = 1L;

    private final IModel<QName> objectClass;

    public ResourceAttributeSearchFilterTypeForQueryModel(
            IModel<SearchFilterType> valueWrapper, PageBase pageBase, boolean useParsing, IModel<QName> objectClass) {
        super(valueWrapper, pageBase, Model.of(ShadowType.class), useParsing);
        this.objectClass = objectClass;
    }

    protected void parseQuery(String object, boolean setValue) throws SchemaException, ConfigurationException {
        PrismObjectDefinition<ShadowType> def = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        ResourceObjectDefinition objectClassDef =
                ((PageResource) getPageBase()).getObjectDetailsModels().getRefinedSchema().findDefinitionForObjectClass(objectClass.getObject());
        PrismObjectDefinition<ShadowType> newDef = ShadowUtil.applyObjectDefinition(def, objectClassDef);

        ObjectFilter objectFilter = getPageBase().getPrismContext().createQueryParser().parseFilter(newDef, object);
        SearchFilterType filter = getPageBase().getQueryConverter().createSearchFilterType(objectFilter);
        filter.setText(object);
        if (setValue) {
            getBaseModel().setObject(filter);
        }
    }
}
