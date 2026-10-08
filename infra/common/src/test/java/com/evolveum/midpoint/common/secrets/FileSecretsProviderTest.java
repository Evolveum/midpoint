/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.common.secrets;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileSecretsProviderType;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class FileSecretsProviderTest {

    private static final Trace LOGGER = TraceManager.getTrace(FileSecretsProviderTest.class);

    private Path tempDir;
    private FileSecretsProvider provider;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("file-secrets-test-");
        LOGGER.debug("tmpdir created:{}",tempDir.getFileName().toString());

        Files.writeString(tempDir.resolve("secret_lf.txt"), "mySecretPassword\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("secret_crlf.txt"), "mySecretPassword\r\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("secret_clean.txt"), "mySecretPassword", StandardCharsets.UTF_8);

        byte[] binaryData = new byte[] { 0x01, 0x02, 0x03, 0x0A };
        Files.write(tempDir.resolve("binary_secret.bin"), binaryData);

        FileSecretsProviderType config = new FileSecretsProviderType();
        config.setParentDirectoryPath(tempDir.toAbsolutePath().toString());


        provider = new FileSecretsProvider(config);
        provider.initialize();

    }
    @AfterMethod
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    public void testResolveSecretStringStripsTrailingNewlines() throws EncryptionException {

        String secretLf = provider.resolveSecret("secret_lf.txt", String.class);
        assertEquals(secretLf, "mySecretPassword", "Trailing \\n must be removed");

        String secretCrlf = provider.resolveSecret("secret_crlf.txt", String.class);
        assertEquals(secretCrlf, "mySecretPassword", "Trailing  \\r\\n must be removed");

        String secretClean = provider.resolveSecret("secret_clean.txt", String.class);
        assertEquals(secretClean, "mySecretPassword", "Raw password without trailing newline must remain unchanged");
    }

    @Test
    public void testResolveSecretByteBufferPreservesTrailingNewlines() throws EncryptionException {
        ByteBuffer buffer = provider.resolveSecret("binary_secret.bin", ByteBuffer.class);

        assertNotNull(buffer, "The ByteBuffer must not be null");
        byte[] resultBytes = new byte[buffer.remaining()];
        buffer.get(resultBytes);

        assertEquals(resultBytes, new byte[] { 0x01, 0x02, 0x03, 0x0A },
                "Binary data in the ByteBuffer must be preserved 1:1, including the trailing 0x0A.");
    }
}
