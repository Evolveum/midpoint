/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.ItemPathParser;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

public class RepoAssignmentListProvider extends ContainerListDataProvider<AssignmentType> {

    private static final long serialVersionUID = 1L;

    public static final String TARGET_NAME_STRING = "targetRef.targetName.orig";
    private static final ItemPath TARGET_NAME_PATH = ItemPath.create(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
    private static final ItemPath TARGET_REF_OBJ = ItemPath.create(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE);



    private final IModel<List<PrismContainerValueWrapper<AssignmentType>>> model;
    private final String oid;
    private final Class<? extends Objectable> objectType;
    private final ItemPath path;

    private transient List<PrismContainerValueWrapper<AssignmentType>> newData;

    public RepoAssignmentListProvider(Component component, @NotNull IModel<Search<AssignmentType>> search, IModel<List<PrismContainerValueWrapper<AssignmentType>>> model,
                Class<? extends Objectable>  objectType, String oid, ItemPath path) {
        super(component, search);
        this.model = model;
        this.oid = oid;
        this.objectType = objectType;
        this.path = path;
        setSort(new SortParam(TARGET_NAME_STRING, true));
    }

    @Override
    public Class<AssignmentType> getType() {
        return AssignmentType.class;
    }

    protected List<PrismContainerValueWrapper<AssignmentType>> postFilter(List<PrismContainerValueWrapper<AssignmentType>> assignmentList) {
        return assignmentList;
    }

    protected ObjectFilter postFilterIds() {
        List<PrismContainerValueWrapper<AssignmentType>> data = model.getObject();
        List<PrismContainerValueWrapper<AssignmentType>> filtered = postFilter(data);

        if (data == filtered) {
            // No filtering were done
            return null;
        }
        var builder = getPrismContext().queryFor(AssignmentType.class);
        if (filtered.isEmpty()) {
            return builder.none().buildFilter();
        }
        List<Long> idList = filtered.stream()
            .filter(i -> i.getRealValue() != null)
            .map(i -> i.getRealValue().getId())
            .filter(i -> i != null)
            .collect(Collectors.toList());

        long[] ids = new long[idList.size()];
        int i = 0;
        for (Long item : idList) {
            ids[i] = item;
            i++;
        }
        return builder.id(ids).buildFilter();
    }


    @Override
    public Iterator<? extends PrismContainerValueWrapper<AssignmentType>> internalIterator(long first, long count) {
        getAvailableData().clear();
        initChangeLists();
        // FIXME: Sort new data

        var newData = this.newData;
        // If current page is inside new data, we add them to result list
        if (first < newData.size()) {
            for (var wrapper : newData.subList((int) first, (int) Math.min(newData.size(), first + count))) {
                getAvailableData().add(wrapper);
            }
        }
        // If there are still less data, then
        if (getAvailableData().size() < count) {
            // We get offset for repo search
            long repoFirst = Math.max(0, first - newData.size());
            // We search and populate availableData from repository
            // removed data are handled in #getQuery call.
            doRepositoryIteration(repoFirst, count - getAvailableData().size());
        }
        return getAvailableData().iterator();
    }


    /**
     * Added and deleted values needs to be treated specially.
     * This method performs walk of model list and updates internal state,
     * to have newData list and set of deleted ids.
     *
     *   Added values are not in repository - so repository search can not detect them,
     *   we prepend them to search result list (see {@link #internalIterator(long, long)}.
     *
     */
    private void initChangeLists() {
        newData = new ArrayList<>();
        for (PrismContainerValueWrapper<AssignmentType> wrapper : model.getObject()) {
            if (ValueStatus.ADDED.equals(wrapper.getStatus())) {
                newData.add(wrapper);
            }
        }
    }

    @Override
    protected int internalSize() {
        initChangeLists();
        return newData.size() + super.internalSize();
    }

    @Override
    protected PrismContainerValueWrapper<AssignmentType> createWrapper(AssignmentType object, Task task,
            OperationResult result) throws SchemaException {
        for (PrismContainerValueWrapper<AssignmentType> item : model.getObject()) {
            if (Objects.equals(item.getRealValue().getId(),object.getId())) {
                postProcessWrapper(item);
                return item;
            }
        }
        // Could possibly, if user was modified in other session.
        // Lets silently skip the item for now (by returning null)
        return null;
    }

    protected void postProcessWrapper(PrismContainerValueWrapper<AssignmentType> valueWrapper) {
        AssignmentType assignmentType = valueWrapper.getRealValue();
        if (assignmentType == null) {
            return;
        }
        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null || targetRef.getObject() != null) {
            return;
        }

        PrismObject<? extends ObjectType> object = WebModelServiceUtils.loadObject(targetRef, getPageBase());
        targetRef.asReferenceValue().setObject(object);
    }

    @Override
    protected @NotNull List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
        if (sortParam == null) {
            return super.createObjectOrderings(sortParam);
        }
        String property = sortParam.getProperty();
        ItemPath path;
        if (TARGET_NAME_STRING.equals(property)) {
            path = TARGET_NAME_PATH;
        } else if (property.contains("/")) {
            // Try to parse it as item path for now
            path = getPrismContext().itemPathParser().asItemPath(property);
        } else {
            path = ItemPath.create(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty()));
        }

        if (path.startsWith(TARGET_REF_OBJ) && determineTargetRefType() != null) {
            // we have more concrete targetRef type and we are sorting using targetRef/@
            var afterDereference = path.rest(2);
            var typeHint = determineTargetRefType();
            path = ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(typeHint));
            path = path.append(afterDereference);
        }

        OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
        return Collections.singletonList(
                getPrismContext().queryFactory().createOrdering(path, order));
    }

    /**
     * Returns query for Data Provider
     *
     * This implementation rewrites query a bit:
     *   - Adds ownedBy filter for parent object
     *   - Optionally adds id filter if AssignmentPanel has postFilter implemented
     *
     */
   @Override
    public ObjectQuery getQuery() {
        var customizeQuery = getCustomizeContentQuery();
        var orig = normalizedSearchQuery();
        orig = mergeQueries(orig, customizeQuery);
        var idFilter = postFilterIds();
        ObjectFilter filter = orig != null ? orig.getFilter() : null;
        if (orig != null) {
            // We have user entered filter
            if (idFilter != null) {
                // PostFilter filtered data, so we need to search only in these data
                filter = getPrismContext().queryFor(getType())
                        .filter(filter)
                        .and().filter(idFilter)
                        .buildFilter();
            }
        } else {
            //
            filter = idFilter;
        }

        if (filter != null) {
            return getPrismContext().queryFor(AssignmentType.class)
                .filter(filter)
                .and()
                    .ownedBy(objectType, path)
                    .id(oid)
                .build();
        }
        return getPrismContext().queryFor(AssignmentType.class)
                .ownedBy(objectType, path)
                    .id(oid)
                .build();
    }

    private QName determineTargetRefType() {
        var searchOverride = getSearchModel().getObject().getContainerDefinitionOverride();
        if (searchOverride == null) {
            return null;
        }
        var targetRefDef = searchOverride.findReferenceDefinition(AssignmentType.F_TARGET_REF);
        QName targetType = targetRefDef.getTargetTypeName();
        if (targetType != null && !Objects.equals(AssignmentHolderType.COMPLEX_TYPE, targetType)) {
            // target type was overriden
            return targetType;
        }


        return null;
    }

    private ObjectQuery normalizedSearchQuery() {
        var search = getSearchModel();
        var orig = search.getObject() == null ? null : search.getObject().createObjectQuery(getVariables(), getPageBase(), null);

        var targetType = determineTargetRefType();

        if (orig != null && targetType != null && SearchBoxModeType.BASIC.equals(search.getObject().getSearchMode()) ) {
            // We should optimize extract all dereferenced items from filter to form single exists(and) filter
            // which can be later extended to exists(type(and))) in order to provide
            // best information for repository

            // This code is written with assumption that all target filters have path targetRef/@
            var root = ObjectQueryUtil.simplify(orig.getFilter());
            List<ObjectFilter> filters;
            if (root instanceof AndFilter) {
                filters = new ArrayList<>(((AndFilter) root).getConditions());
            } else {
                filters = new ArrayList<>();
                filters.add(root);
            }
            var iter = filters.iterator();
            ArrayList<ObjectFilter> targetObjFilters = new ArrayList<>();
            while (iter.hasNext()) {
                var filter = iter.next();
                if (filter instanceof ValueFilter<?, ?>) {
                    if (((ValueFilter<?,?>) filter).canNestInsideExists(TARGET_REF_OBJ)) {
                        // Filter matches targetRef/@/
                        iter.remove();
                        targetObjFilters.add(((ValueFilter<?,?>) filter).nested(TARGET_REF_OBJ));
                    }
                }
            }
            if (!targetObjFilters.isEmpty()) {
                // We create replacement only if targetObjFilters were successfully created
                var typeAnd =getPrismContext().queryFactory().createAnd(targetObjFilters);
                orig = getPrismContext().queryFor(getType()).exists(TARGET_REF_OBJ)
                    .block()
                        .type(targetType).filter(typeAnd)
                    .endBlock().build();
                orig.addFilter(getPrismContext().queryFactory().createAndOptimized(filters));
            }

        }



        if (orig != null && orig.getFilter() instanceof FullTextFilter && SearchBoxModeType.FULLTEXT.equals(search.getObject().getSearchMode())) {
            // User entered full text filter, lets make sure it is invoked on targetRef
            orig = getPrismContext().queryFor(getType()).exists(TARGET_REF_OBJ)
                    .filter(orig.getFilter())
                    .build();
        }
        return orig;
    }

    private ObjectQuery mergeQueries(ObjectQuery origQuery, ObjectQuery query) {
        if (query != null) {
            if (origQuery == null) {
                return query;
            } else {
                origQuery.addFilter(query.getFilter());
            }
        }
        return origQuery;
    }

    @Override
    public void detach() {
        for (PrismContainerValueWrapper<AssignmentType> assignment : getAvailableData()) {
            AssignmentType assignmentType = assignment.getRealValue();
            if (assignmentType == null) {
                continue;
            }
            ObjectReferenceType ref = assignmentType.getTargetRef();
            if (ref == null) {
                continue;
            }
            if (ref.getObject() != null) {
                ref.asReferenceValue().setObject(null);
            }
        }
    }
}
