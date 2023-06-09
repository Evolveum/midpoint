/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.api;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public record AccessCertificationCaseId(@NotNull String campaignOid, long caseId) implements Serializable {

    @Override
    public String toString() {
        return campaignOid + ":" + caseId;
    }
}
