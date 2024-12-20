/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Resource object, full or partial.
 *
 * Partial case is used for associations, when identifiers are carried in the association value.
 */
public abstract class UcfResourceObjectFragment
        implements DebugDumpable, ShortDumpable, Checkable, AbstractShadow {

    /**
     * The resource object itself, potentially incomplete. It may not have a primary identifier,
     * even if the object class requires it. (Which is currently the case for all classes.
     * But - in the future - associated objects may exist without a primary identifier.)
     * */
    @NotNull final ShadowType bean;

    /** Error state of the object. May be set e.g. if the object could not be correctly translated from ConnId. */
    @NotNull final UcfErrorState errorState;

    UcfResourceObjectFragment(@NotNull ShadowType bean, @NotNull UcfErrorState errorState) {
        this.bean = bean;
        this.errorState = errorState;
        // Not doing consistency check here; the subclasses are responsible for that.
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public @NotNull UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public abstract UcfResourceObjectFragment clone();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[%s (%s)]".formatted(bean, errorState);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(this.getClass().getSimpleName(), indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "errorState", errorState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "bean", bean, indent + 1);
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(bean));
        if (errorState.isError()) {
            sb.append(" (").append(errorState).append(")");
        }
    }

    @Override
    public void checkConsistence() {
        AbstractShadow.super.checkConsistence();
        try {
            // Class name check
            var classNameInData = getObjectClassName();
            var classNameInDefinition = ShadowUtil.getResourceObjectDefinition(bean).getTypeName();
            stateCheck(QNameUtil.match(classNameInData, classNameInDefinition),
                    "Object class mismatch in %s: data %s, definition %s",
                    this, classNameInData, classNameInDefinition);
        } catch (SchemaException e) {
            throw checkFailedException(e);
        }
    }
}
