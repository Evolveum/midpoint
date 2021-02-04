/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MUri;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Component hiding details of how QNames are stored in {@link QUri}.
 */
public class UriCache {

    private final Map<Integer, String> idToUri = new HashMap<>();
    private final Map<String, Integer> uriToId = new HashMap<>();

    public synchronized void initialize(JdbcSession jdbcSession) {
        QUri uri = QUri.DEFAULT;
        List<MUri> result = jdbcSession.newQuery()
                .select(uri)
                .from(uri)
                .fetch();

        for (MUri row : result) {
            updateMaps(row);
        }
    }

    private void updateMaps(MUri row) {
        idToUri.put(row.id, row.uri);
        uriToId.put(row.uri, row.id);
    }

    /** Returns ID for QName, possibly {@code null} - does not work with underlying database. */
    public @Nullable Integer getId(@NotNull QName qName) {
        return getId(QNameUtil.qNameToUri(qName));
    }

    /** Returns ID for QName or throws exception - does not work with underlying database. */
    public @NotNull Integer getIdMandatory(@NotNull QName qName) {
        return getIdMandatory(QNameUtil.qNameToUri(qName));
    }

    /** Returns ID for string, possibly {@code null} - does not work with underlying database. */
    public @Nullable Integer getId(@NotNull String uri) {
        return uriToId.get(uri);
    }

    /** Returns ID for QName or throws exception - does not work with underlying database. */
    public @NotNull Integer getIdMandatory(@NotNull String uri) {
        return Objects.requireNonNull(getId(uri), () -> "URI not cached: " + uri);
    }

    /** Returns URI string for ID or {@code null} - does not work with underlying database. */
    public String getQName(Integer id) {
        return idToUri.get(id);
    }

    /** Returns URI string for ID or throws exception - does not work with underlying database. */
    public @NotNull String getQNameMandatory(Integer id) {
        return Objects.requireNonNull(idToUri.get(id), () -> "No URI cached under ID " + id);
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

        return id;
    }
}
