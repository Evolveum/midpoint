/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeConstants;

import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

public class UpgradePreCheckStep implements UpgradeStep<UpgradePreCheckResult> {

    private UpgradeStepsContext context;

    public UpgradePreCheckStep(@NotNull UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public String getIdentifier() {
        return "versionCheck";
    }

    @Override
    public String getPresentableName() {
        return "version check";
    }

    // todo check database schema version + audit db schema version
    @Override
    public UpgradePreCheckResult execute() throws Exception {
        final Log log = context.getContext().getLog();
        final RepositoryService repository = context.getContext().getRepository();

        if (!repository.isNative()) {
            // todo error, this midpoint installation doesn't run on top of native repository
        }

        OperationResult result = new OperationResult("Search nodes");
        try {
            SearchResultList<PrismObject<NodeType>> nodes = repository.searchObjects(NodeType.class, null, null, result);
            Set<String> versions = new HashSet<>();

            nodes.forEach(o -> {
                NodeType node = o.asObjectable();
                if (node.getBuild() == null) {
                    return;
                }

                versions.add(node.getBuild().getVersion());
            });

            log.info(Ansi.ansi().fgBlue().a("Node versions in cluster: ").reset().a(Arrays.toString(versions.toArray())).toString());

            if (versions.isEmpty()) {
                // todo error, couldn't obtain version. Ask whether to continue?

            } else if (versions.size() > 1) {
                // todo error, cluster contains nodes with multiple versions? Ask whether to continue?

            }

            String version = versions.iterator().next();
            boolean match = Arrays.stream(UpgradeConstants.SUPPORTED_VERSIONS).anyMatch(v -> v.equals(version));
            if (!match) {
                // todo error, version didn't match. Ask whether to continue?

            }

            // todo add database version check, schema version check
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String version = "";
        // todo implement midPoint version check, also implement midpoint version support in midpoint sqale db
        return new UpgradePreCheckResult()
                .currentVersion(version);
    }
}
