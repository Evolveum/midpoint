/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.KeyValueTreeNode;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachesStateInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ComponentSizeInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class InternalsCachePanel extends BasePanel<Void>{

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(InternalsCachePanel.class);

    private static final String ID_CLEAR_CACHES_BUTTON = "clearCaches";
    private static final String ID_DUMP_CONTENT_BUTTON = "dumpContent";
    private static final String ID_INFORMATION = "information";

    public InternalsCachePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        AceEditor informationText = new AceEditor(ID_INFORMATION, new IModel<String>() {
            @Override
            public String getObject() {
                return getCacheInformation();
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

        add(new AjaxButton(ID_CLEAR_CACHES_BUTTON, createStringResource("InternalsCachePanel.button.clearCaches")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().getCacheDispatcher().dispatchInvalidation(null, null, true, null);
                target.add(InternalsCachePanel.this);
            }
        });

        add(new AjaxButton(ID_DUMP_CONTENT_BUTTON, createStringResource("InternalsCachePanel.button.dumpContent")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String cacheInformation = getCacheInformation();
                LOGGER.info("Dumping the content of the caches.\nCurrent counters:\n{}\n", cacheInformation);
                MidPointApplication.get().getCacheRegistry().dumpContent();

                getSession().success(getPageBase().getString("InternalsCachePanel.result.dumped"));
                target.add(getPageBase());
            }
        });
    }

    private static class SizeInformation {
        private final int size;
        private final int secondarySize;

        private SizeInformation(SingleCacheStateInformationType info) {
            size = defaultIfNull(info.getSize(), 0);
            secondarySize = defaultIfNull(info.getSecondarySize(), 0);
        }

        private SizeInformation(ComponentSizeInformationType info) {
            size = defaultIfNull(info.getSize(), 0);
            secondarySize = defaultIfNull(info.getSecondarySize(), 0);
        }
    }

    private String getCacheInformation() {
        StringBuilder sb = new StringBuilder();
        CachesStateInformationType state = MidPointApplication.get().getCacheRegistry().getStateInformation();
        List<KeyValueTreeNode<String, SizeInformation>> trees = new ArrayList<>();
        for (SingleCacheStateInformationType entry : state.getEntry()) {
            if (isEmptyCacheStateInformationEntry(entry)) {
                continue;
            }
            KeyValueTreeNode<String, SizeInformation> root = new KeyValueTreeNode<>(entry.getName(), new SizeInformation(entry));
            trees.add(root);
            addComponents(root, entry.getComponent());
        }
        Holder<Integer> maxLabelLength = new Holder<>(0);
        Holder<Integer> maxSize = new Holder<>(1); // to avoid issues with log10
        Holder<Integer> maxSecondarySize = new Holder<>(1); // to avoid issues with log10
        trees.forEach(tree -> tree.acceptDepthFirst(node -> {
            int labelLength = node.getUserObject().getKey().length() + node.getDepth() * 2;
            int size = node.getUserObject().getValue().size;
            int secondarySize = node.getUserObject().getValue().secondarySize;
            if (labelLength > maxLabelLength.getValue()) {
                maxLabelLength.setValue(labelLength);
            }
            if (size > maxSize.getValue()) {
                maxSize.setValue(size);
            }
            if (secondarySize > maxSecondarySize.getValue()) {
                maxSecondarySize.setValue(secondarySize);
            }
        }));
        int labelSize = Math.max(maxLabelLength.getValue() + 1, 30);
        int sizeSize = Math.max((int) Math.log10(maxSize.getValue()) + 2, 7);
        int secondarySizeSize = Math.max((int) Math.log10(maxSecondarySize.getValue()) + 2, 8);
        int firstPart = labelSize + 3;
        int secondPart = sizeSize + 2;
        int thirdPart = secondarySizeSize + 3;
        String headerFormatString = "  %-" + labelSize + "s | %" + sizeSize + "s | %" + secondarySizeSize + "s\n";
        String valueFormatString = "  %-" + labelSize + "s | %" + sizeSize + "d | %" + secondarySizeSize + "s\n";
        sb.append("\n");
        sb.append(String.format(headerFormatString, "Cache", "Size", "Sec. size"));
        sb.append(StringUtils.repeat("=", firstPart)).append("+")
                .append(StringUtils.repeat("=", secondPart)).append("+")
                .append(StringUtils.repeat("=", thirdPart)).append("\n");
        trees.forEach(tree -> {
            tree.acceptDepthFirst(node -> printNode(sb, valueFormatString, node));
            sb.append(StringUtils.repeat("-", firstPart)).append("+")
                    .append(StringUtils.repeat("-", secondPart)).append("+")
                    .append(StringUtils.repeat("-", thirdPart)).append("\n");
        });
        return sb.toString();
    }

    private void printNode(StringBuilder sb, String formatString, TreeNode<Pair<String, SizeInformation>> node) {
        Pair<String, SizeInformation> pair = node.getUserObject();
        if (pair != null) {
            int depth = node.getDepth();
            StringBuilder prefix = new StringBuilder();
            if (depth > 0) {
                for (int i = 0; i < depth - 1; i++) {
                    prefix.append("  ");
                }
                prefix.append("- ");
            }
            sb.append(String.format(formatString, prefix + pair.getKey(), pair.getValue().size,
                    emptyIfZero(pair.getValue().secondarySize)));
        }
    }

    private String emptyIfZero(int number) {
        if (number != 0) {
            return String.valueOf(number);
        } else {
            return "";
        }
    }

    private void addComponents(KeyValueTreeNode<String, SizeInformation> node, List<ComponentSizeInformationType> components) {
        for (ComponentSizeInformationType component : components) {
            KeyValueTreeNode<String, SizeInformation> child = node.createChild(component.getName(), new SizeInformation(component));
            addComponents(child, component.getComponent());
        }
    }

    private boolean isEmptyCacheStateInformationEntry(SingleCacheStateInformationType entry) {
        return entry == null || entry.getName() == null && entry.getSize() == null && entry.getSecondarySize() == null;
    }
}
