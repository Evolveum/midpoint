/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.statistics.CachePerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.schema.cache.CachePerformanceCollector;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.wicket.model.IModel;

import java.util.Map;

/**
 *
 */
public class InternalsPerformancePanel extends BasePanel<Void> {
    private static final long serialVersionUID = 1L;

    private static final String ID_INFORMATION = "information";

    InternalsPerformancePanel(String id) {
        super(id);
        initLayout();
    }

    private void initLayout() {
        AceEditor informationText = new AceEditor(ID_INFORMATION, new IModel<String>() {
            @Override
            public String getObject() {
                return getStatistics();
            }

            @Override
            public void setObject(String object) {
                // nothing to do here
            }
        });
        informationText.setReadonly(true);
        informationText.setHeight(300);
        informationText.setResizeToMaxHeight(true);
        informationText.setMode(null);
        add(informationText);

    }

    @SuppressWarnings("Duplicates")
    private String getStatistics() {
        StringBuilder sb = new StringBuilder();
        MidPointApplication midPointApplication = MidPointApplication.get();
        if (midPointApplication != null) {
            PerformanceInformation performanceInformation = midPointApplication.getSqlPerformanceMonitorsCollection()
                    .getGlobalPerformanceInformation();
            sb.append("SQL performance information (repository, audit)\n\n")
                    .append(RepositoryPerformanceInformationUtil.format(performanceInformation.toRepositoryPerformanceInformationType()))
                    .append("\n");
        }
        Map<String, CachePerformanceCollector.CacheData> cache = CachePerformanceCollector.INSTANCE
                .getGlobalPerformanceMap();
        if (cache != null) {
            sb.append("Cache performance information\n\n")
                    .append(CachePerformanceInformationUtil.format(CachePerformanceInformationUtil.toCachesPerformanceInformationType(cache)))
                    .append("\n");
            sb.append("Cache performance information (extra - experimental):\n")
                    .append(CachePerformanceInformationUtil.formatExtra(cache))
                    .append("\n");
        } else {
            sb.append("Cache performance information is currently not available."
                    + "Please set up cache monitoring in the system configuration.\n\n");
        }
        OperationsPerformanceInformation methods = OperationsPerformanceMonitor.INSTANCE
                .getGlobalPerformanceInformation();
        if (methods != null) {
            sb.append("Methods performance information\n\n")
                    .append(OperationsPerformanceInformationUtil.format(OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(methods)))
                    .append("\n");
        } else {
            sb.append("Methods performance information is currently not available."
                    + "Please set up performance monitoring in the system configuration.\n\n");
        }
        return sb.toString();
    }
}
