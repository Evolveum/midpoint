/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.manual;

import java.io.IOException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;

/**
 * @author semancik
 *
 */
public interface BackingStore {

    void initialize() throws IOException;

    void provisionWill(String interest) throws IOException;

    void updateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus, String password) throws IOException;

    void deprovisionWill() throws IOException;

    void addJack() throws IOException;

    void deleteJack() throws IOException;

    void addPhantom()  throws IOException;

    void addPhoenix()  throws IOException;

    void deleteAccount(String username) throws IOException;

    void displayContent() throws IOException;

}
