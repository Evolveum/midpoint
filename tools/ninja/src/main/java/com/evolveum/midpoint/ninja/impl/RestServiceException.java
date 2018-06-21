package com.evolveum.midpoint.ninja.impl;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RestServiceException extends NinjaException {

    private OperationResult result;

    public RestServiceException(String message) {
        super(message);
    }

    public RestServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestServiceException(String message, OperationResult result) {
        super(message);
        this.result = result;
    }
}
