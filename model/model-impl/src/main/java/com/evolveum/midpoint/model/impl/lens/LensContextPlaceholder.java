/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * This class does nothing. It just takes place when no real Lens Context is available.
 * @see ModelExpressionThreadLocalHolder
 * 
 * @author semancik
 *
 */
public class LensContextPlaceholder<F extends ObjectType> extends LensContext<F> {

	public LensContextPlaceholder(PrismObject<F> focus, PrismContext prismContext) {
		super(prismContext);
		createFocusContext((Class<F>) focus.asObjectable().getClass());
		getFocusContext().setLoadedObject(focus);
	}

	@Override
	public String toString() {
		return "LensContextPlaceholder()";
	}

	@Override
	public String dump(boolean showTriples) {
		return "LensContextPlaceholder()";
	}

	@Override
	public String debugDump(int indent, boolean showTriples) {
		StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("LensContextPlaceholder");
        return sb.toString();
	}
	
	
}
