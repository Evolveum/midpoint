/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
