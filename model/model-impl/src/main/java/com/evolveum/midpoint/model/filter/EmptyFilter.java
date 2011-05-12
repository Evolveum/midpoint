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

package com.evolveum.midpoint.model.filter;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DebugUtil;
import org.w3c.dom.Node;

/**
 * Empty filter. It does not tranformate the value at all.
 * It only logs it. Can be used in tests and for diagnostics.
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class EmptyFilter extends AbstractFilter {

    private static transient Trace logger = TraceManager.getTrace(EmptyFilter.class);

    @Override
    public Node apply(Node node) {
       logger.debug("EmptyFilter see {}",DebugUtil.prettyPrint(node));
       return node;
    }

}
