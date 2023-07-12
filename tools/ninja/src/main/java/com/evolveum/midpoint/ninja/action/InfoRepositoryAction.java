/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Short information about the currently used environment, including repository diagnostics.
 */
public class InfoRepositoryAction extends RepositoryAction<InfoOptions, Void> {

    @Override
    public String getOperationName() {
        return "get repository information";
    }

    @Override
    public Void execute() {
        MidpointConfiguration config = context.getMidpointConfiguration();
        context.out.println("MidPoint home: " + config.getMidpointHome());
        context.out.println("Java home: " + System.getProperty("java.home"));

        RepositoryService repository = context.getRepository();
        OperationResult repoTestResult = new OperationResult("repo.test");
        repository.repositorySelfTest(repoTestResult);
        repoTestResult.close();
        context.out.println("Repository test: " + repoTestResult.getStatus());

        try {
            RepositoryDiag repositoryDiag = repository.getRepositoryDiag();
            context.out.println("Repository diag:"
                    + "\n Type: " + repositoryDiag.getImplementationShortName()
                    + "\n Description: " + repositoryDiag.getImplementationDescription()
                    + "\n JDBC URL: " + repositoryDiag.getRepositoryUrl()
                    + "\n Driver type: " + repositoryDiag.getDriverShortName()
                    + "\n Driver version: " + repositoryDiag.getDriverVersion()
                    + "\n Additional details:");
            for (LabeledString detail : repositoryDiag.getAdditionalDetails()) {
                context.out.println(" - " + detail.getLabel() + ": " + detail.getData());
            }
        } catch (Exception e) {
            // Fatal crash during Ninja start is more likely than this, but just to be sure...
            context.err.println("Unexpected problem during repo diag: " + e);
        }

        return null;
    }
}
