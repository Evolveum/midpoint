/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.expr;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 *  Adds recompute triggers to objects in an optimized way:
 *
 *  - The triggers are set to a given time in future (e.g. 1 minute from now).
 *  - If more requests to create triggers to the same object come before that time comes (minus some safety margin,
 *    e.g. 2 seconds), their creation is skipped.
 *
 * Currently we deal only with the recompute triggers. Other types can be added as necessary.
 *
 * The deduplication works best if:
 *
 * - the requests are of the same kind (i.e. either name-based or OID-based);
 * - the triggers are created on a single node, because the creators share a common state which is node-wide.
 *
 * But even if these conditions are not met, the extra triggers creation is still avoided, at the cost of extra
 * read operations against the repository.
 */
public interface OptimizingTriggerCreator {

    /**
     * Creates a trigger for the user with the given name.
     * @return true if the trigger was really added; false if it already existed or could not be added
     *
     * (Note that if the object cannot be found by the name, currently no exception is reported.)
     */
    boolean createForNamedUser(@NotNull String name) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;

    /**
     * Creates a trigger for the object with the given name.
     * @return true if the trigger was really added; false if it already existed or could not be added
     *
     * (Note that if the object cannot be found by the name, currently no exception is reported.)
     */
    boolean createForNamedObject(@NotNull Class<? extends ObjectType> type, @NotNull String name)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException;

    /**
     * Creates a trigger to the user with the given OID.
     * @return true if the trigger was really added; false if it already existed or could not be added
     */
    boolean createForObject(@NotNull Class<? extends ObjectType> type, @NotNull String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException;
}
