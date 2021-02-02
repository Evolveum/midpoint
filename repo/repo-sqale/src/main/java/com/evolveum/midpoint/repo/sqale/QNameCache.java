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

import com.evolveum.midpoint.repo.sqale.qmodel.common.MQName;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QQName;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Component hiding details of how QNames are stored in {@link QQName}.
 */
public class QNameCache {

    private final Map<Integer, String> idToUri = new HashMap<>();
    private final Map<String, Integer> uriToId = new HashMap<>();

    public void initialize(JdbcSession jdbcSession) {
        final QQName qn = new QQName("qn");
        final List<MQName> result = jdbcSession.newQuery()
                .select(qn)
                .from(qn)
                .fetch();

        for (MQName row : result) {
            idToUri.put(row.id, row.uri);
            uriToId.put(row.uri, row.id);
        }
    }

    /** Returns ID for QName, possibly {@code null} - does not work with underlying database. */
    public Integer getId(QName qName) {
        return uriToId.get(QNameUtil.qNameToUri(qName));
    }

    /** Returns ID for QName or throws exception - does not work with underlying database. */
    public Integer getIdMandatory(QName qName) {
        return Objects.requireNonNull(
                uriToId.get(QNameUtil.qNameToUri(qName)),
                () -> "QName not cached: " + qName);
    }

    /** Returns QName for ID, possibly {@code null} - does not work with underlying database. */
    public QName getQName(Integer id) {
        return QNameUtil.uriToQName(idToUri.get(id));
    }

    /** Returns QName for ID or throws exception - does not work with underlying database. */
    public QName getQNameMandatory(Integer id) {
        return Objects.requireNonNull(
                QNameUtil.uriToQName(idToUri.get(id)),
                () -> "No QName cached under ID " + id);
    }
}
