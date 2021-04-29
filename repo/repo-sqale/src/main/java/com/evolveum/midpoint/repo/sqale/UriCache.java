/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MUri;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Component hiding details of how QNames are stored in {@link QUri}.
 * Following prefixes are used for its methods:
 *
 * * `get` returns URI or ID for ID or URI, may return null, no DB access;
 * * `search` like `get` but returns {@link #UNKNOWN_ID} instead of null, used for query predicates,
 * no DB access by the URI cache itself;
 * * `resolve` returns URI/ID for ID/URI or throws exception if not found, this is for situations
 * where the entry for URI is expected to exist already, still no DB access required;
 * * finally, {@link #processCacheableUri(String)} is the only operation that accesses
 * the database if the URI is not found in the cache in order to write it there.
 *
 * [NOTE]
 * URI is added in the database in its own separate transaction.
 * It is tempting to add cached URI as part of the existing transaction, but when the provided
 * transaction is rolled back, the cache still thinks the URI row is already there.
 * This could be avoided if the runtime maps were updated *only* after the row was successfully
 * read from the DB in other operations - which beats the purposes of those fast operations.
 * Instead we risk adding row that is never used as it is no harm; it will likely be used later.
 * While there is a potential "attack" on the URI cache, it is good enough for now.
 * Later perhaps only part of the `m_uri` table will be cached in the runtime and then it may be
 * possible.
 */
public class UriCache {

    private static final Trace LOGGER = TraceManager.getTrace(UriCache.class);

    /**
     * Unknown id placeholder, not actually in DB but returned when URI is not in the cache
     * and returning `null` or throwing exception would not make sense.
     * Typical case is using it for query predicate when searching for unknown URI should result
     * in a condition comparing URI ID attribute (e.g. relation_id) to id that will not be found.
     * This is completely transient and can be changed if the need arises.
     */
    public static final int UNKNOWN_ID = -1;

    private final Map<Integer, String> idToUri = new ConcurrentHashMap<>();
    private final Map<String, Integer> uriToId = new ConcurrentHashMap<>();

    private Supplier<JdbcSession> jdbcSessionSupplier;

    /**
     * Initializes the URI cache.
     * Provided {@link JdbcSession} supplier will be used for later writes as well.
     */
    public synchronized void initialize(Supplier<JdbcSession> jdbcSessionSupplier) {
        this.jdbcSessionSupplier = jdbcSessionSupplier;

        // this can be called repeatedly in tests, so the clear may be necessary
        idToUri.clear();
        uriToId.clear();

        QUri uri = QUri.DEFAULT;
        List<MUri> result;
        try (JdbcSession jdbcSession = jdbcSessionSupplier.get().startReadOnlyTransaction()) {
            result = jdbcSession.newQuery()
                    .select(uri)
                    .from(uri)
                    .fetch();
            jdbcSession.commit();
        }

        for (MUri row : result) {
            updateMaps(row);
        }
        LOGGER.info("URI cache initialized with {} items.", result.size());
    }

    private void updateMaps(MUri row) {
        if (row.id == UNKNOWN_ID) {
            throw new AssertionError("URI with ID=" + UNKNOWN_ID + " MUST NOT be in the database!");
        }

        idToUri.put(row.id, row.uri);
        uriToId.put(row.uri, row.id);
    }

    /** Returns ID for QName, possibly {@code null} - does not work with underlying database. */
    public @Nullable Integer getId(@NotNull QName qName) {
        return getId(QNameUtil.qNameToUri(qName));
    }

    /** Returns ID for QName, possibly {@link #UNKNOWN_ID} - does not work with underlying database. */
    public @NotNull Integer searchId(@NotNull QName qName) {
        return searchId(QNameUtil.qNameToUri(qName));
    }

    /** Returns ID for QName or throws exception - does not work with underlying database. */
    public @NotNull Integer resolveUriToId(@NotNull QName qName) {
        return resolveUriToId(QNameUtil.qNameToUri(qName));
    }

    /** Returns ID for string, possibly {@code null} - does not work with underlying database. */
    public @Nullable Integer getId(@NotNull String uri) {
        Integer id = uriToId.get(uri);
        LOGGER.trace("URI cache 'get' returned ID={} for URI={}", id, uri);
        return id;
    }

    /** Returns ID for string or {@link #UNKNOWN_ID} - does not work with underlying database. */
    public @NotNull Integer searchId(@NotNull String uri) {
        Integer id = uriToId.getOrDefault(uri, UNKNOWN_ID);
        LOGGER.trace("URI cache 'search' returned ID={} for URI={}", id, uri);
        return id;
    }

    /** Returns ID for QName or throws exception - does not work with underlying database. */
    public @NotNull Integer resolveUriToId(@NotNull String uri) {
        Integer id = getId(uri);
        LOGGER.trace("URI cache 'resolve' returned ID={} for URI={}", id, uri);
        return Objects.requireNonNull(id, () -> "URI not cached: " + uri);
    }

    /** Returns URI string for ID or {@code null} - does not work with underlying database. */
    public String getUri(Integer id) {
        String uri = idToUri.get(id);
        LOGGER.trace("URI cache 'get' returned URI={} for ID={}", uri, id);
        return uri;
    }

    /** Returns URI string for ID or throws exception - does not work with underlying database. */
    public @NotNull String resolveToUri(Integer id) {
        String uri = idToUri.get(id);
        LOGGER.trace("URI cache 'resolve' returned URI={} for ID={}", uri, id);
        return Objects.requireNonNull(uri, () -> "No URI cached under ID " + id);
    }

    /**
     * Returns ID for URI creating new cache row in DB as needed.
     * Returns null for null URI parameter.
     */
    public synchronized @Nullable Integer processCacheableUri(@Nullable String uri) {
        if (uri == null) {
            return null;
        }
        if (jdbcSessionSupplier == null) {
            throw new IllegalStateException("URI cache was not initialized yet!");
        }

        Integer id = getId(uri);
        if (id != null) {
            return id;
        }

        QUri qu = QUri.DEFAULT;
        try (JdbcSession jdbcSession = jdbcSessionSupplier.get().startTransaction()) {
            id = jdbcSession.newInsert(qu)
                    .set(qu.uri, uri)
                    .executeWithKey(qu.id);
            updateMaps(MUri.of(id, uri));
            jdbcSession.commit();
        }

        LOGGER.debug("URI cache inserted URI={} under ID={}", uri, id);
        return id;
    }
}
