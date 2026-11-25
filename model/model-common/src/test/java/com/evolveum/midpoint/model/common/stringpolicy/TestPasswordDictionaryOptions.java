/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.testng.annotations.Test;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DictionaryCheckType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/** Tests the options of the dictionary check: forms of the value that are compared with the words. */
public class TestPasswordDictionaryOptions {

    private static final PasswordDictionary WORDS =
            new FixedPasswordDictionary("password", "horse", "abc", "alice", "mix");

    private final PasswordDictionaryService service = new PasswordDictionaryService(List.of(WORDS));

    private static LimitationsType limitations(Consumer<DictionaryCheckType> options) {
        DictionaryCheckType check = new DictionaryCheckType();
        options.accept(check);
        return new LimitationsType().checkAgainstDictionary(true).dictionaryCheck(check);
    }

    private static LimitationsType noOptions() {
        return new LimitationsType().checkAgainstDictionary(true);
    }

    @Test
    public void testOptionsAreOffByDefault() {
        for (String value : List.of("drowssap", "42password!", "fooPassword", "pa55w0rd", "my horse is")) {
            assertTrue(value, service.isValid(value, noOptions()));
            assertTrue(value, service.isValid(value, limitations(o -> { })));
        }
        assertFalse(service.isValid("Password", noOptions()));
    }

    @Test
    public void testReversedValue() {
        var limitations = limitations(o -> o.reversed(true));

        assertFalse(service.isValid("drowssap", limitations));
        assertFalse(service.isValid("DROWSSAP", limitations));
        assertTrue(service.isValid("drowssapx", limitations));
    }

    @Test
    public void testNonAlphaCharactersAreStripped() {
        var limitations = limitations(o -> o.stripNonAlpha(true));

        assertFalse(service.isValid("42password!", limitations));
        assertFalse(service.isValid("p-a-s-s-w-o-r-d", limitations));
        assertTrue(service.isValid("42passw0rd!", limitations));
    }

    @Test
    public void testSubstring() {
        var limitations = limitations(o -> o.substring(true));

        assertFalse(service.isValid("fooPassword", limitations));
        assertFalse(service.isValid("passwordbar", limitations));
        assertTrue(service.isValid("fooPasswrd", limitations));
    }

    @Test
    public void testSubstringIgnoresShortWordsByDefault() {
        var limitations = limitations(o -> o.substring(true));

        assertTrue("abc is shorter than 4 characters", service.isValid("xxabcxx", limitations));
        assertFalse("horse is long enough", service.isValid("xxhorsexx", limitations));
    }

    @Test
    public void testMinWordLengthForSubstring() {
        assertFalse(service.isValid("xxabcxx", limitations(o -> o.substring(true).minWordLength(3))));
        assertTrue("password has 8 characters", service.isValid("fooPassword", limitations(o -> o.substring(true).minWordLength(9))));
    }

    @Test
    public void testMinWordLengthForExactComparison() {
        assertFalse(service.isValid("abc", noOptions()));
        assertTrue(service.isValid("abc", limitations(o -> o.minWordLength(4))));
        assertFalse(service.isValid("horse", limitations(o -> o.minWordLength(4))));
    }

    @Test
    public void testLeetspeak() {
        var limitations = limitations(o -> o.leetspeak(true));

        assertFalse(service.isValid("pa55w0rd", limitations));
        assertFalse(service.isValid("p@$$w0rd", limitations));
        assertFalse("1 as l", service.isValid("a1ice", limitations));
        assertFalse("1 as i", service.isValid("m1x", limitations));
        assertTrue(service.isValid("pa55w1rd", limitations));
    }

    @Test
    public void testOptionsCanBeCombined() {
        var leetspeakOnly = limitations(o -> o.leetspeak(true));
        var combined = limitations(o -> o.leetspeak(true).reversed(true).stripNonAlpha(true));

        assertTrue(service.isValid("dr0wss4p", leetspeakOnly));
        assertFalse(service.isValid("dr0wss4p", combined));
        assertFalse(service.isValid("42dr0wss4p!", combined));
    }

    @Test
    public void testLeetspeakWithStrippedEdges() {
        var limitations = limitations(o -> o.leetspeak(true).stripNonAlpha(true));

        assertFalse(service.isValid("42p4ssw0rd!", limitations));
        assertTrue("without stripping the leading 4 becomes a letter", service.isValid("42p4ssw0rd!", limitations(o -> o.leetspeak(true))));
    }

    @Test
    public void testEachWord() {
        String passphrase = "correct horse battery staple";

        assertTrue("whole pass phrase is not in the dictionary", service.isValid(passphrase, noOptions()));
        assertFalse(service.isValid(passphrase, limitations(o -> o.eachWord(true))));
        assertTrue(service.isValid("correct battery staple", limitations(o -> o.eachWord(true))));
    }

    @Test
    public void testEachWordUsesOtherOptions() {
        var limitations = limitations(o -> o.eachWord(true).reversed(true));

        assertFalse(service.isValid("correct esroh battery", limitations));
    }

    @Test
    public void testOptionsWorkWithBuiltInDictionary() {
        var builtIn = new PasswordDictionaryService(List.of(new BundledPasswordDictionary()));

        assertTrue(builtIn.isValid("fooPassword", noOptions()));
        assertFalse(builtIn.isValid("fooPassword", limitations(o -> o.substring(true))));
        assertTrue(builtIn.isValid("m0nk3y", noOptions()));
        assertFalse(builtIn.isValid("m0nk3y", limitations(o -> o.leetspeak(true))));
        assertTrue(builtIn.isValid("yeknom", noOptions()));
        assertFalse(builtIn.isValid("yeknom", limitations(o -> o.reversed(true))));
    }

    @Test
    public void testCandidates() {
        var options = new DictionaryCheckType().reversed(true).stripNonAlpha(true);

        assertEquals(Set.of("42password!", "password", "!drowssap24", "drowssap"),
                PasswordVariants.candidates("42Password!", options));
    }

    @Test
    public void testCandidatesNeverContainEmptyString() {
        var options = new DictionaryCheckType().stripNonAlpha(true).eachWord(true);

        assertEquals(Set.of("1234"), PasswordVariants.candidates("1234", options));
    }
}
