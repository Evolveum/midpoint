package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTable<T> extends Table<T> {

    public UsersTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
