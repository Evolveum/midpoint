/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.util;

/**
 * Exception that is thrown when action execution is in progress and input parameters combination is invalid,
 * e.g. when two mutually exclusive parameters are specified
 *
 * Not to be used for validation of individual parameters, parameter parsing using {@link com.beust.jcommander.IParameterValidator}
 * that happens in JCommander throws {@link com.beust.jcommander.ParameterException} for that.
 */
public class InputParameterException extends RuntimeException {

    private Integer exitCode;

    public InputParameterException(String message) {
        super(message);
    }

    public InputParameterException(String message, Integer exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }
}
