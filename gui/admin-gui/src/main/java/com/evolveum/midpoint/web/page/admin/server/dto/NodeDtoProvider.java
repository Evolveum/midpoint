/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.apache.wicket.Component;

import java.util.*;

/**
 * @author lazyman
 */
public class NodeDtoProvider extends BaseSortableDataProvider<NodeDto> {

    private static final Trace LOGGER = TraceManager.getTrace(NodeDtoProvider.class);
    private static final String DOT_CLASS = NodeDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_NODES = DOT_CLASS + "listNodes";
    private static final String OPERATION_COUNT_NODES = DOT_CLASS + "countNodes";

    public NodeDtoProvider(Component component) {
        super(component);
    }

    @Override
    public Iterator<? extends NodeDto> internalIterator(long first, long count) {
        Collection<String> selectedOids = getSelectedOids();
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_NODES);
        Task task = getTaskManager().createTaskInstance(OPERATION_LIST_NODES);
        try {
            ObjectPaging paging = createPaging(first, count);
            ObjectQuery query = getQuery();
            if (query == null) {
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            List<PrismObject<NodeType>> nodes = getModel().searchObjects(NodeType.class, query, getDefaultOptionsBuilder().build(), task, result);

            for (PrismObject<NodeType> node : nodes) {
                getAvailableData().add(createNodeDto(node));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing nodes", ex);
            result.recordFatalError(getPage().createStringResource("NodeDtoProvider.message.internalIterator.fatalError").getString(), ex);
        }

        setSelectedOids(selectedOids);
        return getAvailableData().iterator();
    }

    private Collection<String> getSelectedOids() {
        Set<String> oids = new HashSet<>();
        for (NodeDto nodeDto : getAvailableData()) {
            if (nodeDto.isSelected()) {
                oids.add(nodeDto.getOid());
            }
        }
        return oids;
    }

    private void setSelectedOids(Collection<String> selectedOids) {
        for (NodeDto nodeDto : getAvailableData()) {
            if (selectedOids.contains(nodeDto.getOid())) {
                nodeDto.setSelected(true);
            }
        }
    }


    public NodeDto createNodeDto(PrismObject<NodeType> node) {
        return new NodeDto(node.asObjectable());
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_NODES);
        Task task = getTaskManager().createTaskInstance(OPERATION_COUNT_NODES);
        try {
            count = getModel().countObjects(NodeType.class, getQuery(), getDefaultOptionsBuilder().build(), task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when counting nodes", ex);
            result.recordFatalError(getPage().createStringResource("NodeDtoProvider.message.internalSize.fatalError").getString(), ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return count;
    }
}
