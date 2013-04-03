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
 */

package com.evolveum.midpoint.util;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.evolveum.midpoint.util.QNameUtil;
import javax.xml.namespace.QName;

/**
 *
 * @author semancik
 */
public class QNameUtilTest {

    public QNameUtilTest() {
    }

    @Test
    public void uriToQName1() {
        // Given

        String uri="http://foo.com/bar#baz";

        // When

        QName qname = QNameUtil.uriToQName(uri);

        // Then

        AssertJUnit.assertEquals(new QName("http://foo.com/bar","baz"), qname);
    }

    @Test
    public void uriToQName2() {
        // Given

        String uri="http://foo.com/bar/baz";

        // When

        QName qname = QNameUtil.uriToQName(uri);

        // Then

        AssertJUnit.assertEquals(new QName("http://foo.com/bar/","baz"), qname);
    }

    @Test
    public void qNameToUri1() {
        // Given

        QName qname = new QName("http://foo.com/bar","baz");

        // When

        String uri = QNameUtil.qNameToUri(qname);

        // Then

        AssertJUnit.assertEquals("http://foo.com/bar#baz", uri);

    }

    @Test
    public void qNameToUri2() {
        // Given

        QName qname = new QName("http://foo.com/bar/","baz");

        // When

        String uri = QNameUtil.qNameToUri(qname);

        // Then

        AssertJUnit.assertEquals("http://foo.com/bar/baz", uri);

    }

    @Test
    public void qNameToUri3() {
        // Given

        QName qname = new QName("http://foo.com/bar#","baz");

        // When

        String uri = QNameUtil.qNameToUri(qname);

        // Then

        AssertJUnit.assertEquals("http://foo.com/bar#baz", uri);

    }

}