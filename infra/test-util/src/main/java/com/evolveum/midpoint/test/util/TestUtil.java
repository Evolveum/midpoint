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

import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * Unit test utility.
 * 
 * The utility is able to locate and unmarshall sample XML objects.
 * 
 * It is expected that this will be used by other unit tests to reuse
 * the same XML objects instead of each test creating its own set of
 * testing objects.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class TestUtil {

    public static final String code_id = "$Id$";
    public static final JAXBContext ctx;

    static {
        JAXBContext context = null;
        try {
            context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        } catch (JAXBException ex) {
            ex.printStackTrace();
        }
        ctx = context;
    }

    /**
     * Gets an object from the sample object store and unmarshall it.
     *
     * The sample objects are stored in XML form in /repository/%s.xml files.
     * The objects are referenceable by {@link SampleObjects} enum.
     * 
     * @param object
     * @return the sample object instance or assert null error if the object is not available
     */
    public static ExtensibleObjectType getSampleObject(SampleObjects object) {
        assert null != object;
        String resourceName = String.format("/test-data/repository/%s.xml", object.getOID());

        InputStream in = TestUtil.class.getResourceAsStream(resourceName);
        ExtensibleObjectType out = null;
        if (null != in) {
            try {
                JAXBElement<ExtensibleObjectType> o = (JAXBElement<ExtensibleObjectType>) ctx.createUnmarshaller().unmarshal(in);
                out = o.getValue();
            } catch (JAXBException ex) {
                Logger.getLogger(TestUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        assert null != out;
        return out;
    }
}
