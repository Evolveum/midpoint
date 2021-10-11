/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.casemgmt.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * An interface through which external observers can be notified about case-related events.
 * Used e.g. for implementing case-related notifications.
 *
 * EXPERIMENTAL. This interface may change in near future.
 */
public interface CaseEventListener {

    /**
     * This method is called when a case is created.
     */
    void onCaseCreation(CaseType aCase, OperationResult result);
}
