/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query.builder;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface S_MatchingRuleEntry extends S_AtomicFilterExit {
    S_AtomicFilterExit matchingOrig();
    S_AtomicFilterExit matchingNorm();
    S_AtomicFilterExit matchingStrict();
    S_AtomicFilterExit matchingCaseIgnore();
    S_AtomicFilterExit matching(QName matchingRuleName);
}
