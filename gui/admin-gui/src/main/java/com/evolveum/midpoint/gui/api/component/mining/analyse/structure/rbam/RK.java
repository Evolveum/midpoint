/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam;

import java.util.List;

public class RK {

    public RK(List<CandidateRole> candidatesGroup, int degree) {
        this.candidatesGroup = candidatesGroup;
        this.degree = degree;
    }

    public List<CandidateRole> getCandidatesGroup() {
        return candidatesGroup;
    }

    public void setCandidatesGroup(List<CandidateRole> candidatesGroup) {
        this.candidatesGroup = candidatesGroup;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    List<CandidateRole> candidatesGroup;
    int degree;

}
