package com.evolveum.midpoint.ninja.impl;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaException extends RuntimeException {

    public NinjaException(String message) {
        super(message);
    }

    public NinjaException(String message, Throwable cause) {
        super(message, cause);
    }
}
