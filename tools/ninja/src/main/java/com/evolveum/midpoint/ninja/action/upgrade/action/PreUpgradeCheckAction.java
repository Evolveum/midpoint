/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.util.*;

import com.evolveum.midpoint.init.AuditServiceProxy;
import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeConstants;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditService;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.apache.commons.lang3.StringUtils;

public class PreUpgradeCheckAction extends Action<PreUpgradeCheckOptions, ActionResult<Boolean>> {

    public PreUpgradeCheckAction() {
    }

    public PreUpgradeCheckAction(boolean partial) {
        super(partial);
    }

    @Override
    public String getOperationName() {
        return "pre-upgrade checks";
    }

    @Override
    public ActionResult<Boolean> execute() throws Exception {
        final RepositoryService repository = context.getRepository();

        if (!repository.isNative()) {
            log.error("Repository is not using native implementation for PostgreSQL (sqale)");
            return new ActionResult<>(false, 1);
        }

        if (!options.isSkipNodesVersionCheck()) {
            if (!checkNodesVersion(repository)) {
                return new ActionResult<>(false, 2);
            }
        } else {
            log.warn("Skipping nodes version check");
        }

        if (!options.isSkipDatabaseVersionCheck()) {
            if (!checkDatabaseSchemaVersion(repository)) {
                return new ActionResult<>(false, 3);
            }
        } else {
            log.warn("Skipping database schema version check");
        }

        log.info(ConsoleFormat.formatSuccessMessage("Pre-upgrade checks finished successfully"));

        return new ActionResult<>(true);
    }

    private boolean checkDatabaseSchemaVersion(RepositoryService repository) {
        log.info("Checking database schema version");

        RepositoryDiag repositoryInfo = repository.getRepositoryDiag();

        boolean result = validateChangeNumber(
                repositoryInfo.getAdditionalDetails(), SqaleUtils.SCHEMA_CHANGE_NUMBER,
                SqaleUtils.CURRENT_SCHEMA_CHANGE_NUMBER);
        if (!result) {
            return false;
        }

        AuditServiceProxy auditServiceProxy = context.getApplicationContext().getBean(AuditServiceProxy.class);
        SqaleAuditService audit = auditServiceProxy.getImplementation(SqaleAuditService.class);
        if (audit == null) {
            log.info("Audit service is not configured, skipping audit schema version check");
            return true;
        }

        RepositoryDiag auditInfo = audit.getRepositoryDiag();
        return validateChangeNumber(
                auditInfo.getAdditionalDetails(), SqaleUtils.SCHEMA_AUDIT_CHANGE_NUMBER,
                SqaleUtils.CURRENT_SCHEMA_AUDIT_CHANGE_NUMBER);
    }

    private boolean validateChangeNumber(List<LabeledString> list, String label, int expected) {
        String number = getValue(list, label);
        boolean equals = Objects.equals(number, Integer.toString(expected));

        if (!equals) {
            log.error(
                    "Database schema change number ({}) doesn't match supported one ({}) for label {}.",
                    number, expected, label);
        } else {
            log.info("Database schema change number matches supported one ({}) for label {}.", expected, label);
        }

        return equals;
    }

    private String getValue(List<LabeledString> list, String name) {
        if (list == null) {
            return null;
        }

        LabeledString labeled = list.stream().filter(ls -> name.equals(ls.getLabel())).findFirst().orElse(null);
        return labeled != null ? labeled.getData() : null;
    }

    private boolean checkNodesVersion(RepositoryService repository) throws SchemaException {
        log.info("Checking node versions in midPoint cluster");

        OperationResult result = new OperationResult("Search nodes");

        SearchResultList<PrismObject<NodeType>> nodes = repository.searchObjects(NodeType.class, null, null, result);
        Set<String> versions = new HashSet<>();

        nodes.forEach(o -> {
            NodeType node = o.asObjectable();
            if (node.getBuild() == null) {
                return;
            }

            versions.add(node.getBuild().getVersion());
        });

        log.info("Found {} nodes in cluster", nodes.size());
        log.info(
                ConsoleFormat.formatMessageWithInfoParameters(
                        "Nodes version in cluster: {}", StringUtils.join(versions, ", ")));

        if (versions.isEmpty()) {
            log.warn("There are zero nodes in cluster to validate current midPoint version.");

            return true;
        } else if (versions.size() > 1) {
            log.error("There are nodes with different versions of midPoint.");
            log.error("Please remove incorrect nodes from cluster and related Node objects from repository.");
            return false;
        }

        String version = versions.iterator().next();
        if (!Objects.equals(version, UpgradeConstants.SUPPORTED_VERSION)) {
            log.error(
                    "There are midPoint nodes with versions " + Arrays.toString(versions.toArray())
                            + " that doesn't match supported version for upgrade (" + UpgradeConstants.SUPPORTED_VERSION + ")");
            log.error("Make sure that all nodes in midPoint cluster are running same and supported version of midPoint, "
                    + "remove all obsolete Node objects from repository.");
            return false;
        }

        return true;
    }
}
