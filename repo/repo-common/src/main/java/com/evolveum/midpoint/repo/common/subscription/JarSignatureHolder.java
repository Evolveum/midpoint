/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

/**
 * Just to (statically) hold the information about `midpoint.jar` file signature validity.
 *
 * Temporary solution.
 */
public class JarSignatureHolder {

    private static Validity jarSignatureValidity;

    public static void setJarSignatureValidity(Validity validity) {
        jarSignatureValidity = validity;
    }

    public static boolean isJarSignatureValid() {
        return jarSignatureValidity == Validity.VALID;
    }

    public static boolean isOverlayDetected() {
        return jarSignatureValidity == Validity.OVERLAY_DETECTED;
    }

    public enum Validity {

        /** The signature is present and valid. */
        VALID,

        /** The signature is either missing or not valid. */
        INVALID,

        /** There was an error while verifying the signature. */
        ERROR,

        /** The signature checking is not applicable, e.g. because we are not running from a JAR file. */
        NOT_APPLICABLE,

        /** The overlay was detected, so the signature is not checked at all. */
        OVERLAY_DETECTED
    }
}
