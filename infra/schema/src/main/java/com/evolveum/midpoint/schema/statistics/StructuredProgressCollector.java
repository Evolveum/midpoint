/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

/**
 * Object capable of receiving updates on structured progress.
 */
@Experimental
public interface StructuredProgressCollector {

    void setStructuredProgressPartInformation(String partUri, Integer partNumber, Integer expectedParts);

    void incrementStructuredProgress(String partUri, QualifiedItemProcessingOutcomeType outcome);

}
