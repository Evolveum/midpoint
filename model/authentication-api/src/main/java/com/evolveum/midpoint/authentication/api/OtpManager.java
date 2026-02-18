/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;

public interface OtpManager {

    /**
     * Check if OTP authentication module is available for the focus object. This method can be used to determine whether
     * OTP credential can be created for the focus object and whether OTP authentication can be used for the focus object.
     *
     * @param focus the focus object for which the credential is being created.
     * It is expected that the focus object is already persisted and has OID.
     * @param task task object for processing
     * @param result operation result for processing
     * @return true if OTP authentication module is available for the focus object, false otherwise.
     */
    <F extends FocusType> boolean isOtpAvailable(PrismObject<F> focus, Task task, OperationResult result);

    /**
     * Create new OTP credential for the focus object. The credential is not persisted,
     * it needs to be saved by the caller. The secret in the credential is not encrypted, it is
     * caller's responsibility to encrypt it before saving.
     *
     * @param focus the focus object for which the credential is being created.
     * It is expected that the focus object is already persisted and has OID.
     * @param task task object for processing
     * @param result operation result for processing
     * @return new OTP credential with generated secret. The credential is not persisted, it needs to be saved by the caller.
     */
    <F extends FocusType> OtpCredentialType createOtpCredential(PrismObject<F> focus, Task task, OperationResult result);

    /**
     * Create OTP auth URL for the given credential. The URL can be used to generate QR code that can
     * be scanned by authenticator app.
     *
     * @param focus the focus object for which auth url is being created.
     * It is expected that the focus object is already persisted and has OID.
     * @param credential the OTP credential for which auth url is being created.
     * It is expected that the credential has secret generated.
     * @param task task object for processing
     * @param result operation result for processing
     * @return true if the code is correct, false otherwise.
     */
    <F extends FocusType> String createOtpAuthUrl(
            PrismObject<F> focus, OtpCredentialType credential, Task task, OperationResult result);

    /**
     * Verify the provided OTP code against the secret in the credential.
     * If the code is correct, the credential is marked as verified.
     *
     * @param focus the focus object for which the code is being verified.
     * It is expected that the focus object is already persisted and has OID.
     * @param credential the OTP credential against which the code is being verified.
     * @param code the OTP code to verify
     * @param task task object for processing
     * @param result operation result for processing
     * @return true if the code is correct, false otherwise.
     */
    <F extends FocusType> boolean verifyOtpCredential(
            PrismObject<F> focus, OtpCredentialType credential, int code, Task task, OperationResult result);
}
