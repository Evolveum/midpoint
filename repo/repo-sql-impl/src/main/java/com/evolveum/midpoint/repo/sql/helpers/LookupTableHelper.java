/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.AlphanumericPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.repo.sql.data.common.RLookupTable;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.data.common.other.RLookupTableRow;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Contains methods specific to handle LookupTable rows.
 * (As these rows are stored outside main LookupTable object.)
 *
 * It is quite a temporary solution in order to ease SqlRepositoryServiceImpl
 * from tons of type-specific code. Serious solution would be to implement
 * subobject-level operations more generically.
 *
 * @author lazyman, mederly
 */
@Component
public class LookupTableHelper {

    private static final Trace LOGGER = TraceManager.getTrace(LookupTableHelper.class);

    @Autowired
    private GeneralHelper generalHelper;

    public void addLookupTableRows(Session session, RObject object, boolean deleteBeforeAdd) {
        if (!(object instanceof RLookupTable)) {
            return;
        }
        RLookupTable table = (RLookupTable) object;

        if (deleteBeforeAdd) {
            deleteLookupTableRows(session, table.getOid());
        }
        if (table.getRows() != null) {
            for (RLookupTableRow row : table.getRows()) {
                session.save(row);
            }
        }
    }

    private void addLookupTableRows(Session session, String tableOid, Collection<PrismContainerValue> values, int currentId, boolean deleteBeforeAdd) throws SchemaException {
        for (PrismContainerValue value : values) {
            LookupTableRowType rowType = new LookupTableRowType();
            rowType.setupContainerValue(value);
            if (deleteBeforeAdd) {
                deleteRowByKey(session, tableOid, rowType.getKey());
            }
            RLookupTableRow row = RLookupTableRow.toRepo(tableOid, rowType);
            row.setId(currentId);
            currentId++;
            session.save(row);
        }
    }

    /**
     * This method removes all lookup table rows for object defined by oid
     * @param session
     * @param oid
     */
    public void deleteLookupTableRows(Session session, String oid) {
        Query query = session.getNamedQuery("delete.lookupTableData");
        query.setParameter("oid", oid);

        query.executeUpdate();
    }

    public void updateLookupTableData(Session session, String tableOid, Collection<? extends ItemDelta> modifications)
            throws SchemaException {
        if (modifications.isEmpty()) {
            return;
        }

        for (ItemDelta delta : modifications) {
            if (delta.getPath().size() == 1) {  // whole row add/delete/replace
                if (!(delta instanceof ContainerDelta)) {
                    throw new IllegalStateException("Wrong table delta sneaked into updateLookupTableData: class=" + delta.getClass() + ", path=" + delta.getPath());
                }
                // one "table" container modification
                ContainerDelta containerDelta = (ContainerDelta) delta;

                if (containerDelta.getValuesToDelete() != null) {
                    // todo do 'bulk' delete like delete from ... where oid=? and id in (...)
                    for (PrismContainerValue<LookupTableRowType> value : (Collection<PrismContainerValue>) containerDelta.getValuesToDelete()) {
                        if (value.getId() != null) {
                            deleteRowById(session, tableOid, value.getId());
                        } else if (value.asContainerable().getKey() != null) {
                            deleteRowByKey(session, tableOid, value.asContainerable().getKey());
                        } else {
                            // ignore (or throw an exception?)
                        }
                    }
                }
                if (containerDelta.getValuesToAdd() != null) {
                    int currentId = generalHelper.findLastIdInRepo(session, tableOid, "get.lookupTableLastId") + 1;
                    addLookupTableRows(session, tableOid, containerDelta.getValuesToAdd(), currentId, true);
                }
                if (containerDelta.getValuesToReplace() != null) {
                    deleteLookupTableRows(session, tableOid);
                    addLookupTableRows(session, tableOid, containerDelta.getValuesToReplace(), 1, false);
                }
            } else if (delta.getPath().size() == 3) {  // row segment modification (structure is already checked)
                List<ItemPathSegment> segments = delta.getPath().getSegments();
                Long rowId = ((IdItemPathSegment) segments.get(1)).getId();
                QName name = ((NameItemPathSegment) segments.get(2)).getName();

                RLookupTableRow row = (RLookupTableRow) session.get(RLookupTableRow.class, new RContainerId(RUtil.toInteger(rowId), tableOid));
                LookupTableRowType rowType = row.toJAXB();
                delta.setParentPath(ItemPath.EMPTY_PATH);
                delta.applyTo(rowType.asPrismContainerValue());
                if (!QNameUtil.match(name, LookupTableRowType.F_LAST_CHANGE_TIMESTAMP)) {
                    rowType.setLastChangeTimestamp(null);   // in order to get filled in via toRepo call below
                }
                RLookupTableRow rowUpdated = RLookupTableRow.toRepo(tableOid, rowType);
                session.merge(rowUpdated);
            }
        }
    }

    private void deleteRowById(Session session, String tableOid, Long id) {
        Query query = session.getNamedQuery("delete.lookupTableDataRow");
        query.setParameter("oid", tableOid);
        query.setParameter("id", RUtil.toInteger(id));
        query.executeUpdate();
    }

    private void deleteRowByKey(Session session, String tableOid, String key) {
        Query query = session.getNamedQuery("delete.lookupTableDataRowByKey");
        query.setParameter("oid", tableOid);
        query.setParameter("key", key);
        query.executeUpdate();
    }

    public GetOperationOptions findLookupTableGetOption(Collection<SelectorOptions<GetOperationOptions>> options) {
        final ItemPath tablePath = new ItemPath(LookupTableType.F_ROW);

        Collection<SelectorOptions<GetOperationOptions>> filtered = SelectorOptions.filterRetrieveOptions(options);
        for (SelectorOptions<GetOperationOptions> option : filtered) {
            ObjectSelector selector = option.getSelector();
            if (selector == null) {
            	// Ignore this. These are top-level options. There will not
            	// apply to lookup table
            	continue;
            }
            ItemPath selected = selector.getPath();

            if (tablePath.equivalent(selected)) {
                return option.getOptions();
            }
        }

        return null;
    }

    public <T extends ObjectType> void updateLoadedLookupTable(PrismObject<T> object,
                                                               Collection<SelectorOptions<GetOperationOptions>> options,
                                                               Session session) throws SchemaException {
        if (!SelectorOptions.hasToLoadPath(LookupTableType.F_ROW, options)) {
            return;
        }

        LOGGER.debug("Loading lookup table data.");

        GetOperationOptions getOption = findLookupTableGetOption(options);
        RelationalValueSearchQuery queryDef = getOption == null ? null : getOption.getRelationalValueSearchQuery();
        Query query = setupLookupTableRowsQuery(session, queryDef, object.getOid());
        if (queryDef != null && queryDef.getPaging() != null) {
            ObjectPaging paging = queryDef.getPaging();

            if (paging.getOffset() != null) {
                query.setFirstResult(paging.getOffset());
            }
            if (paging.getMaxSize() != null) {
                query.setMaxResults(paging.getMaxSize());
            }
        }

        List<RLookupTableRow> rows = query.list();
        if (rows == null || rows.isEmpty()) {
            return;
        }

        LookupTableType lookup = (LookupTableType) object.asObjectable();
        List<LookupTableRowType> jaxbRows = lookup.getRow();
        for (RLookupTableRow row : rows) {
            LookupTableRowType jaxbRow = row.toJAXB();
            jaxbRows.add(jaxbRow);
        }
    }

    private Query setupLookupTableRowsQuery(Session session, RelationalValueSearchQuery queryDef, String oid) throws SchemaException {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery(RLookupTableRow.class);

        Root<RLookupTableRow> root = cq.from(RLookupTableRow.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(root.get("ownerOid"), oid));

        if (queryDef == null) {
            appendWhereClause(cq, where, cb);

            return session.createQuery(cq);
        }

        if (queryDef.getColumn() != null
                && queryDef.getSearchType() != null
                && StringUtils.isNotEmpty(queryDef.getSearchValue())) {

            Path param;
            String value = queryDef.getSearchValue();
            if (LookupTableRowType.F_LABEL.equals(queryDef.getColumn())) {
                param = root.get("label").get("norm");

                PolyString poly = new PolyString(value);
                poly.recompute(new AlphanumericPolyStringNormalizer());
                value = poly.getNorm();
            } else {
                param = root.get(queryDef.getColumn().getLocalPart());
            }
            switch (queryDef.getSearchType()) {
                case EXACT:
                    where.add(cb.equal(param, value));
                    break;
                case STARTS_WITH:
                    where.add(cb.like(param, value + "%"));
                    break;
                case SUBSTRING:
                    where.add(cb.like(param, "%" + value + "%"));
            }
        }

        appendWhereClause(cq, where, cb);

        ObjectPaging paging = queryDef.getPaging();
        if (paging == null) {
            return session.createQuery(cq);
        }

        ItemPath orderByPath = paging.getOrderBy();
        if (paging.getDirection() != null && orderByPath != null && !orderByPath.isEmpty()) {
            if (orderByPath.size() > 1 ||
                    !(orderByPath.first() instanceof NameItemPathSegment) && !(orderByPath.first() instanceof IdentifierPathSegment)) {
                throw new SchemaException("OrderBy has to consist of just one naming or identifier segment");
            }

            ItemPathSegment first = orderByPath.first();
            String orderBy = first instanceof NameItemPathSegment ?
                    ((NameItemPathSegment) first).getName().getLocalPart() : RLookupTableRow.ID_COLUMN_NAME;

            Path[] orderByPaths = buildOrderByPaths(orderBy, root);
            Order[] order = null;
            switch (paging.getDirection()) {
                case ASCENDING:
                    order = Arrays.stream(orderByPaths).map(item -> cb.asc(item)).toArray(Order[]::new);
                    break;
                case DESCENDING:
                    order = Arrays.stream(orderByPaths).map(item -> cb.desc(item)).toArray(Order[]::new);
                    break;
            }

            if (order != null) {
                cq.orderBy(order);
            }
        }

        return session.createQuery(cq);
    }

    private Path[] buildOrderByPaths(String orderBy, Root<RLookupTableRow> root) {
        if (!LookupTableRowType.F_LABEL.getLocalPart().equals(orderBy)) {
            return new Path[]{root.get(orderBy)};
        }

        Path label = root.get(orderBy);
        return new Path[]{label.get("norm"), label.get("orig")};
    }

    private void appendWhereClause(CriteriaQuery cq, List<Predicate> where, CriteriaBuilder cb) {
        Predicate wherePredicate = where.get(0);
        if (where.size() > 1) {
            wherePredicate = cb.and(where.toArray(new Predicate[where.size()]));
        }
        cq.where(wherePredicate);
    }

    public <T extends ObjectType> Collection<? extends ItemDelta> filterLookupTableModifications(Class<T> type,
                                                                                                 Collection<? extends ItemDelta> modifications) {
        Collection<ItemDelta> tableDelta = new ArrayList<>();
        if (!LookupTableType.class.equals(type)) {
            return tableDelta;
        }

        ItemPath rowPath = new ItemPath(LookupTableType.F_ROW);
        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Lookup table cannot be modified via empty-path modification");
            } else if (path.equivalent(rowPath)) {
                tableDelta.add(delta);
            } else if (path.isSuperPath(rowPath)) {
                // should be row[id]/xxx where xxx=key|value|label?
                List<ItemPathSegment> segments = path.getSegments();
                if (segments.size() != 3
                        || !(segments.get(1) instanceof IdItemPathSegment)
                        || !(segments.get(2) instanceof NameItemPathSegment)) {
                    throw new UnsupportedOperationException("Unsupported modification path for lookup tables: " + path);
                }
                tableDelta.add(delta);
            }
        }

        modifications.removeAll(tableDelta);

        return tableDelta;
    }
}
