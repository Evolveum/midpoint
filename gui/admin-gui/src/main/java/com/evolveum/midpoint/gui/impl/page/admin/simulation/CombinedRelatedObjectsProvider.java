package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Just and extension of {@link RelatedObjectsProvider}, which prepends currently displayed processed objects into
 * list of related objects (for better navigation).
 */
public class CombinedRelatedObjectsProvider extends RelatedObjectsProvider {

    private IModel<SimulationResultProcessedObjectType> self;

    public CombinedRelatedObjectsProvider(Component component, @NotNull IModel<Search<SimulationResultProcessedObjectType>> searchModel,
            IModel<SimulationResultProcessedObjectType> self) {

        super(component, searchModel);

        this.self = self;
    }

    @Override
    protected List<SimulationResultProcessedObjectType> searchObjects(Class<SimulationResultProcessedObjectType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
        List<SimulationResultProcessedObjectType> results = super.searchObjects(type, query, options, task, result);

        boolean prependSelf = false;
        if (self.getObject() != null && query != null && query.getPaging() != null) {
            ObjectPaging paging = query.getPaging();
            if (Objects.equals(paging.getOffset(), 0)) {
                prependSelf = true;
            }
        }

        if (!prependSelf) {
            return results;
        }

        List<SimulationResultProcessedObjectType> list = new ArrayList<>();
        list.add(self.getObject());
        list.addAll(results);

        return list;
    }

    @Override
    protected Integer countObjects(Class<SimulationResultProcessedObjectType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) throws CommonException {
        Integer count = super.countObjects(type, query, currentOptions, task, result);

        return self.getObject() != null ? count + 1 : count;
    }

    @Override
    public ObjectPaging createPaging(long offset, long pageSize) {
        if (self.getObject() != null) {
            if (offset == 0) {
                pageSize = pageSize - 1;
            } else {
                offset = offset - 1;
            }
        }

        return super.createPaging(offset, pageSize);
    }
}
