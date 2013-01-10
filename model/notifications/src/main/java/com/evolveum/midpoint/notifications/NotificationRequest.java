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

package com.evolveum.midpoint.notifications;

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

    /*
     * concrete source data from which the notification has to be constructed
     * these are a bit specific to the notifiers
     * currently there are the following ones:
     * - accountDeltas: a list of account deltas
     * - modelContext: whole model context; for its complexity it would be better to work only with the deltas,
     *   however, some information is not there (e.g. resource names, ...)
     *
     * THIS IS TO BE CHANGED LATER
     */
    private Map<String,Object> parameters = new HashMap<String,Object>();

    private QName source;       // currently unused

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public QName getSource() {
        return source;
    }

    public void setUser(UserType user) {
        this.user = user;
    }

    public UserType getUser() {
        return user;
    }

    public Object getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public String toString() {
        return "NotificationRequest{" +
                "user=" + user +
                ", parameters=" + parameters +
                ", source=" + source +
                '}';
    }
}
