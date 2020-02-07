/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
