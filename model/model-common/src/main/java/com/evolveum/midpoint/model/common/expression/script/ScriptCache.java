/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.model.common.expression.script;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;

/**
 * Cache for compiled scripts and interpreters, aware of expression profiles.
 * 
 * @param <C> compiled code
 * @author Radovan Semancik
 */
public class ScriptCache<I,C> {

	private final Map<String, I> interpreterCache = new HashMap<>();
	private final Map<String, Map<String, C>> codeCache = new HashMap<>();
	
	public synchronized I getInterpreter(ExpressionProfile profile) {
		return interpreterCache.get(getProfileKey(profile));
	}
	
	public synchronized void putInterpreter(ExpressionProfile profile, I interpreter) {
		interpreterCache.put(getProfileKey(profile), interpreter);
	}
	
	public synchronized C getCode(ExpressionProfile profile, String sourceCodeKey) {
		String profileKey = getProfileKey(profile);
		Map<String, C> profileCache = codeCache.get(profileKey);
		if (profileCache == null) {
			return null;
		}
		return profileCache.get(sourceCodeKey);
	}
	
	public synchronized void putCode(ExpressionProfile profile, String sourceCodeKey, C compiledCode) {
		String profileKey = getProfileKey(profile);
		Map<String, C> profileCache = codeCache.get(profileKey);
		if (profileCache == null) {
			profileCache = new HashMap<>();
			codeCache.put(profileKey, profileCache);
		}
		profileCache.put(sourceCodeKey, compiledCode);
	}
	
	private String getProfileKey(ExpressionProfile profile) {
		if (profile == null) {
			return null;
		} else {
			return profile.getIdentifier();
		}
	}

	public synchronized void clear() {
		codeCache.clear();
	}
	
}
