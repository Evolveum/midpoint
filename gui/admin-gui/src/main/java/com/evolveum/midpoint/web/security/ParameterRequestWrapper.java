/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.HashMap;
import java.util.Map;

public class ParameterRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String[]> params;

    public ParameterRequestWrapper(HttpServletRequest request, String name, String value) {
        super(request);
        params = new HashMap<>(request.getParameterMap());
        params.put(name, new String[]{value});
    }

    @Override
    public String getParameter(String name) {
        String[] values = params.get(name);
        return values != null ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return params;
    }

    @Override
    public String[] getParameterValues(String name) {
        return params.get(name);
    }
}
