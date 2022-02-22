/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.outcomeStrategies;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

public class ResponsesSummary {

    private int[] counts = new int[AccessCertificationResponseType.values().length];
    private boolean empty = true;

    public void add(AccessCertificationResponseType response) {
        counts[response.ordinal()]++;
        empty = false;
    }

    public int get(AccessCertificationResponseType response) {
        return counts[response.ordinal()];
    }

    public boolean has(AccessCertificationResponseType response) {
        return get(response) > 0;
    }

    public boolean isEmpty() {
        return empty;
    }
}
