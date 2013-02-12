/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications.request;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 10.1.2013
 * Time: 23:26
 * To change this template use File | Settings | File Templates.
 */
public class AccountNotificationRequest extends NotificationRequest {

    private ResourceOperationDescription accountOperationDescription;

    private ChangeType changeType;

    // the following two are currently unused
    private boolean activationRequested;
    private boolean deactivationRequested;

    public ResourceOperationDescription getAccountOperationDescription() {
        return accountOperationDescription;
    }

    public void setAccountOperationDescription(ResourceOperationDescription accountOperationDescription) {
        this.accountOperationDescription = accountOperationDescription;
    }

    public boolean isActivationRequested() {
        return activationRequested;
    }

    public void setActivationRequested(boolean activationRequested) {
        this.activationRequested = activationRequested;
    }

    public boolean isDeactivationRequested() {
        return deactivationRequested;
    }

    public void setDeactivationRequested(boolean deactivationRequested) {
        this.deactivationRequested = deactivationRequested;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
}
