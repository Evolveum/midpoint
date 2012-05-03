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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class NodeDtoProvider extends SortableDataProvider<NodeDto> {

    private static final String OPERATION_LIST_NODES = "taskDtoProvider.listNodes";
    private static final String OPERATION_COUNT_NODES = "taskDtoProvider.countNodes";
    private PageBase page;
    private QueryType query;
    private List<NodeDto> availableData;

    private static final long ALLOWED_CLUSTER_INFO_AGE = 1200L;

    public NodeDtoProvider(PageBase page) {
        Validate.notNull(page, "Page must not be null.");
        this.page = page;

        setSort("name", SortOrder.ASCENDING);
    }

    private TaskManager getTaskManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskManager();
    }

    @Override
    public Iterator<? extends NodeDto> iterator(int first, int count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_NODES);
        try {
            SortParam sortParam = getSort();
            OrderDirectionType order;
            if (sortParam.isAscending()) {
                order = OrderDirectionType.ASCENDING;
            } else {
                order = OrderDirectionType.DESCENDING;
            }

            PagingType paging = PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());

            TaskManager manager = getTaskManager();
            ClusterStatusInformation info = manager.getRunningTasksClusterwide(ALLOWED_CLUSTER_INFO_AGE);
            List<Node> nodes = manager.searchNodes(query, paging, info, result);

            for (Node node : nodes) {
                getAvailableData().add(new NodeDto(node));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list tasks.", ex);
        }

        return getAvailableData().iterator();
    }

    @Override
    public int size() {
        try {
            return getTaskManager().countNodes(query, new OperationResult("size"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    public QueryType getQuery() {
        return query;
    }

    public void setQuery(QueryType query) {
        this.query = query;
    }

    @Override
    public IModel<NodeDto> model(NodeDto object) {
        return new Model<NodeDto>(object);
    }

    public List<NodeDto> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<NodeDto>();
        }
        return availableData;
    }
}
