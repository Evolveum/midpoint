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

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import java.util.UUID;
import javax.xml.namespace.QName;

/**
 * This needs to be a common object. It's a temporary solution.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class MidPointResult {

    public static final String code_id = "$Id$";
    private OperationalResultType _currentOperationalResult;
    private OperationalResultType _parentOperationalResult;

    public MidPointResult() {
        _currentOperationalResult = new OperationalResultType();
        _currentOperationalResult.setOid(UUID.randomUUID().toString());
    }

    public MidPointResult(OperationalResultType result, QName operation) {
        _currentOperationalResult = new OperationalResultType();
        _currentOperationalResult.setOid(UUID.randomUUID().toString());
        _currentOperationalResult.setType(operation);
        _parentOperationalResult = result;
    }

    public void addNamedResult(String name, Object value) {
    }

    public void addException(Throwable t) {
    }

    public Object getResult(String type) {
        return "TEST";
    }

    public boolean hasError(){
        return false;
    }
}
