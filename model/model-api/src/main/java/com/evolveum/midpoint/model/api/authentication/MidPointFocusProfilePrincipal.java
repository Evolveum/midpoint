/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Principal that extends simple MidPointPrincipal with user interface concepts (user profile).
 *
 * @since 4.0
 * @author Radovan Semancik
 */
public class MidPointFocusProfilePrincipal extends MidPointPrincipal {
    private static final long serialVersionUID = 1L;

    private CompiledUserProfile compiledUserProfile;

    private transient int activeSessions = 0;

    public MidPointFocusProfilePrincipal(@NotNull FocusType user) {
        super(user);
    }

    @NotNull
    public CompiledUserProfile getCompiledUserProfile() {
        if (compiledUserProfile == null) {
            compiledUserProfile = new CompiledUserProfile();
        }
        return compiledUserProfile;
    }

    public void setCompiledUserProfile(CompiledUserProfile compiledUserProfile) {
        this.compiledUserProfile = compiledUserProfile;
    }

    /**
     * Semi-shallow clone.
     */
    public MidPointFocusProfilePrincipal clone() {
        MidPointFocusProfilePrincipal clone = new MidPointFocusProfilePrincipal(this.getFocus());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(MidPointFocusProfilePrincipal clone) {
        super.copyValues(clone);
        // No need to clone user profile here. It is essentially read-only.
        clone.compiledUserProfile = this.compiledUserProfile;
    }

    @Override
    protected void debugDumpInternal(StringBuilder sb, int indent) {
        super.debugDumpInternal(sb, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "compiledUserProfile", compiledUserProfile, indent + 1);
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MidPointFocusProfilePrincipal)) {
            return false;
        }
        return getFocus().equals(((MidPointFocusProfilePrincipal) obj).getFocus());
    }

    @Override
    public int hashCode() {
        return getFocus().hashCode();
    }
}
