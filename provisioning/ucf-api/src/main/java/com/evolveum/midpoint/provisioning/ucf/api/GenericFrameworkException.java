/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * This exception should be thrown if there is no other practical way how to
 * determine the problem.
 *
 * @author Radovan Semancik
 */
public class GenericFrameworkException extends UcfException {

    /**
     * Creates a new instance of <code>GenericFrameworkException</code> without detail message.
     */
    public GenericFrameworkException() {
    }

    /**
     * Constructs an instance of <code>GenericFrameworkException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public GenericFrameworkException(String msg) {
        super(msg);
    }

    public GenericFrameworkException(Exception ex) {
        super(ex);
    }

    public GenericFrameworkException(String msg, Exception ex) {
        super(msg,ex);
    }

}
