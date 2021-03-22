/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
public class ObjectClassSearchItem extends PropertySearchItem<QName> {

    private static final long serialVersionUID = 1L;
    private String lastResourceOid;

    private final IModel<List<SearchItem>> itemsModel;

    public ObjectClassSearchItem(Search search, SearchItemDefinition definition,  @NotNull IModel<List<SearchItem>> itemsModel) {
        super(search, definition);
        this.itemsModel = itemsModel;
    }

    public ObjectClassSearchItem(Search search, SearchItemDefinition definition) {
        super(search, definition);
        this.itemsModel = Model.ofList(search.getItems());
    }

    @Override
    public List<DisplayableValue<QName>> getAllowedValues(PageBase pageBase) {
        List<DisplayableValue<QName>> list = new ArrayList<>();
        for (SearchItem item : itemsModel.getObject()) {
            if (item instanceof PropertySearchItem) {
                PropertySearchItem property = (PropertySearchItem) item;
                if (ShadowType.F_RESOURCE_REF.equivalent(property.getPath())
                        && property.getValue() != null && property.getValue().getValue() != null) {
                    Referencable ref = (Referencable) property.getValue().getValue();
                    Task task = pageBase.createSimpleTask("load resource");
                    if (StringUtils.isNotBlank(ref.getOid())) {
                        PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ref, pageBase,
                                task, task.getResult());
                        if (resource != null) {
                            lastResourceOid = resource.getOid();
                            List<QName> objectClasses = WebComponentUtil.loadResourceObjectClassValues(resource.asObjectable(), pageBase);
                            for (QName objectClass : objectClasses) {
                                list.add(new SearchValue(objectClass, pageBase.createStringResource(objectClass.getLocalPart()).getString()));
                            }
                        }
                        break;
                    }
                }
            }

        }
        return list;
    }

    @Override
    public Type getSearchItemType() {
        return Type.ENUM;
    }

    @Override
    public DisplayableValue<QName> getValue() {
        for (PropertySearchItem property : getSearch().getPropertyItems()) {
            if (existResourceValue(property)) {
                return super.getValue();
            }
        }
        for (SearchItem item : getSearch().getSpecialItems()) {
            if (item instanceof PropertySearchItem && existResourceValue((PropertySearchItem) item)) {
                return super.getValue();
            }
        }
        return new SearchValue<>();
    }

    private boolean existResourceValue(PropertySearchItem property) {
        if (ShadowType.F_RESOURCE_REF.equivalent(property.getPath())
                && property.getValue() != null && property.getValue().getValue() != null) {
            Referencable ref = (Referencable) property.getValue().getValue();
            return StringUtils.isNotBlank(ref.getOid())
                    && ref.getOid().equals(lastResourceOid);
        }
        return false;
    }

    @Override
    protected String getTitle(PageBase pageBase) {
        for (PropertySearchItem property : getSearch().getPropertyItems()) {
            if (ShadowType.F_RESOURCE_REF.equivalent(property.getPath())
                    && property.getValue() != null && property.getValue().getValue() != null) {
                Referencable ref = (Referencable) property.getValue().getValue();
                if (StringUtils.isNotBlank(ref.getOid())) {
                    return super.getTitle(pageBase);
                }
            }
        }
        return pageBase.createStringResource("ObjectClassSearchItem.notFoundResourceItemSearchPanel").getString();
    }
}
