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

package com.evolveum.midpoint.report;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Component
public class ReportManagerImpl implements ReportManager {

    @Autowired
    private ModelService modelService;
    @Autowired
    private PrismContext prismContext;

    @Override
    public String addReport(PrismObject<ReportType> object, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PrismObject<ReportType> getReport(String oid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<PrismObject<ReportType>> searchReports(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int countReports(ObjectQuery query, OperationResult parentResult) throws SchemaException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void modifyReport(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteReport(String oid, OperationResult parentResult) throws ObjectNotFoundException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createReport(PrismObject<ReportType> report, OperationResult parentResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
