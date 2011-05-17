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

package com.evolveum.midpoint.test.ldap;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author elek
 */
public class OpenDJUnitTestAdapterTest extends OpenDJUnitTestAdapter {

    public OpenDJUnitTestAdapterTest() {
    }

    @BeforeClass
    public static void init() throws Exception {
        //dbTemplateDir = "src/main/resources/test-data/opendj.template";
        startACleanDJ();
    }

    @AfterClass
    public static void stop() throws Exception {
        stopDJ();
    }

    @Test
    public void testSomeMethod() {
        //comment it out if you would like to connect to the ldap
        //please attention that it's only a copy of the template so if you would like
        //to modify the ldap you should copy the resource back to src/main/resource/...
//       try {
//            Thread.sleep(100000000);
//        } catch (InterruptedException ex) {
//            System.out.println("OpenDJ server will be stopped. ");
//        }

    }
}
