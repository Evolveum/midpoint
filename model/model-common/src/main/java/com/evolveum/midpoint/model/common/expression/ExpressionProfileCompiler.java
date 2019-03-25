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
package com.evolveum.midpoint.model.common.expression;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfiles;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionEvaluatorProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationExpressionsType;

/**
 * @author Radovan Semancik
 */
@Component
public class ExpressionProfileCompiler {
	
	public ExpressionProfiles compile(SystemConfigurationExpressionsType expressionsType) throws SchemaException {
		List<ExpressionPermissionProfile> permissionProfiles = compilePermissionProfiles(expressionsType.getPermissionProfile());
		ExpressionProfiles expressionProfiles = compileExpressionProfiles(expressionsType.getExpressionProfile(), permissionProfiles);
		return expressionProfiles;
	}

	private List<ExpressionPermissionProfile> compilePermissionProfiles(List<ExpressionPermissionProfileType> permissionProfileTypes) {
		List<ExpressionPermissionProfile> permissionProfiles = new ArrayList<>();
		for (ExpressionPermissionProfileType permissionProfileType : permissionProfileTypes) {
			permissionProfiles.add(compilePermissionProfile(permissionProfileType));
		}
		return permissionProfiles;
	}

	private ExpressionPermissionProfile compilePermissionProfile(ExpressionPermissionProfileType permissionProfileType) {
		ExpressionPermissionProfile profile = new ExpressionPermissionProfile(permissionProfileType.getIdentifier());
		
		profile.setDecision(AccessDecision.translate(permissionProfileType.getDecision()));
		profile.getPackageProfiles().addAll(permissionProfileType.getPackage());
		profile.getClassProfiles().addAll(permissionProfileType.getClazz());
		
		return profile;
	}
	
	private ExpressionProfiles compileExpressionProfiles(List<ExpressionProfileType> expressionProfileTypes, List<ExpressionPermissionProfile> permissionProfiles) throws SchemaException {
		ExpressionProfiles expressionProfiles = new ExpressionProfiles();
		for(ExpressionProfileType expressionProfileType : expressionProfileTypes) {
			expressionProfiles.add(compileExpressionProfile(expressionProfileType, permissionProfiles));
		}
		return expressionProfiles;
	}

	private ExpressionProfile compileExpressionProfile(ExpressionProfileType expressionProfileType, List<ExpressionPermissionProfile> permissionProfiles) throws SchemaException {
		ExpressionProfile profile = new ExpressionProfile(expressionProfileType.getIdentifier());
		
		profile.setDecision(AccessDecision.translate(expressionProfileType.getDecision()));
		
		for(ExpressionEvaluatorProfileType evaluatorType : expressionProfileType.getEvaluator()) {
			profile.add(compileEvaluatorProfile(evaluatorType, permissionProfiles));
		}
		
		return profile;
	}

	private ExpressionEvaluatorProfile compileEvaluatorProfile(ExpressionEvaluatorProfileType evaluatorType, List<ExpressionPermissionProfile> permissionProfiles) throws SchemaException {
		ExpressionEvaluatorProfile profile = new ExpressionEvaluatorProfile(evaluatorType.getType());
		
		profile.setDecision(AccessDecision.translate(evaluatorType.getDecision()));
		
		for (ScriptExpressionProfileType scriptType : evaluatorType.getScript()) {
			profile.add(compileScriptProfile(scriptType, permissionProfiles));
		}
		
		return profile;
	}

	private ScriptExpressionProfile compileScriptProfile(ScriptExpressionProfileType scriptType, List<ExpressionPermissionProfile> permissionProfiles) throws SchemaException {
		ScriptExpressionProfile profile = new ScriptExpressionProfile(scriptType.getLanguage());
		
		profile.setDecision(AccessDecision.translate(scriptType.getDecision()));
		profile.setTypeChecking(scriptType.isTypeChecking());
		
		profile.setPermissionProfile(findPermissionProfile(permissionProfiles, scriptType.getPermissionProfile()));
		
		return profile;
	}

	private ExpressionPermissionProfile findPermissionProfile(List<ExpressionPermissionProfile> permissionProfiles, String profileIdentifier) throws SchemaException {
		if (profileIdentifier == null) {
			return null;
		}
		for (ExpressionPermissionProfile permissionProfile : permissionProfiles) {
			if (profileIdentifier.equals(permissionProfile.getIdentifier())) {
				return permissionProfile;
			}
		}
		throw new SchemaException("Permission profile '"+profileIdentifier+"' not found");
	}

}
