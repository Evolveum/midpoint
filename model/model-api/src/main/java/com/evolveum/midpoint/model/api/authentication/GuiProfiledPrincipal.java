/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Principal that extends simple MidPointPrincipal with user interface concepts (user profile).
 *
 * @since 4.0
 * @author Radovan Semancik
 */
public class GuiProfiledPrincipal extends MidPointPrincipal {
    @Serial private static final long serialVersionUID = 1L;

    private CompiledGuiProfile compiledGuiProfile;
    private LoginGuiProfileInputs loginGuiProfileInputs;

    private transient int activeSessions = 0;

    public GuiProfiledPrincipal(@NotNull FocusType user) {
        super(user);
    }

    @NotNull
    public CompiledGuiProfile getCompiledGuiProfile() {
        if (compiledGuiProfile == null) {
            compiledGuiProfile = new CompiledGuiProfile();
        }
        return compiledGuiProfile;
    }

    public void setCompiledGuiProfile(CompiledGuiProfile compiledGuiProfile) {
        this.compiledGuiProfile = compiledGuiProfile;
    }

    public @Nullable LoginGuiProfileInputs getLoginGuiProfileInputs() {
        return loginGuiProfileInputs;
    }

    public void setLoginGuiProfileInputs(@Nullable LoginGuiProfileInputs loginGuiProfileInputs) {
        this.loginGuiProfileInputs = loginGuiProfileInputs;
    }

    public void clearLoginGuiProfileInputs() {
        this.loginGuiProfileInputs = null;
    }

    /**
     * Semi-shallow clone.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public GuiProfiledPrincipal clone() {
        GuiProfiledPrincipal clone = new GuiProfiledPrincipal(this.getFocus());
        copyValues(clone);
        return clone;
    }

    private void copyValues(GuiProfiledPrincipal clone) {
        super.copyValues(clone);
        // No need to clone user profile here. It is essentially read-only.
        clone.compiledGuiProfile = this.compiledGuiProfile;
        clone.loginGuiProfileInputs = this.loginGuiProfileInputs;
    }

    @Override
    protected void debugDumpInternal(StringBuilder sb, int indent) {
        super.debugDumpInternal(sb, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "compiledUserProfile", compiledGuiProfile, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "loginGuiProfileInputs", indent + 1);
        sb.append(loginGuiProfileInputs);
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GuiProfiledPrincipal)) {
            return false;
        }
        // return getFocus().equals(((GuiProfiledPrincipal) obj).getFocus());
        // We can not compare on whole focus, same goes about hashcode, we should use oid
        return Objects.equals(getFocus().getOid(), ((MidPointPrincipal) obj).getFocus().getOid());
    }

    @Override
    public int hashCode() {
        return getFocus().getOid().hashCode();
    }

    public record LoginGuiProfileInputs(List<AdminGuiConfigurationType> adminGuiConfigurations,
                                        Set<String> profileDependencies) implements Serializable {
            @Serial private static final long serialVersionUID = 1L;

            public LoginGuiProfileInputs(
                    @NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
                    @NotNull Set<String> profileDependencies) {
                this.adminGuiConfigurations = new ArrayList<>(adminGuiConfigurations);
                this.profileDependencies = new HashSet<>(profileDependencies);
            }

            @Override
            public @NotNull String toString() {
                return "LoginGuiProfileInputs(adminGuiConfigurations=" + adminGuiConfigurations.size()
                        + ", profileDependencies=" + profileDependencies.size() + ")";
            }
        }
}
