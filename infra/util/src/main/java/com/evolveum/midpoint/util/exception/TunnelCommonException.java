package com.evolveum.midpoint.util.exception;

public class TunnelCommonException extends TunnelException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public TunnelCommonException(CommonException cause) {
        super(cause);
    }

    public void rethrow() throws CommonException {
        throw (CommonException) getCause();
    }

}
