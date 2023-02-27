/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.prune;

import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;

import java.util.HashMap;
import java.util.List;

public interface PruneFunctionality {

    void userPurification(int usersCount, HashMap<Integer, CandidateRole> roleDegreeMinus);

    List<Integer> identifyRemovableRoles(double minSupport, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS);

    void removeRoles(List<Integer> removableRoleKeys, HashMap<Integer,
            CandidateRole> ROLE_DEGREE_PLUS, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS);

    void updateAfterRemoveOperation(List<Integer> removableRoleKeys, HashMap<Integer, CandidateRole> ROLE_DEGREE_MINUS,
            List<HashMap<Integer, CandidateRole>> rolesDegrees, int usersCount);
}
