/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.Serial;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

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
    }

    @Override
    protected void debugDumpInternal(StringBuilder sb, int indent) {
        super.debugDumpInternal(sb, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "compiledUserProfile", compiledGuiProfile, indent + 1);
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
}
