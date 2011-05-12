/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util.diff;

import com.evolveum.midpoint.logging.TraceManager;
import java.util.LinkedList;
import java.util.List;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.slf4j.Logger;
import org.w3c.dom.Node;

/**
 * Implementation of XMLUnit's DifferenceListener.
 * Decides which XMLUnit differences should be accepted and which ignored.
 *
 * @see DifferenceListener
 * 
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class MidPointDifferenceListener implements DifferenceListener {

    private static final Logger logger = TraceManager.getTrace(MidPointDifferenceListener.class);

    public MidPointDifferenceListener() {
    }

    @Override
    public int differenceFound(Difference difference) {

        if ((DiffConstants.isForReplaceProperty(difference))) {
            //here accept all diff for replace properties
            return RETURN_ACCEPT_DIFFERENCE;
        }

        if ((difference.equals(DifferenceConstants.TEXT_VALUE)) && (DiffConstants.isForContainerProperty(difference))) {
            //ignore textual diff for container
            return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
        }

        if ((difference.equals(DifferenceConstants.TEXT_VALUE)) && (!DiffConstants.isForContainerProperty(difference))) {
            //accept textual diff for simple properties
            return RETURN_ACCEPT_DIFFERENCE;
        }

        Node controlNode = difference.getControlNodeDetail().getNode();
        if ((difference.equals(DifferenceConstants.CHILD_NODE_NOT_FOUND)) && (null != controlNode) && (controlNode.getNodeType() == Node.TEXT_NODE)) {
            //ignore simple properties (remove) identified by text() change
            return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
        }

        return DiffConstants.IGNORE_DIFFERENCES.contains(difference) ? RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL : RETURN_ACCEPT_DIFFERENCE;
    }

    @Override
    public void skippedComparison(Node nodeControl, Node nodeTest) {
        logger.warn("Skipped comparison for " + nodeControl + " " + nodeTest);
    }
}
