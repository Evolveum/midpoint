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

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.NotificationRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotifierConfigurationType;

/**
 * @author mederly
 */
public interface Notifier {

    public void notify(NotificationRequest request, NotificationConfigurationEntryType notificationConfigurationEntry,
                       NotifierConfigurationType notifierConfiguration);
}
