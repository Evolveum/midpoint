/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;
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

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

/**
 * @author Radovan Semancik
 */
@Component
public class ExpressionProfileCompiler {

    public @NotNull ExpressionProfiles compile(@NotNull SystemConfigurationExpressionsType definition)
            throws SchemaException, ConfigurationException {
        var permissionProfiles = compilePermissionProfiles(definition.getPermissionProfile());
        return compileExpressionProfiles(definition.getExpressionProfile(), permissionProfiles);
    }

    private List<ExpressionPermissionProfile> compilePermissionProfiles(
            List<ExpressionPermissionProfileType> permissionProfileBeans) throws ConfigurationException {
        List<ExpressionPermissionProfile> permissionProfiles = new ArrayList<>();
        for (ExpressionPermissionProfileType permissionProfileBean : permissionProfileBeans) {
            permissionProfiles.add(compilePermissionProfile(permissionProfileBean));
        }
        return permissionProfiles;
    }

    private ExpressionPermissionProfile compilePermissionProfile(ExpressionPermissionProfileType bean)
            throws ConfigurationException {
        return ExpressionPermissionProfile.closed(
                configNonNull(bean.getIdentifier(), "No identifier in permission profile: %s", bean),
                AccessDecision.translate(bean.getDecision()),
                bean.getPackage(),
                bean.getClazz());
    }

    private ExpressionProfiles compileExpressionProfiles(
            List<ExpressionProfileType> expressionProfileTypes, List<ExpressionPermissionProfile> permissionProfiles)
            throws ConfigurationException {
        List<ExpressionProfile> expressionProfilesList = new ArrayList<>();
        for (ExpressionProfileType expressionProfileType : expressionProfileTypes) {
            expressionProfilesList.add(compileExpressionProfile(expressionProfileType, permissionProfiles));
        }
        return new ExpressionProfiles(expressionProfilesList);
    }

    private @NotNull ExpressionProfile compileExpressionProfile(
            ExpressionProfileType expressionProfileBean, List<ExpressionPermissionProfile> permissionProfiles)
            throws ConfigurationException {
        List<ExpressionEvaluatorProfile> compiledEvaluatorProfiles = new ArrayList<>();
        for (ExpressionEvaluatorProfileType evaluatorBean : expressionProfileBean.getEvaluator()) {
            compiledEvaluatorProfiles.add(compileEvaluatorProfile(evaluatorBean, permissionProfiles));
        }
        return new ExpressionProfile(
                configNonNull(
                        expressionProfileBean.getIdentifier(), "No identifier in profile: %s", expressionProfileBean),
                AccessDecision.translate(expressionProfileBean.getDecision()),
                compiledEvaluatorProfiles);
    }

    private ExpressionEvaluatorProfile compileEvaluatorProfile(
            ExpressionEvaluatorProfileType evaluatorBean, List<ExpressionPermissionProfile> permissionProfiles)
            throws ConfigurationException {
        List<ScriptExpressionProfile> compiledScriptProfiles = new ArrayList<>();
        for (ScriptExpressionProfileType scriptProfileBean : evaluatorBean.getScript()) {
            compiledScriptProfiles.add(
                    compileScriptProfile(scriptProfileBean, permissionProfiles));
        }
        return new ExpressionEvaluatorProfile(
                configNonNull(evaluatorBean.getType(), "No evaluator type in profile: %s", evaluatorBean),
                AccessDecision.translate(evaluatorBean.getDecision()),
                compiledScriptProfiles);
    }

    private ScriptExpressionProfile compileScriptProfile(
            ScriptExpressionProfileType scriptType, List<ExpressionPermissionProfile> permissionProfiles)
            throws ConfigurationException {
        return new ScriptExpressionProfile(
                configNonNull(scriptType.getLanguage(), "No language URL in script profile: %s", scriptType),
                AccessDecision.translate(scriptType.getDecision()),
                Boolean.TRUE.equals(scriptType.isTypeChecking()),
                findPermissionProfile(
                        permissionProfiles,
                        scriptType.getPermissionProfile()));
    }

    private ExpressionPermissionProfile findPermissionProfile(
            List<ExpressionPermissionProfile> permissionProfiles, String profileIdentifier) throws ConfigurationException {
        if (profileIdentifier == null) {
            return null;
        }
        for (ExpressionPermissionProfile permissionProfile : permissionProfiles) {
            if (profileIdentifier.equals(permissionProfile.getIdentifier())) {
                return permissionProfile;
            }
        }
        throw new ConfigurationException("Permission profile '"+profileIdentifier+"' not found");
    }

}
