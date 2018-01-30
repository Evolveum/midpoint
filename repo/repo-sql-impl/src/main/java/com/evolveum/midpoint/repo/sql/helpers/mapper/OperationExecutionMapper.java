/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.ROperationExecution;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationExecutionMapper extends ContainerMapper<OperationExecutionType, ROperationExecution> {

    @Override
    public ROperationExecution map(OperationExecutionType input, MapperContext context) {
        ROperationExecution execution = new ROperationExecution();

        RObject owner = (RObject) context.getOwner();

        RepositoryContext repositoryContext =
                new RepositoryContext(context.getRepositoryService(), context.getPrismContext());

        try {
            ROperationExecution.copyFromJAXB(input, execution, owner, repositoryContext);
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate trigger to entity", ex);
        }

        return execution;
    }
}
