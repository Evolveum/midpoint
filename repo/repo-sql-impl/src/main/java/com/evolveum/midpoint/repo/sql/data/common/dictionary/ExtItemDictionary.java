/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.dictionary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.Session;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.RestartOperationRequestedException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ExtItemDictionary {

    private static final Trace LOGGER = TraceManager.getTrace(ExtItemDictionary.class);

    @Autowired private SqlRepositoryServiceImpl repositoryService;
    @Autowired private BaseHelper baseHelper;

    private Map<Integer, RExtItem> itemsById;
    private Map<RExtItem.Key, RExtItem> itemsByKey;

    @PostConstruct
    public synchronized void initialize() {
        itemsByKey = null;
        itemsById = null;
    }

    private boolean fetchItemsIfNeeded() {
        if (itemsByKey != null) {
            return false;
        } else {
            fetchItems();
            return true;
        }
    }

    private void fetchItems() {
        executeAttempts(RepositoryService.OP_FETCH_EXT_ITEMS, RExtItem.class, "fetch ext items", () -> fetchItemsAttempt());
    }

    private void fetchItemsAttempt() {
        Session session = null;
        try {
            session = baseHelper.beginReadOnlyTransaction();

            JpaCriteriaQuery<RExtItem> query = session.getCriteriaBuilder().createQuery(RExtItem.class);
            query.select(query.from(RExtItem.class));
            List<RExtItem> items = session.createQuery(query).getResultList();
            LOGGER.debug("Fetched {} item definitions", items.size());

            itemsById = new ConcurrentHashMap<>(items.size());
            itemsByKey = new ConcurrentHashMap<>(items.size());

            for (RExtItem item : items) {
                itemsById.put(item.getId(), item);
                itemsByKey.put(item.toKey(), item);
            }

            session.getTransaction().commit();
        } catch (RuntimeException ex) {
            LOGGER.debug("Exception fetch: {}", ex.getMessage());
            baseHelper.handleGeneralException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }
    }

    @NotNull
    public synchronized RExtItem createOrFindItemDefinition(@NotNull ItemDefinition<?> definition, boolean throwExceptionAfterCreate) {
        return createOrFindItemByDefinitionInternal(definition, true, throwExceptionAfterCreate);
    }

    @NotNull
    public synchronized RExtItem createOrFindItemDefinition(@NotNull ItemDefinition<?> definition) {
        return createOrFindItemByDefinitionInternal(definition, true, true);
    }

    @Nullable
    public synchronized RExtItem findItemByDefinition(@NotNull ItemDefinition<?> definition) {
        return createOrFindItemByDefinitionInternal(definition, false, true);
    }

    @Contract("_, _, true -> !null")
    private RExtItem createOrFindItemByDefinitionInternal(
            @NotNull ItemDefinition<?> definition, boolean create, boolean throwExceptionAfterCreate) {

        boolean fetchedNow = fetchItemsIfNeeded();
        RExtItem.Key key = RExtItem.createKeyFromDefinition(definition);

        RExtItem item = itemsByKey.get(key);

        if (item == null && !fetchedNow) {
            LOGGER.debug("Ext item for {} not found, fetching all items.", key);
            fetchItems();
            item = itemsByKey.get(key);
        }
        if (item == null && create) {
            LOGGER.debug("Ext item for {} not found even in current items; creating it.", key);

            item = RExtItem.createFromDefinition(definition);

            addExtItemAttempt(item);

            if (throwExceptionAfterCreate) {
                throw new RestartOperationRequestedException("Restarting parent operation because an extension item was created");
            }
        }

        return item;
    }

    private void addExtItemAttempt(RExtItem item) {
        Session session = null;
        try {
            session = baseHelper.beginTransaction();
            session.persist(item);
            session.getTransaction().commit();
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }
    }

    private void executeAttempts(String operationName, Class<?> type, String operationVerb, Runnable runnable) {
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operationName, type);
        int attempt = 1;
        try {
            while (true) {
                try {
                    runnable.run();
                    break;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operationVerb, attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    public synchronized RExtItem getItemById(Integer extItemId) {
        boolean fresh = fetchItemsIfNeeded();
        RExtItem extItem = itemsById.get(extItemId);
        if (extItem != null || fresh) {
            return extItem;
        }
        fetchItems();
        return itemsById.get(extItemId);
    }
}
