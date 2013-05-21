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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import org.apache.wicket.Component;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class NodeDtoProvider extends BaseSortableDataProvider<NodeDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(NodeDtoProvider.class);
    private static final String DOT_CLASS = NodeDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_NODES = DOT_CLASS + "listNodes";
    private static final String OPERATION_COUNT_NODES = DOT_CLASS + "countNodes";

    private static final long ALLOWED_CLUSTER_INFO_AGE = 1200L;

    public NodeDtoProvider(Component component) {
        super(component);
    }

    @Override
    public Iterator<? extends NodeDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_NODES);
        try {
//            SortParam sortParam = getSort();
//            OrderDirectionType order;
//            if (sortParam.isAscending()) {
//                order = OrderDirectionType.ASCENDING;
//            } else {
//                order = OrderDirectionType.DESCENDING;
//            }
//
//            PagingType paging = PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());
        	ObjectPaging paging = createPaging(first, count);
        	ObjectQuery query = getQuery();
        	if (query == null){
        		query = new ObjectQuery();
        	}
        	query.setPaging(paging);

            TaskManager manager = getTaskManager();
            ClusterStatusInformation info = manager.getRunningTasksClusterwide(ALLOWED_CLUSTER_INFO_AGE, result);
            List<Node> nodes = manager.searchNodes(query, info, result);

            for (Node node : nodes) {
                getAvailableData().add(new NodeDto(node));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing nodes", ex);
            result.recordFatalError("Couldn't list nodes.", ex);
        }

        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_NODES);
        try {
            count = getTaskManager().countNodes(getQuery(), result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count nodes.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return count;
    }
}
