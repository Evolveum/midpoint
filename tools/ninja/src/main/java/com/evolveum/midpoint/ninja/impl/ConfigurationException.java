package com.evolveum.midpoint.ninja.impl;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConfigurationException extends NinjaException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
