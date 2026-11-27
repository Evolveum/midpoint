/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LimitationsType;

/** Tests external password dictionaries stored in files inside midpoint.home. */
public class TestFilePasswordDictionary {

    private Path home;
    private FilePasswordDictionary fileDictionary;
    private PasswordDictionaryService service;

    @BeforeMethod
    public void setUp() throws IOException {
        home = Files.createTempDirectory("midpoint-home-test");
        fileDictionary = new FilePasswordDictionary(home);
        service = new PasswordDictionaryService(List.of(new BundledPasswordDictionary(), fileDictionary));
    }

    private void writeFile(String name, String content) throws IOException {
        Path file = home.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static LimitationsType limitations(String... files) {
        LimitationsType limitations = new LimitationsType().checkAgainstDictionary(true);
        for (String file : files) {
            limitations.dictionaryFile(file);
        }
        return limitations;
    }

    @Test
    public void testWordFromFileIsNotValid() throws IOException {
        writeFile("dictionaries/words.txt", "firmaheslo\nsecondword\n");
        var limitations = limitations("dictionaries/words.txt");

        assertFalse(service.isValid("firmaheslo", limitations));
        assertFalse(service.isValid("SecondWord", limitations));
        assertTrue(service.isValid("Zk7mQx2pLw", limitations));
    }

    @Test
    public void testCommentsAndEmptyLinesAreIgnored() throws IOException {
        writeFile("words.txt", "# comment\n\n   \nfirmaheslo\n");
        var limitations = limitations("words.txt");

        assertFalse(service.isValid("firmaheslo", limitations));
        assertTrue(service.isValid("# comment", limitations));
        assertTrue(service.isValid("", limitations));
    }

    @Test
    public void testExternalDictionaryReplacesBuiltInOne() throws IOException {
        writeFile("words.txt", "firmaheslo\n");

        assertFalse(service.isValid("Password1", limitations()));
        assertTrue("built-in dictionary must not be used when a file is configured",
                service.isValid("Password1", limitations("words.txt")));
    }

    @Test
    public void testSeveralFilesAreCombined() throws IOException {
        writeFile("sk.txt", "heslo123\n");
        writeFile("de.txt", "passwort123\n");
        var limitations = limitations("sk.txt", "de.txt");

        assertFalse(service.isValid("heslo123", limitations));
        assertFalse(service.isValid("passwort123", limitations));
        assertTrue(service.isValid("Zk7mQx2pLw", limitations));
    }

    @Test
    public void testMissingFileIsAnError() {
        assertFails(() -> service.isValid("anything", limitations("missing.txt")), "not found");
    }

    @Test
    public void testFileOutsideMidpointHomeIsAnError() throws IOException {
        Path outside = Files.createTempFile("outside-home", ".txt");
        Files.writeString(outside, "secretword\n");

        assertFails(() -> service.isValid("secretword", limitations("../" + outside.getFileName())), "outside");
        assertFails(() -> service.isValid("secretword", limitations(outside.toString())), "relative");
    }

    @Test
    public void testSymbolicLinkLeadingOutsideMidpointHomeIsAnError() throws IOException {
        Path outside = Files.createTempFile("outside-home", ".txt");
        Files.writeString(outside, "secretword\n");
        Files.createSymbolicLink(home.resolve("link.txt"), outside);

        assertFails(() -> service.isValid("secretword", limitations("link.txt")), "outside");
    }

    @Test
    public void testFileIsReadOnlyOnce() throws IOException {
        writeFile("words.txt", "firmaheslo\n");
        var limitations = limitations("words.txt");
        assertFalse(service.isValid("firmaheslo", limitations));

        writeFile("words.txt", "otherword\n");

        assertFalse("changes are applied after a restart only", service.isValid("firmaheslo", limitations));
        assertTrue(service.isValid("otherword", limitations));
    }

    private static void assertFails(Runnable action, String expectedMessagePart) {
        try {
            action.run();
            fail("Expected an exception");
        } catch (SystemException e) {
            assertTrue("Unexpected message: " + e.getMessage(), e.getMessage().contains(expectedMessagePart));
        }
    }
}
