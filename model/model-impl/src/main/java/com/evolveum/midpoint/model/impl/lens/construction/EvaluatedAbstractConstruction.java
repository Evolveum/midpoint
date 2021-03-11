/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import java.io.Serializable;

/**
 * Facade interface for evaluated resource object and persona constructions.
 *
 * @author Radovan Semancik
 */
public interface EvaluatedAbstractConstruction<AH extends AssignmentHolderType> extends Serializable {

    AbstractConstruction<AH,?,?> getConstruction();

}
