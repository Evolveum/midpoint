/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EncodingTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(EncodingTest.class);

    /**
     * Long text with national characters. This is used to test whether the database can store a long text
     * and that it maintains national characters.
     */
    public static String POLICIJA =
            "\u013e\u0161\u010d\u0165\u017e\u00fd\u00e1\u00ed\u00e9\u013d\u0139\u00c1\u00c9\n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. \n" +
                    "P\u014dlic\u012dja, p\u014dlic\u012dja, S\u00e3la\u015d\u00e5ry, pr\u00e0va Jova. Z c\u00e9\u00dfty pr\u00efva, z c\u00e8\u00dfty pr\u00e5va, s\u0177mpatika, korpora. \n" +
                    "Popul\u00e1ry, Karpatula. \u00d0uv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. ";

    private static final String NAME_PREFIX = "selftest";
    private static final int NAME_RANDOM_LENGTH = 5;

    private static final String USER_FULL_NAME = "Grăfula Fèlix Teleke z Tölökö";
    private static final String USER_GIVEN_NAME = "P\u00f3lic\u00edja, p\u00f3lic\u00edja, S\u00e1la\u0161\u00e1ry, pr\u00e1va Jova. Z c\u00e9sty pr\u00edva, z c\u00e9sty pr\u00e1va, s\u00fdmpatika, korpora. Popul\u00e1ry, Karpatula. Juv\u00e1 svorno polic\u00e1na. Kerl\u00e9\u0161 na strach, polic\u00edja. Bumtar\u00e1ra, bumtar\u00e1ra, bum. ";//"Fëľïx";
    private static final String USER_FAMILY_NAME = "ŢæĺêkéčišćeľščťžýáíéäöåøřĺąćęłńóśźżrůāēīūŗļķņģšžčāäǟḑēīļņōȯȱõȭŗšțūžÇĞIİÖŞÜáàâéèêíìîóòôúùûáâãçéêíóôõúÁáĄąÄäÉéĘęĚěÍíÓóÔôÚúŮůÝýČčĎďŤťĽľĹĺŇňŔŕŘřŠšŽž";
    private static final String[] USER_ORGANIZATION = {"COMITATVS NOBILITVS HVNGARIÆ", "Salsa Verde ğomorula prïvatûła"};
    private static final String[] USER_EMPLOYEE_TYPE = {"Ģŗąfųŀą", "CANTATOR"};
    private static final String INSANE_NATIONAL_STRING = "Pørúga ném nå väšȍm apârátula";

    private RandomString randomString;

    public EncodingTest() {
        randomString = new RandomString(NAME_RANDOM_LENGTH, true);
    }


    @Test
    public void repositorySelfTest() throws Exception {
        OperationResult testResult = new OperationResult("self test");
        // Give repository chance to run its own self-test if available
        repositoryService.repositorySelfTest(testResult);

        repositorySelfTestUser(testResult);

        testResult.computeStatus();

        LOGGER.info(testResult.debugDump());
        System.out.println(testResult.debugDump());
        AssertJUnit.assertEquals(OperationResultStatus.SUCCESS, testResult.getStatus());
    }

    private void repositorySelfTestUser(OperationResult testResult) throws SchemaException {
        OperationResult result = testResult.createSubresult("user");

        PrismObject<UserType> user = getObjectDefinition(UserType.class).instantiate();
        UserType userType = user.asObjectable();

        String name = generateRandomName();
        PolyStringType namePolyStringType = toPolyStringType(name);
        userType.setName(namePolyStringType);
        result.addContext("name", name);
        userType.setDescription(POLICIJA);
        userType.setFullName(toPolyStringType(USER_FULL_NAME));
        userType.setGivenName(toPolyStringType(USER_GIVEN_NAME));
        userType.setFamilyName(toPolyStringType(USER_FAMILY_NAME));
        userType.setTitle(toPolyStringType(INSANE_NATIONAL_STRING));
        userType.getEmployeeType().add(USER_EMPLOYEE_TYPE[0]);
        userType.getEmployeeType().add(USER_EMPLOYEE_TYPE[1]);
        userType.getOrganization().add(toPolyStringType(USER_ORGANIZATION[0]));
        userType.getOrganization().add(toPolyStringType(USER_ORGANIZATION[1]));

        String oid;
        try {
            oid = repositoryService.addObject(user, null, result);
        } catch (Exception e) {
            result.recordFatalError(e);
            return;
        }

        try {
            OperationResult subresult = result.createSubresult(result.getOperation() + ".getObject");
            PrismObject<UserType> userRetrieved;
            try {
                userRetrieved = repositoryService.getObject(UserType.class, oid, null, subresult);
            } catch (Exception e) {
                result.recordFatalError(e);
                return;
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Self-test:user getObject:\n{}", userRetrieved.debugDump());
            }
            checkUser(userRetrieved, name, subresult);
            subresult.recordSuccessIfUnknown();

            OperationResult subresult1 = result.createSubresult(result.getOperation() + ".searchObjects.fullName");
            try {
                ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                        .item(UserType.F_FULL_NAME).eq(toPolyString(USER_FULL_NAME)).matchingNorm()
                        .build();
                subresult1.addParam("query", query);
                List<PrismObject<UserType>> foundObjects = repositoryService.searchObjects(UserType.class, query, null, subresult1);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Self-test:user searchObjects:\n{}", DebugUtil.debugDump(foundObjects));
                }
                assertSingleSearchResult("user", foundObjects, subresult1);

                userRetrieved = foundObjects.iterator().next();
                checkUser(userRetrieved, name, subresult1);

                subresult1.recordSuccessIfUnknown();
            } catch (Exception e) {
                subresult1.recordFatalError(e);
                return;
            }
        } finally {
//            try {
//                repositoryService.deleteObject(UserType.class, oid, testResult);
//            } catch (ObjectNotFoundException e) {
//                result.recordFatalError(e);
//                return;
//            } catch (RuntimeException e) {
//                result.recordFatalError(e);
//                return;
//            }

            result.computeStatus();
        }

    }

    private void assertSingleSearchResult(String objectTypeMessage, List<PrismObject<UserType>> foundObjects, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(parentResult.getOperation() + ".numberOfResults");
        assertTrue("Found no " + objectTypeMessage, !foundObjects.isEmpty(), result);
        assertTrue("Expected to find a single " + objectTypeMessage + " but found " + foundObjects.size(), foundObjects.size() == 1, result);
        result.recordSuccessIfUnknown();
    }

    private void checkUser(PrismObject<UserType> userRetrieved, String name, OperationResult subresult) {
        checkUserPropertyPolyString(userRetrieved, UserType.F_NAME, subresult, name);
        checkUserPropertyPolyString(userRetrieved, UserType.F_FULL_NAME, subresult, USER_FULL_NAME);
        checkUserPropertyPolyString(userRetrieved, UserType.F_GIVEN_NAME, subresult, USER_GIVEN_NAME);
        checkUserPropertyPolyString(userRetrieved, UserType.F_FAMILY_NAME, subresult, USER_FAMILY_NAME);
        checkUserPropertyPolyString(userRetrieved, UserType.F_TITLE, subresult, INSANE_NATIONAL_STRING);
        checkUserProperty(userRetrieved, UserType.F_EMPLOYEE_TYPE, subresult, USER_EMPLOYEE_TYPE);
        checkUserPropertyPolyString(userRetrieved, UserType.F_ORGANIZATION, subresult, USER_ORGANIZATION);
        checkUserProperty(userRetrieved, UserType.F_DESCRIPTION, subresult, POLICIJA);
    }

    private <O extends ObjectType, T> void checkUserProperty(PrismObject<O> object, QName propQName, OperationResult parentResult, T... expectedValues) {
        String propName = propQName.getLocalPart();
        OperationResult result = parentResult.createSubresult(parentResult.getOperation() + "." + propName);
        PrismProperty<T> prop = object.findProperty(propQName);
        Collection<T> actualValues = prop.getRealValues();
        result.addArbitraryObjectCollectionAsParam("actualValues", actualValues);
        assertMultivalue("User, property '" + propName + "'", expectedValues, actualValues, result);
        result.recordSuccessIfUnknown();
    }

    private <T> void assertMultivalue(String message, T expectedVals[], Collection<T> actualVals, OperationResult result) {
        if (expectedVals.length != actualVals.size()) {
            fail(message + ": expected " + expectedVals.length + " values but has " + actualVals.size() + " values: " + actualVals, result);
            return;
        }
        for (T expected : expectedVals) {
            boolean found = false;
            for (T actual : actualVals) {
                if (expected.equals(actual)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail(message + ": expected value '" + expected + "' not found in actual values " + actualVals, result);
                return;
            }
        }
    }


    private String polyStringNorm(String orig) {
        return prismContext.getDefaultPolyStringNormalizer().normalize(orig);
    }

    private void assertTrue(String message, boolean condition, OperationResult result) {
        if (!condition) {
            fail(message, result);
        }
    }

    private void assertEquals(String message, Object expected, Object actual, OperationResult result) {
        if (!MiscUtil.equals(expected, actual)) {
            fail(message + "; expected " + expected + ", actual " + actual, result);
        }
    }

    private void fail(String message, OperationResult result) {
        System.out.println(message);
        result.recordFatalError(message);
        LOGGER.error("Repository self-test assertion failed: {}", message);
        AssertJUnit.fail(message);
    }

    private void assertMultivaluePolyString(String message, String expectedOrigs[], Collection<PolyString> actualPolyStrings, OperationResult result) {
        if (expectedOrigs.length != actualPolyStrings.size()) {
            fail(message + ": expected " + expectedOrigs.length + " values but has " + actualPolyStrings.size() + " values: " + actualPolyStrings, result);
            return;
        }
        for (String expectedOrig : expectedOrigs) {
            boolean found = false;
            for (PolyString actualPolyString : actualPolyStrings) {
                if (expectedOrig.equals(actualPolyString.getOrig())) {
                    found = true;
                    assertEquals(message + ", norm", polyStringNorm(expectedOrig), actualPolyString.getNorm(), result);
                    break;
                }
            }
            if (!found) {
                fail(message + ": expected value '" + expectedOrig + "' not found in actual values " + actualPolyStrings, result);
                return;
            }
        }
    }

    private <O extends ObjectType> void checkUserPropertyPolyString(PrismObject<O> object, QName propQName, OperationResult parentResult, String... expectedValues) {
        String propName = propQName.getLocalPart();
        OperationResult result = parentResult.createSubresult(parentResult.getOperation() + "." + propName);
        PrismProperty<PolyString> prop = object.findProperty(propQName);
        Collection<PolyString> actualValues = prop.getRealValues();
        result.addArbitraryObjectCollectionAsParam("actualValues", actualValues);
        assertMultivaluePolyString("User, property '" + propName + "'", expectedValues, actualValues, result);
        result.recordSuccessIfUnknown();
    }

    private PolyString toPolyString(String orig) {
        PolyString polyString = new PolyString(orig);
        polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
        return polyString;
    }

    private String generateRandomName() {
        return NAME_PREFIX + randomString.nextString();
    }

    private PolyStringType toPolyStringType(String orig) {
        PolyStringType polyStringType = new PolyStringType();
        polyStringType.setOrig(orig);
        return polyStringType;
    }

    private <T extends ObjectType> PrismObjectDefinition<T> getObjectDefinition(Class<T> type) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    }
}
