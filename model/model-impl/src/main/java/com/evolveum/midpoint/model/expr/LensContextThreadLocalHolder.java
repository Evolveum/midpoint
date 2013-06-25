/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.expr;

import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
public class LensContextThreadLocalHolder {
	
	private static ThreadLocal<LensContext<ObjectType, ShadowType>> threadLocalContext = new ThreadLocal<LensContext<ObjectType, ShadowType>>();
	
	public static <F extends ObjectType, P extends ObjectType> void set(LensContext<F, P> ctx) {
		threadLocalContext.set((LensContext<ObjectType, ShadowType>) ctx);
	}
	
	public static <F extends ObjectType, P extends ObjectType> void reset() {
		threadLocalContext.set(null);
	}

	public static <F extends ObjectType, P extends ObjectType> LensContext<F, P> get() {
		return (LensContext<F, P>) threadLocalContext.get();
	}
	
}
