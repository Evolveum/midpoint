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

package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class AccountContentDataProvider extends BaseSortableDataProvider<SelectableBean<AccountContentDto>> {

    private static final Trace LOGGER = TraceManager.getTrace(AccountContentDataProvider.class);
    private static final String DOT_CLASS = AccountContentDataProvider.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";

    private IModel<PrismObject<ResourceType>> model;
    private String resourceOid;
    private QName objectClass;

    public AccountContentDataProvider(PageBase page, IModel<PrismObject<ResourceType>> model) {
        super(page);

        Validate.notNull(model, "Model with resource object must not be null.");
        this.model = model;
    }

    @Override
    public Iterator<? extends SelectableBean<AccountContentDto>> iterator(int first, int count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        try {
//            PagingType paging = createPaging(first, count);
//            Task task = getPage().createSimpleTask(OPERATION_LOAD_ACCOUNTS);
//
//            QueryType query = null;
//            List<PrismObject<AccountShadowType>> list = getModel().searchObjects(AccountShadowType.class, query, paging, task, result);
//
            AccountContentDto dto;
//            for (PrismObject<AccountShadowType> object : list) {
//
//                dto = new AccountContentDto();
//                dto.setAccountName(WebMiscUtil.getName(object));
//                dto.setAccountOid(object.getOid());
//
//                dto.setIdentifiers(Arrays.asList("identifier1", "identifier2"));
//                dto.setOwnerName("Administrator");
//                dto.setOwnerOid("00000000-0000-0000-0000-000000000002");
//                dto.setSituation(WebMiscUtil.getValue(object, ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION,
//                        SynchronizationSituationType.class));
//
//                getAvailableData().add(new SelectableBean<AccountContentDto>(dto));
//            }

            dto = new AccountContentDto();
            dto.setAccountName("uid=lazyman,ou=People,dc=example,dc=com");
            dto.setAccountOid("ffffffff-0000-0000-0000-000000000002");
            dto.setIdentifiers(Arrays.asList("identifier1", "identifier2"));
            dto.setOwnerName("Administrator");
            dto.setOwnerOid("00000000-0000-0000-0000-000000000002");
            dto.setSituation(SynchronizationSituationType.LINKED);
            getAvailableData().add(new SelectableBean<AccountContentDto>(dto));

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResultInSession(result);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        return Integer.MAX_VALUE;
    }

    private String getResourceOid() {
        if (StringUtils.isNotEmpty(resourceOid)) {
            return resourceOid;
        }

        PrismObject<ResourceType> resource = this.model.getObject();
        resourceOid = resource.getOid();

        return resourceOid;
    }

    private QName getObjectClass() throws SchemaException {
        if (objectClass != null) {
            return objectClass;
        }

        MidPointApplication application = (MidPointApplication) getPage().getApplication();
        PrismObject<ResourceType> resource = this.model.getObject();
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, application.getPrismContext());
        Collection<ObjectClassComplexTypeDefinition> list = resourceSchema.getObjectClassDefinitions();
        if (list != null) {
            for (ObjectClassComplexTypeDefinition def : list) {
                if (def.isDefaultAccountType()) {
                    this.objectClass = def.getTypeName();
                    break;
                }
            }
        }

        return objectClass;
    }
}
