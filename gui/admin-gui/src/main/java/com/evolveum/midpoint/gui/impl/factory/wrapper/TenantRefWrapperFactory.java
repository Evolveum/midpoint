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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;

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
        wrapper.setPredefinedSearchItem(Set.of(createTenantSearchItem()));
//        wrapper.setSpecialSearchItemFunctions(Collections.singleton(this::createTenantSearchItem));
        return wrapper;
    }

    private SearchItemType createTenantSearchItem() {
        SearchItemType searchItem = new SearchItemType();
        searchItem.setPath(new ItemPathType(OrgType.F_TENANT));
        searchItem.setVisibleByDefault(true);
        SearchFilterType searchFilter = new SearchFilterType();
        searchFilter.setText("tenant = true");
        searchItem.setFilter(searchFilter);
        return searchItem;
//        PrismPropertyDefinition tenantDef = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(OrgType.class)
//                .findPropertyDefinition(OrgType.F_TENANT);
//        SearchItemType searchItem = new SearchItemType()
//                .path(new ItemPathType(ItemPath.create(OrgType.F_TENANT)))
//                .displayName(WebComponentUtil.getItemDefinitionDisplayNameOrName(tenantDef, null));
//        ChoicesSearchItemWrapper searchItemWrapper = new ChoicesSearchItemWrapper(ItemPath.create(OrgType.F_TENANT),
//                Collections.singletonList(new SearchValue<Boolean>(Boolean.TRUE, "Boolean.TRUE")))  {
//            @Override
//            public boolean canRemoveSearchItem() {
//                return false;
//            }
//
//            @Override
//            public boolean isEnabled() {
//                return false;
//            }
//        };
//        searchItemWrapper.setValue(new SearchValue<Boolean>(Boolean.TRUE, "Boolean.TRUE"));
//        return searchItemWrapper;
    }
}
