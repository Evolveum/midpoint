/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpMethodType;
import org.springframework.http.HttpMethod;

public class HttpUtil {

    // Unfortunately, this requires spring-web, so it cannot be moved e.g. into schema module.
    public static HttpMethod toHttpMethod(HttpMethodType method) {
        if (method == null) {
            return null;
        }
        switch (method) {
            case GET: return HttpMethod.GET;
            case POST: return HttpMethod.POST;
            case PUT: return HttpMethod.PUT;
            case HEAD: return HttpMethod.HEAD;
            case PATCH: return HttpMethod.PATCH;
            case DELETE: return HttpMethod.DELETE;
            case OPTIONS: return HttpMethod.OPTIONS;
            case TRACE: return HttpMethod.TRACE;
            default: throw new AssertionError("HttpMethodType: " + method);
        }
    }
}
