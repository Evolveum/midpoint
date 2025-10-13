/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

/**
 * Querydsl "row bean" type related to {@link QConnectorHost}.
 */
public class MConnectorHost extends MObject {

    public String hostname;
    public String port;
}
