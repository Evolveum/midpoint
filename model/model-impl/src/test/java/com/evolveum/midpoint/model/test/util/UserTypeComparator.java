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

package com.evolveum.midpoint.model.test.util;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 *
 * @author lazyman
 */
public class UserTypeComparator extends Equals<UserType> {

    private static final Trace trace = TraceManager.getTrace(UserTypeComparator.class);

    @Override
    public boolean areEqual(UserType o1, UserType o2) {
        if (!new ExtensibleObjectTypeComparator().areEqual(o1, o2)) {
            return false;
        }
        trace.warn("Comparator not comparing accounts, accountRefs (not implemented yet).");
        //TODO: compare account, account ref and so on...
        o1.getAccount();
        o1.getActivation();
        o1.getAssignment();
        o1.getCredentials();

        return areStringEqual(o1.getEmployeeNumber(), o2.getEmployeeNumber()) &&
                areStringEqual(o1.getFamilyName(), o2.getFamilyName()) &&
                areStringEqual(o1.getFullName(), o2.getFullName()) &&
                areStringEqual(o1.getGivenName(), o2.getGivenName()) &&
                areStringEqual(o1.getHonorificPrefix(), o2.getHonorificPrefix()) &&
                areStringEqual(o1.getHonorificSuffix(), o2.getHonorificSuffix()) &&
                areStringEqual(o1.getLocality(), o2.getLocality()) &&
                areStringEqual(o1.getEmployeeNumber(), o2.getEmployeeNumber()) &&
                areListsEqual(o1.getTelephoneNumber(), o2.getTelephoneNumber()) &&
                areListsEqual(o1.getOrganizationalUnit(), o2.getOrganizationalUnit()) &&
                areListsEqual(o1.getEMailAddress(), o2.getEMailAddress()) &&
                areListsEqual(o1.getAdditionalNames(), o2.getAdditionalNames()) &&
                areListsEqual(o1.getAccountRef(), o2.getAccountRef(), new ObjectReferenceTypeComparator());
    }
}
