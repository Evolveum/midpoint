/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;

/**
 * @author skublik
 */

public class MyCredentialsDto implements Serializable {

    public static final String F_MY_PASSOWRDS_DTO = "myPasswordsDto";
    public static final String F_PASSWORD_QUESTIONS_DTO = "passwordQuestionsDto";

    private MyPasswordsDto myPasswordsDto;
    private PasswordQuestionsDto passwordQuestionsDto;

    public MyPasswordsDto getMyPasswordsDto() {
        return myPasswordsDto;
    }

    public void setMyPasswordsDto(MyPasswordsDto myPasswordsDto) {
        this.myPasswordsDto = myPasswordsDto;
    }

    public PasswordQuestionsDto getPasswordQuestionsDto() {
        return passwordQuestionsDto;
    }

    public void setPasswordQuestionsDto(PasswordQuestionsDto passwordQuestionsDto) {
        this.passwordQuestionsDto = passwordQuestionsDto;
    }
}
