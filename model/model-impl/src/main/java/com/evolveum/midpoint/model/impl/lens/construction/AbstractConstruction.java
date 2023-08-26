/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A superclass for both resource object constructions ({@link ResourceObjectConstruction})
 * and persona object constructions ({@link PersonaConstruction}).
 *
 * Contains the construction definition (bean) and the relevant context: assignment path, source object, lens context,
 * validity information. Basically, everything that is needed to evaluate this construction.
 *
 * Note: it is still not quite clear how much is persona construction evaluation similar to the evaluation of a resource
 * object construction. Persona constructions are currently evaluated using object template evaluation mechanism, while
 * resource object constructions evaluation is based on attribute/association mappings evaluation directly in
 * {@link EvaluatedResourceObjectConstructionImpl} and its subclass(es).
 *
 * @param <AH> focus type to which this construction applies
 * @param <ACT> type of the construction bean (e.g. ConstructionType, PersonaConstructionType)
 * @param <EC> "EvaluatedXXX" class paired with the construction
 * (e.g. {@link EvaluatedPlainResourceObjectConstructionImpl}, {@link EvaluatedPersonaConstructionImpl})
 *
 * @author Radovan Semancik
 */
public abstract class AbstractConstruction<
        AH extends AssignmentHolderType, ACT extends AbstractConstructionType, EC extends EvaluatedAbstractConstruction<AH>>
        implements DebugDumpable, Serializable {

    /**
     * Definition of the assigned construction wrapped as a configuration item.
     * (For "artificial" constructions created during outbound mappings evaluations it is null.)
     *
     * [EP:CONSTR] DONE 1/1
     */
    @Nullable final ConfigurationItem<ACT> constructionConfigItem;

    /**
     * The real value from {@link #constructionConfigItem}, if present. Just for convenience.
     */
    @Nullable final ACT constructionBean;

    /**
     * If this construction is assigned, this is the path to it.
     * (For "artificial" constructions created during outbound mappings evaluations it is null.)
     */
    @Nullable protected final AssignmentPathImpl assignmentPath;

    /**
     * Object in which the construction is defined: either assignment segment source object
     * (assignment holder) or a resource object.
     *
     * TODO consider if not superseded by the origin in {@link #constructionConfigItem},
     *  at least for assigned constructions
     */
    @NotNull protected final ObjectType source;

    /**
     * Origin of this construction.
     */
    @NotNull protected final OriginType originType;

    /**
     * Lens context in which this construction is collected and evaluated.
     */
    @NotNull protected final LensContext<AH> lensContext;

    /**
     * Current time. Should be copied from the projector's current time, but it is currently not always the case.
     */
    @NotNull protected final XMLGregorianCalendar now;

    /**
     * Focus ODO. Should be the absolute one i.e. OLD -> summary delta -> NEW.
     *
     * Intentionally not final. It is set just before the construction evaluation
     * in order to ensure it is up to date. (The constructions are collected before
     * focus mappings are evaluated. And focus mappings can provide some item deltas.)
     *
     * Actually it is not quite clear if this field is relevant also for persona constructions.
     * These are currently evaluated using object template evaluator, not using EvaluatedXXXConstruction
     * evaluate() methods.
     */
    private ObjectDeltaObject<AH> focusOdoAbsolute;

    /**
     * Is the construction valid in the new state, i.e.
     * - is the whole assignment path active (regarding activation and lifecycle state),
     * - and are all conditions on the path enabled? (EXCLUDING the focus object itself)
     */
    private final boolean valid;

    /**
     * Was the construction valid in the focus old state?
     *
     * FIXME It is not sure that we set this value correctly. It looks like we simply take wasValid value for evaluated assignment.
     *  MID-6404
     */
    private boolean wasValid = true;

    /**
     * Variables related to the assignmentPath. Lazily evaluated.
     */
    private AssignmentPathVariables assignmentPathVariables;

    AbstractConstruction(AbstractConstructionBuilder<AH, ACT, EC, ?> builder) {
        this.assignmentPath = builder.assignmentPath;
        this.constructionConfigItem = builder.constructionConfigItem; // [EP:CONSTR] DONE
        this.constructionBean = constructionConfigItem != null ? constructionConfigItem.value() : null;
        this.source = builder.source;
        this.originType = builder.originType;
        this.lensContext = builder.lensContext;
        this.now = builder.now;
        this.valid = builder.valid;
    }

    public @NotNull ObjectType getSource() {
        return source;
    }

    public @NotNull OriginType getOriginType() {
        return originType;
    }

    public @NotNull LensContext<AH> getLensContext() {
        return lensContext;
    }

    public @Nullable ACT getConstructionBean() {
        return constructionBean;
    }

    public ObjectDeltaObject<AH> getFocusOdoAbsolute() {
        return focusOdoAbsolute;
    }

    public void setFocusOdoAbsolute(ObjectDeltaObject<AH> focusOdoAbsolute) {
        this.focusOdoAbsolute = focusOdoAbsolute;
    }

    public boolean isWeak() {
        return constructionBean != null
                && constructionBean.getStrength() == ConstructionStrengthType.WEAK;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean getWasValid() {
        return wasValid;
    }

    public void setWasValid(boolean wasValid) {
        this.wasValid = wasValid;
    }

    public @Nullable AssignmentPathImpl getAssignmentPath() {
        return assignmentPath;
    }

    public abstract DeltaSetTriple<EC> getEvaluatedConstructionTriple();

    /**
     * Typical reason for being ignored is that the resourceRef cannot be resolved.
     */
    abstract public boolean isIgnored();

    AssignmentPathVariables getAssignmentPathVariables() throws SchemaException {
        if (assignmentPathVariables == null) {
            assignmentPathVariables = assignmentPath != null ? assignmentPath.computePathVariables() : null;
        }
        return assignmentPathVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof AbstractConstruction<?, ?, ?> that)) {
            return false;
        } else {
            return valid == that.valid
                    && wasValid == that.wasValid
                    && Objects.equals(constructionBean, that.constructionBean)
                    && Objects.equals(assignmentPath, that.assignmentPath);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(constructionBean);
    }

    void debugDumpConstructionDescription(StringBuilder sb, int indent) {
        String description = constructionBean != null ? constructionBean.getDescription() : null;
        if (description != null) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "description", indent + 1);
            sb.append(" ").append(description);
        }
    }

    void debugDumpAssignmentPath(StringBuilder sb, int indent) {
        if (assignmentPath != null) {
            sb.append("\n");
            sb.append(assignmentPath.debugDump(indent + 1));
        }
    }
}
