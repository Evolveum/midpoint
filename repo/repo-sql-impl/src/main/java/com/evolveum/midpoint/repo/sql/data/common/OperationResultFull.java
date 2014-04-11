package com.evolveum.midpoint.repo.sql.data.common;

/**
 * @author lazyman
 */
public interface OperationResultFull extends OperationResult {

    String getFullResult();

    void setFullResult(String fullResult);
}
