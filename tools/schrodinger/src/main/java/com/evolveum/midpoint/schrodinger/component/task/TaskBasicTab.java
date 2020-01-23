/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.task.EditTaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 3/21/2018.
 */
public class TaskBasicTab extends Component<EditTaskPage> {
    public TaskBasicTab(EditTaskPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String utility() {
        return getParentElement().$(Schrodinger.byDataId("category")).getText();
    }
}
