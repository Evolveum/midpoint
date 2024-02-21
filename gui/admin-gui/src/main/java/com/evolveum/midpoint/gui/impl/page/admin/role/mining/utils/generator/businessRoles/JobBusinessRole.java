package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles;

import java.util.Random;

// TODO - this class is used for generating rm dataset. Do not forgot remove it.

public enum JobBusinessRole {
    ASSISTANT("47a627c4-fd0f-42e3-a2d0-3ef0cba09ac1"),
    SUPERVISOR("ccc98c8d-0313-4961-9957-929aa0336620"),
    HR_CLERK("ea84c7ca-74d9-43e8-ad2f-535e5f6022ef"),
    SECURITY_OFFICER("084b93f5-2f23-4087-83ec-94f4f8f5183a"),
    SALES("f188128d-5e50-4ea0-9d14-79c10e030cf9"),
    MANAGER("abf214b8-2d0e-4ec8-8000-bc0f4dfa5466");

    private final String stringValue;

    JobBusinessRole(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static JobBusinessRole getRandomJobBusinessRole() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
}
