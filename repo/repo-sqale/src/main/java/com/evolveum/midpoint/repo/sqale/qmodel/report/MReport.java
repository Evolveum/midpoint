/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrientationType;

/**
 * Querydsl "row bean" type related to {@link QReport}.
 */
public class MReport extends MObject {

    public OrientationType orientation;
    public Boolean parent;
}
