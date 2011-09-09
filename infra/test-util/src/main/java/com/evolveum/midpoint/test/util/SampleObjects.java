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

package com.evolveum.midpoint.test.util;

/**
 * Enumeration of a sample object identifiers (OIDs).
 * 
 * The identifiers are meant to identify objects in files
 * in test-data/repository directory.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public enum SampleObjects {

    /**
     * Localhost OpenDJ (ResourceType)
     */
    RESOURCETYPE_LOCALHOST_OPENDJ("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2"),
    /**
     * Flatfile (ResourceType)
     */
    RESOURCETYPE_FLATFILE("eced6f20-df52-11df-9e9a-0002a5d5c51b"),
    /**
     * Localhost DatabaseTable (ResourceType)
     */
    RESOURCETYPE_LOCALHOST_DATABASETABLE("aae7be60-df56-11df-8608-0002a5d5c51b"),
    /**
     * OpenDJ jbond (AccountShadowType)
     */
    ACCOUNTSHADOWTYPE_OPENDJ_JBOND("dbb0c37d-9ee6-44a4-8d39-016dbce18b4c"),
    /**
     * OpenDJ unsaved jdoe (AccountShadowType)
     */
    ACCOUNTSHADOWTYPE_OPENDJ_JDOE("e341e691-0b89-4245-9ec8-c20f63b69714");

    private String OID;

    private SampleObjects(String _key) {
        this.OID = _key;
    }

    public String getOID() {
        return OID;

    }

    public static SampleObjects findByOID(String value) {
        for (SampleObjects f : SampleObjects.values()) {
            if (f.getOID().equals(value)) {
                return f;
            }
        }
        return null;
    }
}
