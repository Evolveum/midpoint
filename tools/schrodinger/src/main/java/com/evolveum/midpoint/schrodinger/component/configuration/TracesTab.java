/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.configuration.InternalsConfigurationPage;

import static com.evolveum.midpoint.schrodinger.util.Utils.setCheckFormGroupOptionCheckedByTitleResourceKey;
import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionCheckedById;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TracesTab extends Component<InternalsConfigurationPage> {

    public TracesTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TracesTab selectResourceSchemaOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.resourceSchemaOperations", true);
        return this;
    }

    public TracesTab deselectResourceSchemaOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.resourceSchemaOperations", false);
        return this;
    }

    public TracesTab selectConnectorOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.connectorOperations", true);
        return this;
    }

    public TracesTab deselectConnectorOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.connectorOperations", false);
        return this;
    }

    public TracesTab selectShadowFetchOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.shadowFetchOperations", true);
        return this;
    }

    public TracesTab deselectShadowFetchOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.shadowFetchOperations", false);
        return this;
    }

    public TracesTab selectRepositoryOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.repositoryOperations", true);
        return this;
    }

    public TracesTab deselectRepositoryOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.repositoryOperations", false);
        return this;
    }

    public TracesTab selectRoleEvaluations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.roleEvaluations", true);
        return this;
    }

    public TracesTab deselectRoleEvaluations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.roleEvaluations", false);
        return this;
    }

    public TracesTab selectPrismOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.prismOperations", true);
        return this;
    }

    public TracesTab deselectPrismOperations() {
        setCheckFormGroupOptionCheckedByTitleResourceKey("InternalOperationClasses.prismOperations", false);
        return this;
    }

}

