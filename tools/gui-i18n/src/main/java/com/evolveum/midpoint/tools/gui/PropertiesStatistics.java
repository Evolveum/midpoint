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

package com.evolveum.midpoint.tools.gui;

/**
 * @author lazyman
 */
public class PropertiesStatistics {

    private int added;
    private int deleted;

    public int getAdded() {
        return added;
    }

    public void incrementAdded(int value) {
        added += value;
    }

    public void incrementDeleted(int value) {
        deleted += value;
    }

    public void incrementAdded() {
        added++;
    }

    public void incrementDeleted() {
        deleted++;
    }

    public int getDeleted() {
        return deleted;
    }

    @Override
    public String toString() {
        return "{" + "added=" + added + ", deleted=" + deleted + "}";
    }
}
