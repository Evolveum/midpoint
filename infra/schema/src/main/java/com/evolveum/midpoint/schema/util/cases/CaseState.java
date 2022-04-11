/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class CaseState {

    @NotNull private final QName stateQName;

    private CaseState(@Nullable String stateUri) {
        this.stateQName = stateUri != null ?
                QNameUtil.uriToQName(stateUri, true) :
                SchemaConstants.CASE_STATE_CREATED_QNAME;
    }

    public static CaseState of(@NotNull CaseType aCase) {
        return of(aCase.getState());
    }

    public static CaseState of(@Nullable String stateUri) {
        return new CaseState(stateUri);
    }

    public boolean isCreated() {
        return QNameUtil.match(SchemaConstants.CASE_STATE_CREATED_QNAME, stateQName);
    }

    public boolean isOpen() {
        return QNameUtil.match(SchemaConstants.CASE_STATE_OPEN_QNAME, stateQName);
    }

    public boolean isClosing() {
        return QNameUtil.match(SchemaConstants.CASE_STATE_CLOSING_QNAME, stateQName);
    }

    public boolean isClosed() {
        return QNameUtil.match(SchemaConstants.CASE_STATE_CLOSED_QNAME, stateQName);
    }

    @Override
    public String toString() {
        return stateQName.getLocalPart();
    }
}
