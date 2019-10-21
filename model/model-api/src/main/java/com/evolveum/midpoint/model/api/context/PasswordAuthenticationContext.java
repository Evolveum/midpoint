/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

public class PasswordAuthenticationContext extends AbstractAuthenticationContext {

    private String password;

    public String getPassword() {
        return password;
    }

    public PasswordAuthenticationContext(String username, String password) {
        super(username);
        this.password = password;
    }

    @Override
    public Object getEnteredCredential() {
        return getPassword();
    }

}
