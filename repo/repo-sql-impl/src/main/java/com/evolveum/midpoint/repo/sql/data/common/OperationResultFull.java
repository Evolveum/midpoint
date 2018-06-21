package com.evolveum.midpoint.repo.sql.data.common;

/**
 * @author lazyman
 */
public interface OperationResultFull extends OperationResult {

    byte[] getFullResult();

    void setFullResult(byte[] fullResult);
}
