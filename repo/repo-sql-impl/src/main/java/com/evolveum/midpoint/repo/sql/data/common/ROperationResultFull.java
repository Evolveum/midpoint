/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common;

/**
 * @author lazyman
 */
public interface ROperationResultFull extends ROperationResult {

    byte[] getFullResult();

    void setFullResult(byte[] fullResult);
}
