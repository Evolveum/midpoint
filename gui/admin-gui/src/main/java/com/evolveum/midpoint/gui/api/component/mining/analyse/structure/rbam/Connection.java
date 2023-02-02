/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.structure.rbam;

public class Connection {

    public Connection(CandidateRole hParent, CandidateRole hChildren, double hConfidence) {
        this.hParent = hParent;
        this.hChildren = hChildren;
        this.hConfidence = hConfidence;
    }

    public CandidateRole gethParent() {
        return hParent;
    }

    public void sethParent(CandidateRole hParent) {
        this.hParent = hParent;
    }

    public CandidateRole gethChildren() {
        return hChildren;
    }

    public void sethChildren(CandidateRole hChildren) {
        this.hChildren = hChildren;
    }

    public double gethConfidence() {
        return hConfidence;
    }

    public void sethConfidence(double hConfidence) {
        this.hConfidence = hConfidence;
    }

    CandidateRole hParent;
    CandidateRole hChildren;
    double hConfidence;

}
