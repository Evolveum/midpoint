/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes.general;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DecisionList implements Serializable {

    private static final long serialVersionUID = -8603661687788638920L;

    private List<Decision> decisionList = new ArrayList<Decision>();
    private boolean preApproved = false;

    public List<Decision> getDecisionList() {
        return decisionList;
    }

    public void setDecisionList(List<Decision> decisionList) {
        this.decisionList = decisionList;
    }

    public void addDecision(Decision decision) {
        decisionList.add(decision);
    }

    public boolean isPreApproved() {
        return preApproved;
    }

    public void setPreApproved(boolean preApproved) {
        this.preApproved = preApproved;
    }

    @Override
    public String toString() {
        return "DecisionList: " + getDecisionList() + (preApproved ? " [pre-approved]" : "");
    }
}
