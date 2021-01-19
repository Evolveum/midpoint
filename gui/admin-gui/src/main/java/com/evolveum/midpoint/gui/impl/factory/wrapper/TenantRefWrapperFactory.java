/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.Function;

/**
 * @author skublik
 */

@Component
public class TenantRefWrapperFactory extends PrismReferenceWrapperFactory<ObjectReferenceType> implements Serializable{

    private static final long serialVersionUID = 1L;

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return match(def) && QNameUtil.match(AssignmentType.F_TENANT_REF, def.getItemName());
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected PrismReferenceWrapper<ObjectReferenceType> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismReference item, ItemStatus status, WrapperContext ctx) {
        PrismReferenceWrapper<ObjectReferenceType> wrapper =  super.createWrapperInternal(parent, item, status, ctx);

        wrapper.setSpecialSearchItemFunctions(Collections.singleton((Function<Search, SearchItem> & Serializable) search -> createTenantSearchItem(search)));
        return wrapper;
    }

    private SearchItem createTenantSearchItem(Search search) {
        PrismPropertyDefinition tenantDef = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(OrgType.class)
                .findPropertyDefinition(OrgType.F_TENANT);
        PropertySearchItem<Boolean> item = new PropertySearchItem<>(search, new SearchItemDefinition(OrgType.F_TENANT, tenantDef, null)) {
            @Override
            protected boolean canRemoveSearchItem() {
                return false;
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        };
        item.setValue(new SearchValue<Boolean>(Boolean.TRUE, "Boolean.TRUE"));
        return item;
    }
}
