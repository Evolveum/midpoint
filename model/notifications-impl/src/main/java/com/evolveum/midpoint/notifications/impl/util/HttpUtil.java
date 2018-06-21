/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpMethodType;
import org.springframework.http.HttpMethod;

/**
 * @author mederly
 */
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
