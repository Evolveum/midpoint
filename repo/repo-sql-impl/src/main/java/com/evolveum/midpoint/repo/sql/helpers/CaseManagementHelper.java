/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetContainerableIdOnlyResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Contains methods specific to handle case management work items.
 * It is quite a temporary solution in order to ease SqlRepositoryServiceImpl
 * from tons of type-specific code. Serious solution would be to implement
 * subobject-level operations more generically.
 *
 * @author mederly
 */
@Component
public class CaseManagementHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CaseManagementHelper.class);

    @Autowired private ObjectRetriever objectRetriever;

    // TODO find a better name
    public CaseWorkItemType updateLoadedCaseWorkItem(GetContainerableIdOnlyResult result, Map<String, PrismObject<CaseType>> ownersMap,
			Collection<SelectorOptions<GetOperationOptions>> options,
			Session session, OperationResult operationResult)
		    throws SchemaException, ObjectNotFoundException, DtoTranslationException {

        String ownerOid = result.getOwnerOid();
        PrismObject<CaseType> aCase = resolveCase(ownerOid, ownersMap, session, operationResult);
	    PrismContainer<Containerable> workItemContainer = aCase.findContainer(CaseType.F_WORK_ITEM);
	    if (workItemContainer == null) {
	    	throw new ObjectNotFoundException("Case " + aCase + " has no work items even if it should have " + result);
	    }
	    PrismContainerValue<?> workItemPcv = workItemContainer.findValue(result.getId());
	    if (workItemPcv == null) {
		    throw new ObjectNotFoundException("Case " + aCase + " has no work item " + result);
	    }
	    return (CaseWorkItemType) workItemPcv.asContainerable();
    }

    @NotNull
    private PrismObject<CaseType> resolveCase(String caseOid, Map<String, PrismObject<CaseType>> cache, Session session,
			OperationResult operationResult) throws DtoTranslationException, ObjectNotFoundException, SchemaException {
        PrismObject<CaseType> aCase = cache.get(caseOid);
        if (aCase != null) {
            return aCase;
        }
	    aCase = objectRetriever.getObjectInternal(session, CaseType.class, caseOid, null, false, operationResult);
        cache.put(caseOid, aCase);
        return aCase;
    }

}
