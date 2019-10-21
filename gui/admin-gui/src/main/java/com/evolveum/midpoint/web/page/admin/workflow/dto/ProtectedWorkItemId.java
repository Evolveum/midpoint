/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *  Protected version of work item ID in the form of "id:hash".
 *
 *  It is to be used as part of URL used to display the work item. The hash is computed from selected parts of
 *  the work item so it is impossible to guess.
 */
public class ProtectedWorkItemId {

    @NotNull public final String id;
    @NotNull public final String hash;

    private ProtectedWorkItemId(@NotNull String id, @NotNull String hash) {
        this.id = id;
        this.hash = hash;
    }

    public static ProtectedWorkItemId fromExternalForm(@NotNull String externalForm) {
        int i = externalForm.indexOf(':');
        if (i < 0) {
            throw new IllegalArgumentException("Wrong work item ID format");
        }
        return new ProtectedWorkItemId(externalForm.substring(0, i), externalForm.substring(i+1));
    }

    private static String createWorkItemHash(CaseWorkItemType workItem) {
        try {
            String valueToHash = "";
            //TODO fix!!!
//            workItem.getExternalId() + ":" +
//                    WfContextUtil.getTaskOid(workItem) + ":" +
//                    XmlTypeConverter.toMillis(workItem.getCreateTimestamp());
            byte[] hashBytes = MessageDigest.getInstance("SHA-256").digest(valueToHash.getBytes(StandardCharsets.UTF_8));
            return MiscUtil.binaryToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException("Couldn't compute message digest: " + e.getMessage(), e);
        }
    }

    public static String createExternalForm(CaseWorkItemType workItem) {
        return ""; // TODO fix!!!  workItem.getExternalId() + ":" + createWorkItemHash(workItem);
    }

    public boolean isCorrect(CaseWorkItemType workItem) {
        return hash.equals(createWorkItemHash(workItem));
    }
}
