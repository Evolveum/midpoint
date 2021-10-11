/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

/**
 *
 * @author Radovan Semancik
 */
public abstract class UcfException extends Exception {

    /**
     * Creates a new instance of <code>UcfException</code> without detail message.
     */
    public UcfException() {
    }

    /**
     * Constructs an instance of <code>UcfException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public UcfException(String msg) {
        super(msg);
    }

    public UcfException(String msg,Throwable cause) {
        super(msg,cause);
    }

    public UcfException(Throwable cause) {
        super(cause);
    }

}
