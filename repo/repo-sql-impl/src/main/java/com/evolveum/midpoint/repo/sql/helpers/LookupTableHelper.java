/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.*;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
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

/**
 * Contains methods specific to handle LookupTable rows.
 * (As these rows are stored outside main LookupTable object.)
 * <p>
 * It is quite a temporary solution in order to ease SqlRepositoryServiceImpl
 * from tons of type-specific code. Serious solution would be to implement
 * subobject-level operations more generically.
 */
@Component
public class LookupTableHelper {

    private static final Trace LOGGER = TraceManager.getTrace(LookupTableHelper.class);

    @Autowired private GeneralHelper generalHelper;
    @Autowired private PrismContext prismContext;

    public void addLookupTableRows(EntityManager em, RObject object, boolean deleteBeforeAdd) {
        if (!(object instanceof RLookupTable)) {
            return;
        }
        RLookupTable table = (RLookupTable) object;

        if (deleteBeforeAdd) {
            deleteLookupTableRows(em, table.getOid());
        }
        if (table.getRows() != null) {
            for (RLookupTableRow row : table.getRows()) {
                if (deleteBeforeAdd) {
                    row.setTransient(true);
                }
                em.merge(row);
            }
        }
    }

    private void addLookupTableRows(EntityManager em, String tableOid,
            Collection<PrismContainerValue<?>> values, int currentId, boolean deleteBeforeAdd)
            throws SchemaException {
        for (PrismContainerValue<?> value : values) {
            LookupTableRowType rowType = new LookupTableRowType();
            rowType.setupContainerValue(value);
            if (deleteBeforeAdd) {
                deleteRowByKey(em, tableOid, rowType.getKey());
            }
            RLookupTableRow row = RLookupTableRow.toRepo(tableOid, rowType);
            row.setId(currentId);
            currentId++;

            if (deleteBeforeAdd) {
                row.setTransient(true);
            }
            em.merge(row);
        }
    }

    /**
     * This method removes all lookup table rows for object defined by oid
     */
    public void deleteLookupTableRows(EntityManager em, String oid) {
        Query query = em.createNamedQuery("delete.lookupTableData");
        query.setParameter("oid", oid);

        query.executeUpdate();
    }

    public void updateLookupTableData(
            EntityManager em, String tableOid, Collection<? extends ItemDelta<?, ?>> modifications)
            throws SchemaException {
        if (modifications.isEmpty()) {
            return;
        }

        for (ItemDelta delta : modifications) {
            ItemPath deltaPath = delta.getPath();
            if (deltaPath.size() == 1) {  // whole row add/delete/replace
                if (!(delta instanceof ContainerDelta)) {
                    throw new IllegalStateException("Wrong table delta sneaked into updateLookupTableData: class=" + delta.getClass() + ", path=" + deltaPath);
                }
                // one "table" container modification
                ContainerDelta containerDelta = (ContainerDelta) delta;

                if (containerDelta.getValuesToDelete() != null) {
                    // todo do 'bulk' delete like delete from ... where oid=? and id in (...)
                    //noinspection unchecked
                    for (PrismContainerValue<LookupTableRowType> value :
                            (Collection<PrismContainerValue<LookupTableRowType>>) containerDelta.getValuesToDelete()) {
                        if (value.getId() != null) {
                            deleteRowById(em, tableOid, value.getId());
                        } else if (value.asContainerable().getKey() != null) {
                            deleteRowByKey(em, tableOid, value.asContainerable().getKey());
                        } else {
                            // ignore (or throw an exception?)
                        }
                    }
                }
                if (containerDelta.getValuesToAdd() != null) {
                    int currentId = generalHelper.findLastIdInRepo(em, tableOid, "get.lookupTableLastId") + 1;
                    addLookupTableRows(em, tableOid, containerDelta.getValuesToAdd(), currentId, true);
                }
                if (containerDelta.getValuesToReplace() != null) {
                    deleteLookupTableRows(em, tableOid);
                    addLookupTableRows(em, tableOid, containerDelta.getValuesToReplace(), 1, false);
                }
            } else if (deltaPath.size() == 3) {  // row segment modification (structure is already checked)
                Long rowId = ItemPath.toId(deltaPath.getSegment(1));
                QName name = ItemPath.toName(deltaPath.getSegment(2));

                RLookupTableRow row = em.find(RLookupTableRow.class, new RContainerId(RUtil.toInteger(rowId), tableOid));
                LookupTableRowType rowType = row.toJAXB(prismContext);
                delta.setParentPath(ItemPath.EMPTY_PATH);
                delta.applyTo(rowType.asPrismContainerValue());
                if (!QNameUtil.match(name, LookupTableRowType.F_LAST_CHANGE_TIMESTAMP)) {
                    rowType.setLastChangeTimestamp(null);   // in order to get filled in via toRepo call below
                }
                RLookupTableRow rowUpdated = RLookupTableRow.toRepo(tableOid, rowType);
                em.merge(rowUpdated);
            }
        }
    }

    private void deleteRowById(EntityManager session, String tableOid, Long id) {
        Query query = session.createNamedQuery("delete.lookupTableDataRow");
        query.setParameter("oid", tableOid);
        query.setParameter("id", RUtil.toInteger(id));
        query.executeUpdate();
    }

    private void deleteRowByKey(EntityManager session, String tableOid, String key) {
        Query query = session.createNamedQuery("delete.lookupTableDataRowByKey");
        query.setParameter("oid", tableOid);
        query.setParameter("key", key);
        query.executeUpdate();
    }

    public GetOperationOptions findLookupTableGetOption(Collection<SelectorOptions<GetOperationOptions>> options) {
        Collection<SelectorOptions<GetOperationOptions>> filtered = SelectorOptions.filterRetrieveOptions(options);
        for (SelectorOptions<GetOperationOptions> option : filtered) {
            ObjectSelector selector = option.getSelector();
            if (selector == null) {
                // Ignore this. These are top-level options. There will not
                // apply to lookup table
                continue;
            }
            if (LookupTableType.F_ROW.equivalent(selector.getPath())) {
                return option.getOptions();
            }
        }

        return null;
    }

    <T extends ObjectType> void updateLoadedLookupTable(PrismObject<T> object,
            Collection<SelectorOptions<GetOperationOptions>> options,
            EntityManager session) throws SchemaException {
        if (!SelectorOptions.hasToFetchPathNotRetrievedByDefault(LookupTableType.F_ROW, options)) {
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

        //noinspection unchecked
        List<RLookupTableRow> rows = query.getResultList();
        if (CollectionUtils.isNotEmpty(rows)) {
            LookupTableType lookup = (LookupTableType) object.asObjectable();
            List<LookupTableRowType> jaxbRows = lookup.getRow();
            for (RLookupTableRow row : rows) {
                LookupTableRowType jaxbRow = row.toJAXB(prismContext);
                jaxbRows.add(jaxbRow);
            }
            PrismContainer<LookupTableRowType> rowContainer = object.findContainer(LookupTableType.F_ROW);
            rowContainer.setIncomplete(false);
        } else {
            PrismContainer<LookupTableRowType> rowContainer = object.findContainer(LookupTableType.F_ROW);
            if (rowContainer != null) {
                rowContainer.clear();      // just in case
                rowContainer.setIncomplete(false);
            }
        }
    }

    private Query setupLookupTableRowsQuery(EntityManager em, RelationalValueSearchQuery queryDef, String oid) throws SchemaException {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> cq = cb.createQuery(RLookupTableRow.class);

        Root<RLookupTableRow> root = cq.from(RLookupTableRow.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(root.get("ownerOid"), oid));

        if (queryDef == null) {
            appendWhereClause(cq, where, cb);

            return em.createQuery(cq);
        }

        if (queryDef.getColumn() != null
                && queryDef.getSearchType() != null
                && StringUtils.isNotEmpty(queryDef.getSearchValue())) {

            Path<String> param;
            String value = queryDef.getSearchValue();
            if (LookupTableRowType.F_LABEL.equals(queryDef.getColumn())) {
                param = root.get("label").get("norm");
                value = prismContext.getDefaultPolyStringNormalizer().normalize(value);
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
            return em.createQuery(cq);
        }

        ItemPath orderByPath = paging.getPrimaryOrderingPath();
        OrderDirection direction = paging.getPrimaryOrderingDirection();
        if (direction != null && orderByPath != null && !orderByPath.isEmpty()) {
            if (orderByPath.size() > 1 || !orderByPath.startsWithName() && !orderByPath.startsWithIdentifier()) {
                throw new SchemaException("OrderBy has to consist of just one naming or identifier segment");
            }

            Object first = orderByPath.first();
            String orderBy = ItemPath.isName(first) ?
                    ItemPath.toName(first).getLocalPart() : RLookupTableRow.ID_COLUMN_NAME;

            Path[] orderByPaths = buildOrderByPaths(orderBy, root);
            Order[] order;
            switch (direction) {
                case ASCENDING:
                    order = Arrays.stream(orderByPaths).map(item -> cb.asc(item)).toArray(Order[]::new);
                    break;
                case DESCENDING:
                    order = Arrays.stream(orderByPaths).map(item -> cb.desc(item)).toArray(Order[]::new);
                    break;
                default:
                    throw new AssertionError(direction);
            }
            cq.orderBy(order);
        }

        return em.createQuery(cq);
    }

    private Path[] buildOrderByPaths(String orderBy, Root<RLookupTableRow> root) {
        if (!LookupTableRowType.F_LABEL.getLocalPart().equals(orderBy)) {
            return new Path[] { root.get(orderBy) };
        }

        Path label = root.get(orderBy);
        return new Path[] { label.get("norm"), label.get("orig") };
    }

    private void appendWhereClause(CriteriaQuery<?> cq, List<Predicate> where, CriteriaBuilder cb) {
        Predicate wherePredicate = where.get(0);
        if (where.size() > 1) {
            wherePredicate = cb.and(where.toArray(new Predicate[0]));
        }
        cq.where(wherePredicate);
    }

    public <T extends ObjectType> Collection<? extends ItemDelta<?, ?>> filterLookupTableModifications(
            Class<T> type, Collection<? extends ItemDelta<?, ?>> modifications) {
        Collection<ItemDelta<?, ?>> tableDelta = new ArrayList<>();
        if (!LookupTableType.class.equals(type)) {
            return tableDelta;
        }
        for (ItemDelta<?, ?> delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Lookup table cannot be modified via empty-path modification");
            } else if (path.equivalent(LookupTableType.F_ROW)) {
                tableDelta.add(delta);
            } else if (path.isSuperPath(LookupTableType.F_ROW)) {
                // should be row[id]/xxx where xxx=key|value|label?
                if (path.size() != 3
                        || !ItemPath.isId(path.getSegment(1))
                        || !ItemPath.isName(path.getSegment(2))) {
                    throw new UnsupportedOperationException("Unsupported modification path for lookup tables: " + path);
                }
                tableDelta.add(delta);
            }
        }

        //noinspection SuspiciousMethodCalls
        modifications.removeAll(tableDelta);

        return tableDelta;
    }
}
