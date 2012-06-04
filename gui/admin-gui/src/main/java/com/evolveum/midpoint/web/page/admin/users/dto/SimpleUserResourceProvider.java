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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
public class SimpleUserResourceProvider extends BaseSortableDataProvider<SelectableBean<ResourceType>> {

    private static final String DOT_CLASS = SimpleUserResourceProvider.class.getName() + ".";
    private static final String OPERATION_LIST_RESOURCES = DOT_CLASS + "listResources";
    private static final String OPERATION_COUNT_RESOURCES = DOT_CLASS + "countResources";

    private IModel<List<UserAccountDto>> accountsModel;

    public SimpleUserResourceProvider(PageBase page, IModel<List<UserAccountDto>> accountsModel) {
        super(page);
        Validate.notNull(accountsModel, "Accounts model must not be null.");
        this.accountsModel = accountsModel;
    }

    @Override
    public Iterator<SelectableBean<ResourceType>> iterator(int first, int count) {
        getAvailableData().clear();

        Set<String> alreadyUsedResources = createUsedResourceOidSet();

        OperationResult result = new OperationResult(OPERATION_LIST_RESOURCES);
        try {
            PagingType paging = createPaging(first, count);
            Task task = getPage().createSimpleTask(OPERATION_LIST_RESOURCES);

            List<PrismObject<ResourceType>> list = getModel().searchObjects(ResourceType.class,
                    getQuery(), paging, task, result);
            for (PrismObject<ResourceType> object : list) {
                if (alreadyUsedResources.contains(object.getOid())) {
                    continue;
                }
                getAvailableData().add(new SelectableBean<ResourceType>(object.asObjectable()));
            }

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
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
            PrismObject<AccountShadowType> prismAccount = account.getObject().getObject();
            PrismReference resourceRef = prismAccount.findReference(AccountShadowType.F_RESOURCE_REF);
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
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_RESOURCES);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_RESOURCES);
            count = getModel().countObjects(ResourceType.class, getQuery(), task, result);

            Set<String> alreadyUsedResources = createUsedResourceOidSet();
            count -= alreadyUsedResources.size();

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return count;
    }
}
