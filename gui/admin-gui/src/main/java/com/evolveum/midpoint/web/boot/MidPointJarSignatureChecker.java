/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.repo.common.subscription.JarSignatureHolder;
import com.evolveum.midpoint.repo.common.subscription.JarSignatureHolder.Validity;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.system.ApplicationHome;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Checks the signature of `midpoint.jar` file (if applicable).
 *
 * Currently, it uses enclosed certificate file to check the authenticity.
 */
public class MidPointJarSignatureChecker {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointJarSignatureChecker.class);

    public static void setupJarSignature() {
        JarSignatureHolder.setJarSignatureValidity(
                checkJarSignature());
    }

    private static @NotNull Validity checkJarSignature() {
        if (isOverlayDetected()) {
            return Validity.OVERLAY_DETECTED;
        }

        try {
            var home = new ApplicationHome(MidPointSpringApplication.class);
            var source = home.getSource();
            if (!source.isFile() || !source.getName().toLowerCase().endsWith(".jar")) {
                LOGGER.info("Application is not running from a JAR file, skipping JAR signature check: {}", source);
                return Validity.NOT_APPLICABLE;
            }
            try (JarFile jar = new JarFile(source)) {
                return verify(jar);
            }
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't verify JAR file signature", e);
            return Validity.ERROR;
        }
    }

    private static boolean isOverlayDetected() {
        if (MidPointJarSignatureChecker.class.getClassLoader().getResource("overlay-info.txt") != null) {
            LOGGER.info("The overlay-info.txt file was found, skipping JAR signature check");
            return true;
        } else {
            return false;
        }
    }

    private static Validity verify(JarFile jar) throws IOException, CertificateException {

        X509Certificate ourCertificate;
        try (var cert = MidPointJarSignatureChecker.class.getClassLoader().getResourceAsStream("jar-signing.cer")) {
            if (cert == null) {
                LOGGER.info("No jar signing certificate found");
                return Validity.ERROR;
            }
            var certFactory = CertificateFactory.getInstance("X.509");
            ourCertificate = (X509Certificate) certFactory.generateCertificate(cert);
        }

        byte[] scratchBuffer = new byte[8192];
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            LOGGER.trace("Checking JAR entry {}", entry);

            // First, we need to check the signature (~ checksum). This is done simply by reading the entry.
            try (InputStream is = jar.getInputStream(entry)) {
                while (is.read(scratchBuffer, 0, scratchBuffer.length) != -1) {
                    // If there is a signature, the reading will fail if it is not matching.
                }
            } catch (SecurityException e) {
                LOGGER.info("JAR signature verification failed for entry {}", entry, e);
                return Validity.INVALID;
            }

            // The signature is OK. Now let's check the certificate.
            Certificate[] entryCertificates = entry.getCertificates();
            if (entryCertificates == null || entryCertificates.length == 0) {
                if (!entry.getName().startsWith("META-INF/")) {
                    LOGGER.info("Unsigned file in JAR (only those in META-INF are allowed to be unsigned): {}", entry);
                    return Validity.INVALID;
                }
            } else {
                boolean found = false;
                for (Certificate entryCertificate : entryCertificates) {
                    // Very crude way. We ignore the certificate chaining here. We just look for the particular certificate.
                    if (entryCertificate.equals(ourCertificate)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOGGER.info("File without matching certificate in JAR: {}", entry);
                    return Validity.INVALID;
                }
            }
        }
        LOGGER.info("JAR signature verification succeeded for {}", jar.getName());
        return Validity.VALID;
    }
}
