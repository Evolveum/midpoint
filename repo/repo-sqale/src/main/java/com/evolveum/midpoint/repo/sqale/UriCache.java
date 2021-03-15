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
 * * finally, {@link #processCachedUri(String, JdbcSession)} is the only operation that accesses
 * the database if the URI is not found in the cache in order to write it there.
 */
public class UriCache {

    private static final Trace LOGGER = TraceManager.getTrace(UriCache.class);

    /**
     * Unknown id placeholder, not actually in DB but returned when URI is not in the cache
     * and returning `null` or throwing exception would not make sense.
     * Typical case is using it for query predicate when searching for unknown URI should result
     * in a condition comparing URI ID attribute (e.g. relation_id) to id that will not be found.
     */
    public static final int UNKNOWN_ID = -1;

    private final Map<Integer, String> idToUri = new ConcurrentHashMap<>();
    private final Map<String, Integer> uriToId = new ConcurrentHashMap<>();

    public synchronized void initialize(JdbcSession jdbcSession) {
        QUri uri = QUri.DEFAULT;
        List<MUri> result = jdbcSession.newQuery()
                .select(uri)
                .from(uri)
                .fetch();

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
    public synchronized @Nullable Integer processCachedUri(
            @Nullable String uri, JdbcSession jdbcSession) {
        if (uri == null) {
            return null;
        }

        Integer id = getId(uri);
        if (id != null) {
            return id;
        }

        QUri qu = QUri.DEFAULT;
        id = jdbcSession.newInsert(qu)
                .set(qu.uri, uri)
                .executeWithKey(qu.id);
        updateMaps(MUri.of(id, uri));

        LOGGER.debug("URI cache inserted URI={} under ID={}", uri, id);
        return id;
    }
}
