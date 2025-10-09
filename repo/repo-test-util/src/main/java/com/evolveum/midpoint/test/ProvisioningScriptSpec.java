/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
public class ProvisioningScriptSpec {

    private String code;
    private Map<String,Object> args = new HashMap<>();
    private String language;

    public ProvisioningScriptSpec(String code) {
        super();
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public void addArgSingle(String name, String val) {
        args.put(name, val);
    }

    public void addArgMulti(String name, String... val) {
        args.put(name, Arrays.asList(val));
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }



}
