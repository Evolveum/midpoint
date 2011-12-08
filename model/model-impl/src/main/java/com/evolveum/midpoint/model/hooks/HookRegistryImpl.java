/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.hooks;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;

/**
 * @author semancik
 *
 */
@Component
public class HookRegistryImpl implements HookRegistry {

	private Map<String,ChangeHook> changeHookMap = new HashMap<String, ChangeHook>();
	
	@Override
	public void registerChangeHook(String url, ChangeHook changeHook) {
		changeHookMap.put(url,changeHook);
	}

}
