/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.AssignmentPathUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * Path from the source object (focus) to the ultimate assignment that is being processed or referenced.
 * The path consists of a chain (list) of segments. Each segment corresponds to a single assignment or inducement.
 * The source of the first segment is the focus. Source of each following segment (i.e. assignment) is the target
 * of previous segment (i.e. assignment).
 *
 * @author semancik
 */
public interface AssignmentPath extends DebugDumpable, ShortDumpable, Cloneable, Serializable {

    List<? extends AssignmentPathSegment> getSegments();

    /**
     * Returns segment specified by index. Negative indexes work in reverse direction.
     */
    AssignmentPathSegment getSegment(int index);

    AssignmentPathSegment first();

    boolean isEmpty();

    int size();

    AssignmentPathSegment last();

    // beforeLast(0) means last()
    // beforeLast(1) means one before last()
    AssignmentPathSegment beforeLast(int n);

    int countTargetOccurrences(ObjectType target);

    /**
     * Returns a "user understandable" part of this path. I.e. only those objects that are of "order 1" above the focal object.
     * E.g. from chain of
     *
     * jack =(a)=> Engineer =(i)=> Employee =(a)=> PersonMetarole =(i2)=> Person =(i)=> Entity
     *
     * the result would be
     *
     * Engineer -> Employee -> Person -> Entity
     *
     * TODO find a better name
     */
    @NotNull
    List<ObjectType> getFirstOrderChain();

    /**
     * In the context of meta-roles this is the role that the currently-processed inducement "applies to".
     * I.e. the role that would contain this inducement in case that meta-roles were not used.
     * Technically, this is the last element in the "first order chain" or roles.
     *
     * Note: proto- is the opposite of meta-
     */
    ObjectType getProtoRole();

    /**
     * Shallow clone.
     */
    AssignmentPath clone();

    AssignmentPath cloneFirst(int n);

    AssignmentPathType toAssignmentPathType(boolean includeAssignmentsContent);

    ExtensionType collectExtensions(int startAt) throws SchemaException;

    static ExtensionType collectExtensions(AssignmentPathType path, int startAt, ModelService modelService, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return AssignmentPathUtil.collectExtensions(path, startAt, modelService, task, result);
    }

    // Groovy [] operator
    default AssignmentPathSegment getAt(int index) {
        return getSegment(index);
    }

    /**
     * Returns true if the path matches specified order constraints. All of them must match.
     * Although there are some defaults, it is recommended to specify constraints explicitly.
     * Currently not supported on empty paths.
     *
     * Not all parts of OrderConstraintsType are supported. Namely, resetOrder item has no meaning here.
     */
    boolean matches(@NotNull List<OrderConstraintsType> orderConstraints);

    /**
     * Preliminary (limited) implementation. To be used to compare paths pointing to the same target object. Use with care.
     */
    boolean equivalent(AssignmentPath other);

    default boolean containsDelegation() {
        return getSegments().stream()
                .anyMatch(seg -> seg.isDelegation());
    }

    /**
     * Returns the limitation for "other" privileges that are delegated through this path.
     */
    @NotNull OtherPrivilegesLimitations.Limitation getOtherPrivilegesLimitation();
}
