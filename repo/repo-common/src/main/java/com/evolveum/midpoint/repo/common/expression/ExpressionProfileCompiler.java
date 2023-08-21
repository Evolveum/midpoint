/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.expression.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.util.exception.SchemaException;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

/**
 * @author Radovan Semancik
 */
@Component
public class ExpressionProfileCompiler {

    public @NotNull ExpressionProfiles compile(@NotNull SystemConfigurationExpressionsType definition)
            throws SchemaException, ConfigurationException {
        var permissionProfiles = compilePermissionProfiles(definition.getPermissionProfile());
        var bulkActionsProfiles = compileBulkActionsProfiles(definition.getBulkActionsProfile());
        var librariesProfiles = compileLibrariesProfiles(definition.getFunctionLibrariesProfile());
        return compileExpressionProfiles(
                definition.getExpressionProfile(),
                permissionProfiles,
                bulkActionsProfiles,
                librariesProfiles);
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

    private List<BulkActionsProfile> compileBulkActionsProfiles(
            List<BulkActionsProfileType> beans) throws ConfigurationException {
        List<BulkActionsProfile> bulkActionsProfiles = new ArrayList<>();
        for (BulkActionsProfileType bean : beans) {
            bulkActionsProfiles.add(compileScriptingProfile(bean));
        }
        return bulkActionsProfiles;
    }

    private BulkActionsProfile compileScriptingProfile(BulkActionsProfileType bean)
            throws ConfigurationException {
        return BulkActionsProfile.of(bean);
    }

    private List<FunctionLibrariesProfile> compileLibrariesProfiles(
            List<FunctionLibrariesProfileType> beans) throws ConfigurationException {
        List<FunctionLibrariesProfile> profiles = new ArrayList<>();
        for (var bean : beans) {
            profiles.add(
                    FunctionLibrariesProfile.of(bean));
        }
        return profiles;
    }

    private ExpressionProfiles compileExpressionProfiles(
            List<ExpressionProfileType> expressionProfileBeans,
            List<ExpressionPermissionProfile> permissionProfiles,
            List<BulkActionsProfile> bulkActionsProfiles,
            List<FunctionLibrariesProfile> librariesProfiles)
            throws ConfigurationException {
        List<ExpressionProfile> expressionProfilesList = new ArrayList<>();
        for (ExpressionProfileType expressionProfileBean : expressionProfileBeans) {
            expressionProfilesList.add(
                    compileExpressionProfile(expressionProfileBean, permissionProfiles, bulkActionsProfiles, librariesProfiles));
        }
        return new ExpressionProfiles(expressionProfilesList);
    }

    private @NotNull ExpressionProfile compileExpressionProfile(
            ExpressionProfileType expressionProfileBean,
            List<ExpressionPermissionProfile> permissionProfiles,
            List<BulkActionsProfile> bulkActionsProfiles,
            List<FunctionLibrariesProfile> librariesProfiles)
            throws ConfigurationException {
        List<ExpressionEvaluatorProfile> compiledEvaluatorProfiles = new ArrayList<>();
        for (ExpressionEvaluatorProfileType evaluatorBean : expressionProfileBean.getEvaluator()) {
            compiledEvaluatorProfiles.add(compileEvaluatorProfile(evaluatorBean, permissionProfiles));
        }
        var evaluatorsProfile = new ExpressionEvaluatorsProfile(
                AccessDecision.translate(expressionProfileBean.getDecision()),
                compiledEvaluatorProfiles);

        return new ExpressionProfile(
                configNonNull(
                        expressionProfileBean.getIdentifier(), "No identifier in profile: %s", expressionProfileBean),
                evaluatorsProfile,
                determineScriptingProfile(bulkActionsProfiles, expressionProfileBean.getBulkActionsProfile()),
                determineLibrariesProfile(librariesProfiles, expressionProfileBean.getFunctionLibrariesProfile()),
                AccessDecision.translate(
                        Objects.requireNonNullElse(
                                expressionProfileBean.getPrivilegeElevation(),
                                AuthorizationDecisionType.ALLOW)));
    }

    private static @NotNull BulkActionsProfile determineScriptingProfile(
            List<BulkActionsProfile> bulkActionsProfiles, String profileId) throws ConfigurationException {
        if (profileId == null) {
            return BulkActionsProfile.full();
        } else {
            var matching = bulkActionsProfiles.stream()
                    .filter(p -> p.getIdentifier().equals(profileId))
                    .toList();
            return MiscUtil.extractSingletonRequired(
                    matching,
                    () -> new ConfigurationException(
                            "Multiple scripting profiles with the identifier '" + profileId + "' found"),
                    () -> new ConfigurationException(
                            "Scripting profile '" + profileId + "' not found"));
        }
    }

    private static @NotNull FunctionLibrariesProfile determineLibrariesProfile(
            List<FunctionLibrariesProfile> librariesProfiles, String librariesProfileId) throws ConfigurationException {
        if (librariesProfileId == null) {
            return FunctionLibrariesProfile.full();
        } else {
            var matching = librariesProfiles.stream()
                    .filter(p -> p.getIdentifier().equals(librariesProfileId))
                    .toList();
            return MiscUtil.extractSingletonRequired(
                    matching,
                    () -> new ConfigurationException(
                            "Multiple libraries profiles with the identifier '" + librariesProfileId + "' found"),
                    () -> new ConfigurationException(
                            "Libraries profile '" + librariesProfileId + "' not found"));
        }
    }

    private ExpressionEvaluatorProfile compileEvaluatorProfile(
            ExpressionEvaluatorProfileType evaluatorBean, List<ExpressionPermissionProfile> permissionProfiles)
            throws ConfigurationException {
        List<ScriptLanguageExpressionProfile> compiledScriptLanguageProfiles = new ArrayList<>();
        for (var scriptProfileBean : evaluatorBean.getScript()) {
            compiledScriptLanguageProfiles.add(
                    compileScriptLanguageProfile(scriptProfileBean, permissionProfiles));
        }
        return new ExpressionEvaluatorProfile(
                configNonNull(evaluatorBean.getType(), "No evaluator type in profile: %s", evaluatorBean),
                AccessDecision.translate(evaluatorBean.getDecision()),
                compiledScriptLanguageProfiles);
    }

    private ScriptLanguageExpressionProfile compileScriptLanguageProfile(
            ScriptLanguageExpressionProfileType bean, List<ExpressionPermissionProfile> permissionProfiles)
            throws ConfigurationException {
        return new ScriptLanguageExpressionProfile(
                configNonNull(bean.getLanguage(), "No language URL in script profile: %s", bean),
                AccessDecision.translate(bean.getDecision()),
                Boolean.TRUE.equals(bean.isTypeChecking()),
                findPermissionProfile(
                        permissionProfiles,
                        bean.getPermissionProfile()));
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
