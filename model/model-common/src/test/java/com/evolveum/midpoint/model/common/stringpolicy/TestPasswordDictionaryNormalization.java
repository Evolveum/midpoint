/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/** Tests the normalization of values compared with password dictionaries. */
public class TestPasswordDictionaryNormalization {

    private static final LimitationsType ENABLED = new LimitationsType().checkAgainstDictionary(true);

    private final PasswordDictionaryService service =
            new PasswordDictionaryService(List.of(new BundledPasswordDictionary()));

    @Test
    public void testCaseIsIgnored() {
        assertEquals("password1", PasswordDictionaryFormat.normalize("PassWord1"));
    }

    @Test
    public void testLeadingAndTrailingWhiteSpaceIsRemoved() {
        assertEquals("password", PasswordDictionaryFormat.normalize("  password \t\n"));
    }

    @Test
    public void testInnerWhiteSpaceIsReducedToOneSpace() {
        assertEquals("correct horse battery", PasswordDictionaryFormat.normalize("correct   horse\t\tbattery"));
    }

    @Test
    public void testUnicodeWhiteSpaceIsTreatedAsWhiteSpace() {
        assertEquals("pass word", PasswordDictionaryFormat.normalize(" pass 　word "));
    }

    @Test
    public void testFullWidthLettersBecomeOrdinaryLetters() {
        assertEquals("password", PasswordDictionaryFormat.normalize("ｐａｓｓｗｏｒｄ"));
    }

    @Test
    public void testLigaturesAndSuperscriptsAreExpanded() {
        assertEquals("fish", PasswordDictionaryFormat.normalize("ﬁsh"));
        assertEquals("x2", PasswordDictionaryFormat.normalize("x²"));
    }

    @Test
    public void testComposedAndDecomposedLettersAreEqual() {
        assertEquals(PasswordDictionaryFormat.normalize("héslo"), PasswordDictionaryFormat.normalize("héslo"));
    }

    @Test
    public void testDiacriticsAreKept() {
        assertFalse(PasswordDictionaryFormat.normalize("héslo").equals(PasswordDictionaryFormat.normalize("heslo")));
    }

    @Test
    public void testNormalizationIsIdempotent() {
        String once = PasswordDictionaryFormat.normalize("  Ｐass  WORD  ");
        assertEquals(once, PasswordDictionaryFormat.normalize(once));
    }

    @Test
    public void testBlankValueBecomesEmpty() {
        assertEquals("", PasswordDictionaryFormat.normalize(" \t  "));
    }

    @Test
    public void testBuiltInDictionaryUsesNormalization() {
        assertFalse(service.isValid("  PASSWORD  ", ENABLED));
        assertFalse(service.isValid("ｐａｓｓｗｏｒｄ", ENABLED));
        assertTrue(service.isValid("   ", ENABLED));
    }

    @Test
    public void testWordsOfDictionaryAreNormalized() throws IOException {
        String content = "# comment\n  Second    WORD  \nｆｉｒｍａ\n\n   \n";
        Set<String> words = PasswordDictionaryFormat.read(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        assertEquals(Set.of("second word", "firma"), words);
    }

    @Test
    public void testServiceUsesNormalization() {
        PasswordDictionary company = new FixedPasswordDictionary("company pass");
        var combined = new PasswordDictionaryService(List.of(new BundledPasswordDictionary(), company));

        assertFalse(combined.isValid("  COMPANY\t PASS ", ENABLED));
        assertTrue(combined.isValid("Zk7mQx2pLw", ENABLED));
    }
}
