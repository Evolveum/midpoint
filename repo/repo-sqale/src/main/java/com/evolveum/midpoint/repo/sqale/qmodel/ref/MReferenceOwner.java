/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

/**
 * Very abstract contract for entities (objects, containers) that can own references
 * stored in separate table.
 *
 * @param <T> type of reference row related to the owner
 */
public interface MReferenceOwner<T extends MReference> {

    /** Creates the reference and populates the foreign key. */
    T createReference();
}
