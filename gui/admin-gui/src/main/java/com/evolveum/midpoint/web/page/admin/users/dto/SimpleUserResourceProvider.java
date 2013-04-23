/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.*;

/**
 * @author lazyman
 */
public class SimpleUserResourceProvider extends BaseSortableDataProvider<SelectableBean<ResourceType>> {

    private static final String DOT_CLASS = SimpleUserResourceProvider.class.getName() + ".";
    private static final String OPERATION_LIST_RESOURCES = DOT_CLASS + "listResources";
    private static final String OPERATION_COUNT_RESOURCES = DOT_CLASS + "countResources";

    private ObjectDataProvider resourceProvider;
    private IModel<List<UserAccountDto>> accountsModel;

    public SimpleUserResourceProvider(Component component, IModel<List<UserAccountDto>> accountsModel) {
        super(component);
        Validate.notNull(accountsModel, "Accounts model must not be null.");
        this.accountsModel = accountsModel;

        resourceProvider = new ObjectDataProvider(component, ResourceType.class);
    }

    @Override
    public Iterator<SelectableBean<ResourceType>> internalIterator(long first, long count) {
        getAvailableData().clear();

        Set<String> alreadyUsedResources = createUsedResourceOidSet();

        List<SelectableBean<ResourceType>> allData = new ArrayList<SelectableBean<ResourceType>>();
        Iterator<SelectableBean<ResourceType>> iterator = resourceProvider.iterator(0, resourceProvider.size());
        while (iterator.hasNext()) {
            SelectableBean<ResourceType> bean = iterator.next();
            if (alreadyUsedResources.contains(bean.getValue().getOid())) {
                continue;
            }
            allData.add(bean);
        }

        for (long i = first; (i < first + count) && (allData.size() > i); i++) {
            getAvailableData().add(allData.get(WebMiscUtil.safeLongToInteger(i)));
        }

        return getAvailableData().iterator();
    }

    private Set<String> createUsedResourceOidSet() {
        Set<String> set = new HashSet<String>();

        List<UserAccountDto> accounts = accountsModel.getObject();
        if (accounts == null) {
            return set;
        }

        for (UserAccountDto account : accounts) {
            PrismObject<ShadowType> prismAccount = account.getObject().getObject();
            PrismReference resourceRef = prismAccount.findReference(ShadowType.F_RESOURCE_REF);
            if (resourceRef == null || resourceRef.getValue() == null) {
                continue;
            }

            PrismReferenceValue value = resourceRef.getValue();
            set.add(value.getOid());
        }

        return set;
    }

    @Override
    protected int internalSize() {
        long count = resourceProvider.size();
        Set<String> alreadyUsedResources = createUsedResourceOidSet();
        count -= alreadyUsedResources.size();

        return WebMiscUtil.safeLongToInteger(count);
    }
}
