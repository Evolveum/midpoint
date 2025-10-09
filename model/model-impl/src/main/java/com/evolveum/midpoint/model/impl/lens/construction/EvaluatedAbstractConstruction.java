/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import java.io.Serializable;

/**
 * Facade interface for evaluated resource object and persona constructions.
 *
 * @author Radovan Semancik
 */
public interface EvaluatedAbstractConstruction<AH extends AssignmentHolderType> extends Serializable, DebugDumpable {

    AbstractConstruction<AH,?,?> getConstruction();

}
