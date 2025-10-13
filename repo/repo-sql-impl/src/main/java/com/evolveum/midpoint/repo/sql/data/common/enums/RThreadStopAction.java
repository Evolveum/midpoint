/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;

/**
 * @author lazyman
 */
@JaxbType(type = ThreadStopActionType.class)
public enum RThreadStopAction implements SchemaEnum<ThreadStopActionType> {

    RESTART(ThreadStopActionType.RESTART),
    RESCHEDULE(ThreadStopActionType.RESCHEDULE),
    SUSPEND(ThreadStopActionType.SUSPEND),
    CLOSE(ThreadStopActionType.CLOSE);

    private ThreadStopActionType action;

    RThreadStopAction(ThreadStopActionType action) {
        this.action = action;
        RUtil.register(this);
    }

    @Override
    public ThreadStopActionType getSchemaValue() {
        return action;
    }
}
