/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

/**
 * Querydsl "row bean" type related to {@link QQName}.
 */
public class MQName {

    public Integer id;
    public String uri;

    @Override
    public String toString() {
        return "MQName{" +
                "id=" + id +
                ", uri='" + uri + '\'' +
                '}';
    }
}
