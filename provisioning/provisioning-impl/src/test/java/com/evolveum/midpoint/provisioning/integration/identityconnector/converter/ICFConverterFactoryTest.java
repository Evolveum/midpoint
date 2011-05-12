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

package com.evolveum.midpoint.provisioning.integration.identityconnector.converter;


import com.evolveum.midpoint.provisioning.conversion.Converter;
import com.evolveum.midpoint.provisioning.integration.identityconnector.converter.ICFConverterFactory;
import org.identityconnectors.common.security.GuardedString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;




import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elek
 */
public class ICFConverterFactoryTest {

    public ICFConverterFactoryTest() {
    }

    @Test
    public void convert() {
        assertEquals(new GuardedString("test".toCharArray()),ICFConverterFactory.getInstance().getConverter(GuardedString.class, "test").convert("test"));
    }
}
