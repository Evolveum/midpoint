/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.casemgmt.api;

public interface CaseEventDispatcherAware {

    void setDispatcher(CaseEventDispatcher dispatcher);

    CaseEventDispatcher getDispatcher();

}
