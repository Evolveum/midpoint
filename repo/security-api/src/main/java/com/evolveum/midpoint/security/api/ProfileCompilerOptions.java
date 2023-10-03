/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

public class ProfileCompilerOptions {

    private boolean compileGuiAdminConfiguration = true;

    private boolean collectAuthorization = true;

    private boolean locateSecurityPolicy = true;

    private boolean tryReusingSecurityPolicy = false;
    private boolean runAsRunner = false;
    private boolean terminateDisabledUserSession = true;

    private ProfileCompilerOptions(){
    }

    public ProfileCompilerOptions compileGuiAdminConfiguration(boolean compileGuiAdminConfiguration) {
        this.compileGuiAdminConfiguration = compileGuiAdminConfiguration;
        return this;
    }

    public ProfileCompilerOptions collectAuthorization(boolean collectAuthorization) {
        this.collectAuthorization = collectAuthorization;
        return this;
    }

    public ProfileCompilerOptions locateSecurityPolicy(boolean locateSecurityPolicy) {
        this.locateSecurityPolicy = locateSecurityPolicy;
        return this;
    }

    public ProfileCompilerOptions tryReusingSecurityPolicy(boolean tryReusingSecurityPolicy) {
        this.tryReusingSecurityPolicy = tryReusingSecurityPolicy;
        return this;
    }

    public ProfileCompilerOptions runAsRunner(boolean runAsRunner) {
        this.runAsRunner = runAsRunner;
        return this;
    }

    public ProfileCompilerOptions terminateDisabledUserSession(boolean terminateDisabledUserSession) {
        this.terminateDisabledUserSession = terminateDisabledUserSession;
        return this;
    }

    public static ProfileCompilerOptions create() {
        return new ProfileCompilerOptions();
    }
    public static ProfileCompilerOptions createNotCompileGuiAdminConfiguration() {
        return create().compileGuiAdminConfiguration(false);
    }

    public static ProfileCompilerOptions createOnlyPrincipalOption() {
        return createNotCompileGuiAdminConfiguration().locateSecurityPolicy(false).collectAuthorization(false);
    }

    public boolean isCompileGuiAdminConfiguration() {
        return compileGuiAdminConfiguration;
    }

    public boolean isCollectAuthorization() {
        return collectAuthorization;
    }

    public boolean isLocateSecurityPolicy() {
        return locateSecurityPolicy;
    }

    public boolean isTryReusingSecurityPolicy() {
        return tryReusingSecurityPolicy;
    }

    public boolean isRunAsRunner() {
        return runAsRunner;
    }

    public boolean terminateDisabledUserSession() {
        return terminateDisabledUserSession;
    }
}
