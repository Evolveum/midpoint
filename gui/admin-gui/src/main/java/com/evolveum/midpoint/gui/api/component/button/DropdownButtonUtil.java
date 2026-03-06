/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class containing methods used for creation of DropdownButton components.
 *
 * @author matisovaa
 */
public final class DropdownButtonUtil {
    public static DropdownButtonPanel createDownloadButtonPanel(String buttonId, ContainerableListPanel containerableListPanel) {
        DropdownButtonDto model = new DropdownButtonDto(
                null,
                "fa fa-download",
                null,
                DropdownButtonUtil.createDownloadFormatMenu(containerableListPanel)
        );
        DropdownButtonPanel downloadFormatMenu = new DropdownButtonPanel(buttonId, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-default btn-sm";
            }

            @Override
            protected String getSpecialDropdownMenuClass() {
                return "dropdown-menu-left";
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
                getButtonContainer().add(AttributeModifier.append("aria-label", "AssignmentTablePanel.operationMenu"));
            }
        };
        downloadFormatMenu.setVisible(
                WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI) ||
                        WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_XLSX_EXPORT_ACTION_URI)
        );
        return downloadFormatMenu;
    }

    public static List<InlineMenuItem> createDownloadFormatMenu(ContainerableListPanel containerableListPanel) {
        List<InlineMenuItem> items = new ArrayList<>();

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI)) {
            items.add(new CsvDownloadInlineMenuItem(
                    ColumnUtils.createStringResource("CsvDownloadButtonPanel.export"),
                    containerableListPanel.getPageBase()
            ) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected String getFilename() {
                    return "AuditLogViewer_" + ColumnUtils.createStringResource("MainObjectListPanel.exportFileName").getString();
                }

                @Override
                protected DataTable<?, ?> getDataTable() {
                    return containerableListPanel.getTable().getDataTable();
                }
            });
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_XLSX_EXPORT_ACTION_URI)) {
            items.add(new XlsxDownloadInlineMenuItem(
                    ColumnUtils.createStringResource("XlsxDownloadButtonPanel.export"),
                    containerableListPanel.getPageBase()
            ) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected String getFilename() {
                    return "AuditLogViewer_" + ColumnUtils.createStringResource("MainObjectListPanel.exportFileName").getString();
                }

                @Override
                protected DataTable<?, ?> getDataTable() {
                    return containerableListPanel.getTable().getDataTable();
                }
            });
        }
        return items;
    }
}
