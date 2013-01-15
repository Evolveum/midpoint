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

import com.evolveum.midpoint.notifications.OperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class NotificationRequest {

    // a user to which the notification has to be sent
    private UserType user;

    // status of the operation
    private OperationStatus operationStatus;

    private QName source;       // currently unused

    public QName getSource() {
        return source;
    }

    public void setUser(UserType user) {
        this.user = user;
    }

    public UserType getUser() {
        return user;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    @Override
    public String toString() {
        return "NotificationRequest{" +
                "user=" + user +
                ", operationStatus=" + operationStatus +
                ", source=" + source +
                '}';
    }
}
