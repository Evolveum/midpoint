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

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType.SynchronizationState;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author elek
 */
public class SynchronizationResult {

    private List<Change> changes = new ArrayList();

    public List<SynchronizationResult.Change> getChanges() {
        return changes;
    }

    public void addChange(SynchronizationResult.Change change) {
        changes.add(change);
    }

    public static class Change {

        private ResourceObject identifier;

        private ObjectChangeType change;

        private ResourceStateType.SynchronizationState token;

        public Change(ResourceObject identifier, ObjectChangeType change, SynchronizationState token) {
            this.identifier = identifier;
            this.change = change;
            this.token = token;
        }

        public ObjectChangeType getChange() {
            return change;
        }

        public void setChange(ObjectChangeType change) {
            this.change = change;
        }

        // Is ResourceObject really the right type here?
        // TODO: If yes then explain why
        public ResourceObject getIdentifier() {
            return identifier;
        }

        public void setIdentifier(ResourceObject identifier) {
            this.identifier = identifier;
        }

        public SynchronizationState getToken() {
            return token;
        }

        public void setToken(SynchronizationState token) {
            this.token = token;
        }
    }
}
