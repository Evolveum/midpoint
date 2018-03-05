/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
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

        AssertJUnit.assertEquals(new QName("http://foo.com/bar","baz"), qname);
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
