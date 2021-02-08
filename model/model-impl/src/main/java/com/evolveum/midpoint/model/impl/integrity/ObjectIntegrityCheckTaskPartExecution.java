/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import java.util.Collection;
import java.util.Map;

import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.task.DefaultHandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@ItemProcessorClass(ObjectIntegrityCheckItemProcessor.class)
@DefaultHandledObjectType(ObjectType.class)
public class ObjectIntegrityCheckTaskPartExecution
        extends AbstractIterativeModelTaskPartExecution
        <ObjectType,
                ObjectIntegrityCheckTaskHandler,
                ObjectIntegrityCheckTaskHandler.TaskExecution,
                ObjectIntegrityCheckTaskPartExecution, ObjectIntegrityCheckItemProcessor> {

    private static final int HISTOGRAM_COLUMNS = 80;

    final ObjectStatistics objectStatistics = new ObjectStatistics();

    ObjectIntegrityCheckTaskPartExecution(ObjectIntegrityCheckTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
        setRequiresDirectRepositoryAccess();
    }

    @Override
    protected void initialize(OperationResult opResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, TaskException {
        super.initialize(opResult);

        ensureNoWorkerThreads();
        logger.info("Object integrity check is starting");
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException {
        ObjectQuery query = createQueryFromTask();
        logger.info("Using query:\n{}", query.debugDump());
        return query;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> optionsFromTask = createSearchOptionsFromTask();
        return SelectorOptions.updateRootOptions(optionsFromTask, opt -> opt.setAttachDiagData(true), GetOperationOptions::new);
    }

    @Override
    protected void finish(OperationResult opResult) throws SchemaException {
        super.finish(opResult);
        logger.info("Object integrity check finished.");
        dumpStatistics();
    }

    private void dumpStatistics() {
        Map<String, ObjectTypeStatistics> map = objectStatistics.getStatisticsMap();
        if (map.isEmpty()) {
            logger.info("(no objects were found)");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, ObjectTypeStatistics> entry : map.entrySet()) {
                sb.append("\n\n**************************************** Statistics for ").append(entry.getKey()).append(" ****************************************\n\n");
                sb.append(entry.getValue().dump(HISTOGRAM_COLUMNS));
            }
            logger.info("{}", sb.toString());
        }
        logger.info("Objects processed with errors: {}", objectStatistics.getErrors());
    }
}
