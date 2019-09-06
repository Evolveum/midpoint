/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;

import java.io.Serializable;

/**
 * This interface helps handling and translation from and to
 * to {@link com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType}.
 *
 * @author lazyman
 */
public interface OperationResult extends Serializable {

    ROperationResultStatus getStatus();

    void setStatus(ROperationResultStatus status);
}
