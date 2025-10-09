/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import java.io.Serializable;

import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;

/**
 * This interface helps handling and translation from and to
 * {@link com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType}.
 *
 * @author lazyman
 */
public interface ROperationResult extends Serializable {

    ROperationResultStatus getStatus();

    void setStatus(ROperationResultStatus status);
}
