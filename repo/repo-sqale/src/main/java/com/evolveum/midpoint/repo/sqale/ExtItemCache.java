/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.QExtItem;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO
 */
public class ExtItemCache {

    private static final Trace LOGGER = TraceManager.getTrace(ExtItemCache.class);

    private final Map<Integer, MExtItem> idToExtItem = new ConcurrentHashMap<>();
    private final Map<MExtItem.Key, MExtItem> keyToExtItem = new ConcurrentHashMap<>();

    private Supplier<JdbcSession> jdbcSessionSupplier;

    /**
     * Initializes the ext-item cache.
     * Provided {@link JdbcSession} supplier will be used for later writes as well.
     */
    public synchronized void initialize(Supplier<JdbcSession> jdbcSessionSupplier) {
        this.jdbcSessionSupplier = jdbcSessionSupplier;

        // this can be called repeatedly in tests, so the clear may be necessary
        idToExtItem.clear();
        keyToExtItem.clear();

        QExtItem uri = QExtItem.DEFAULT;
        List<MExtItem> result;
        try (JdbcSession jdbcSession = jdbcSessionSupplier.get().startReadOnlyTransaction()) {
            result = jdbcSession.newQuery()
                    .select(uri)
                    .from(uri)
                    .fetch();
            jdbcSession.commit();
        }

        for (MExtItem row : result) {
            updateMaps(row);
        }
        LOGGER.info("Ext item cache initialized with {} items.", result.size());
    }

    private void updateMaps(MExtItem row) {
        idToExtItem.put(row.id, row);
        keyToExtItem.put(row.key(), row);
    }

/*
    public @Nullable Integer getId(@NotNull QName qName) {
        return getId(QNameUtil.qNameToUri(qName));
    }

    public @NotNull Integer searchId(@NotNull QName qName) {
        return searchId(QNameUtil.qNameToUri(qName));
    }

    public @NotNull Integer resolveUriToId(@NotNull QName qName) {
        return resolveUriToId(QNameUtil.qNameToUri(qName));
    }

    public @Nullable Integer getId(@NotNull String uri) {
        Integer id = extItemKeyToId.get(uri);
        LOGGER.trace("URI cache 'get' returned ID={} for URI={}", id, uri);
        return id;
    }

    public @NotNull Integer searchId(@NotNull String uri) {
        Integer id = extItemKeyToId.getOrDefault(uri, UNKNOWN_ID);
        LOGGER.trace("URI cache 'search' returned ID={} for URI={}", id, uri);
        return id;
    }

    public @NotNull Integer resolveUriToId(@NotNull String uri) {
        Integer id = getId(uri);
        LOGGER.trace("URI cache 'resolve' returned ID={} for URI={}", id, uri);
        return Objects.requireNonNull(id, () -> "URI not cached: " + uri);
    }

    public String getUri(Integer id) {
        String uri = idToExtItem.get(id);
        LOGGER.trace("URI cache 'get' returned URI={} for ID={}", uri, id);
        return uri;
    }

    public @NotNull String resolveToUri(Integer id) {
        String uri = idToExtItem.get(id);
        LOGGER.trace("URI cache 'resolve' returned URI={} for ID={}", uri, id);
        return Objects.requireNonNull(uri, () -> "No URI cached under ID " + id);
    }
*/

    public synchronized @NotNull MExtItem resolveExtensionItem(@NotNull MExtItem.Key extItemKey) {
        if (jdbcSessionSupplier == null) {
            throw new IllegalStateException("Ext item cache was not initialized yet!");
        }

        MExtItem extItem = keyToExtItem.get(extItemKey);
        if (extItem != null) {
            return extItem;
        }

        QExtItem ei = QExtItem.DEFAULT;
        try (JdbcSession jdbcSession = jdbcSessionSupplier.get().startTransaction()) {
            Integer id = jdbcSession.newInsert(ei)
                    .set(ei.itemName, extItemKey.itemName)
                    .set(ei.valueType, extItemKey.valueType)
                    .set(ei.holderType, extItemKey.holderType)
                    .set(ei.cardinality, extItemKey.cardinality)
                    .executeWithKey(ei.id);
            jdbcSession.commit();

            extItem = MExtItem.of(id, extItemKey);
            updateMaps(extItem);
        }

        LOGGER.debug("Ext item cache row inserted: {}", extItem);
        return extItem;
    }
}
