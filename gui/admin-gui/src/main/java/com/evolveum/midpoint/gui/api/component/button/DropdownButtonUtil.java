/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class containing methods used for creation of DropdownButton components.
 *
 * @author matisovaa
 */
public final class DropdownButtonUtil {
    public static DropdownButtonPanel createDownloadButtonPanel(
            String buttonId,
            ContainerableListPanel containerableListPanel,
            String fileNamePrefix) {
        DropdownButtonDto model = new DropdownButtonDto(
                null,
                "fa fa-download",
                null,
                DropdownButtonUtil.createDownloadFormatMenu(containerableListPanel, fileNamePrefix)
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
        downloadFormatMenu.setRenderBodyOnly(true);
        downloadFormatMenu.add(
                new VisibleBehaviour(() ->
                        !WebComponentUtil.hasPopupableParent(containerableListPanel)
                                && (isAuthorizedCsv() || isAuthorizedXlsx()))
        );
        return downloadFormatMenu;
    }

    private static List<InlineMenuItem> createDownloadFormatMenu(
            ContainerableListPanel containerableListPanel,
            String fileNamePrefix) {
        List<InlineMenuItem> items = new ArrayList<>();

        if (isAuthorizedCsv()) {
            items.add(new CsvDownloadInlineMenuItem(containerableListPanel, fileNamePrefix));
        }

        if (isAuthorizedXlsx()) {
            items.add(new XlsxDownloadInlineMenuItem(containerableListPanel, fileNamePrefix));
        }
        return items;
    }

    private static boolean isAuthorizedCsv() {
        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI);
    }

    private static boolean isAuthorizedXlsx() {
        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_XLSX_EXPORT_ACTION_URI);
    }
}
