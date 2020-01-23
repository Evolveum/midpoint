/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

/**
 * @author lazyman
 */
public interface OperationResultFull extends OperationResult {

    byte[] getFullResult();

    void setFullResult(byte[] fullResult);
}
