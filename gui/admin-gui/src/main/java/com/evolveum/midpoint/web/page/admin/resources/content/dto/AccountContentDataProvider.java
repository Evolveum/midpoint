/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class AccountContentDataProvider extends BaseSortableDataProvider<AccountContentDto> {

    private static final Trace LOGGER = TraceManager.getTrace(AccountContentDataProvider.class);
    private static final String DOT_CLASS = AccountContentDataProvider.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_OWNER = DOT_CLASS + "loadOwner";

    private IModel<String> resourceOid;
    private IModel<QName> objectClass;

    public AccountContentDataProvider(Component component, IModel<String> resourceOid, IModel<QName> objectClass) {
        super(component);

        Validate.notNull(resourceOid, "Resource oid model must not be null.");
        Validate.notNull(objectClass, "Object class model must not be null.");
        this.resourceOid = resourceOid;
        this.objectClass = objectClass;
    }

    @Override
    public Iterator<AccountContentDto> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            Task task = getPage().createSimpleTask(OPERATION_LOAD_ACCOUNTS);

            ObjectQuery baseQuery = ObjectQueryUtil.createResourceAndAccountQuery(resourceOid.getObject(),
                    objectClass.getObject(), getPage().getPrismContext());
            ObjectQuery query = getQuery();
            if (query != null) {
                ObjectFilter baseFilter = baseQuery.getFilter();
                ObjectFilter filter = query.getFilter();

                query = new ObjectQuery();
                ObjectFilter andFilter = AndFilter.createAnd(baseFilter, filter);
                query.setFilter(andFilter);
            } else {
                query = baseQuery;
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query filter:\n{}", query);
            }

            List<PrismObject<ShadowType>> list = getModel().searchObjects(ShadowType.class, query, null, task, result);

            AccountContentDto dto;
            for (PrismObject<ShadowType> object : list) {
                dto = createAccountContentDto(object, result);
                getAvailableData().add(dto);
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            getPage().showResultInSession(result);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        return Integer.MAX_VALUE;
    }

    private AccountContentDto createAccountContentDto(PrismObject<ShadowType> object, OperationResult result)
            throws SchemaException, SecurityViolationException {

        AccountContentDto dto = new AccountContentDto();
        dto.setAccountName(WebMiscUtil.getName(object));
        dto.setAccountOid(object.getOid());
        ShadowType shadow = object.asObjectable();

        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(object);
        if (identifiers != null) {
            List<ResourceAttribute<?>> idList = new ArrayList<ResourceAttribute<?>>();
            idList.addAll(identifiers);
            dto.setIdentifiers(idList);
        }

        PrismObject<UserType> owner = loadOwner(dto.getAccountOid(), result);
        if (owner != null) {
            dto.setOwnerName(WebMiscUtil.getName(owner));
            dto.setOwnerOid(owner.getOid());
        }

        dto.setSituation(WebMiscUtil.getValue(object, ShadowType.F_SYNCHRONIZATION_SITUATION,
                SynchronizationSituationType.class));

        dto.setKind(shadow.getKind());
        dto.setIntent(shadow.getIntent());
        dto.setObjectClass(shadow.getObjectClass().getLocalPart());

        addInlineMenuToDto(dto);

        return dto;
    }

    protected void addInlineMenuToDto(AccountContentDto dto) {
    }

    private PrismObject<UserType> loadOwner(String accountOid, OperationResult result)
            throws SecurityViolationException, SchemaException {

        Task task = getPage().createSimpleTask(OPERATION_LOAD_OWNER);
        try {
            return getModel().findShadowOwner(accountOid, task, result);
        } catch (ObjectNotFoundException ex) {
            //owner was not found, it's possible and it's ok on unlinked accounts
        }
        return null;
    }

    @Override
    public boolean isSizeAvailable() {
        return false;
    }
}
