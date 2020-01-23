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

public class InternalsCachePanel extends BasePanel<Void>{

    private static final long serialVersionUID = 1L;

    private static final String ID_CLEAR_CACHES_BUTTON = "clearCaches";
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

        AjaxButton clearCaches = new AjaxButton(ID_CLEAR_CACHES_BUTTON, createStringResource("InternalsCachePanel.button.clearCaches")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().getCacheDispatcher().dispatchInvalidation(null, null, true, null);
                target.add(InternalsCachePanel.this);
            }
        };

        add(clearCaches);
    }

    private String getCacheInformation() {
        StringBuilder sb = new StringBuilder();
        MidPointApplication midPointApplication = MidPointApplication.get();
        if (midPointApplication != null) {
            CachesStateInformationType state = midPointApplication.getCacheRegistry().getStateInformation();
            List<KeyValueTreeNode<String, Integer>> trees = new ArrayList<>();
            for (SingleCacheStateInformationType entry : state.getEntry()) {
                KeyValueTreeNode<String, Integer> root = new KeyValueTreeNode<>(entry.getName(), entry.getSize());
                trees.add(root);
                addComponents(root, entry.getComponent());
            }
            Holder<Integer> maxLabelLength = new Holder<>(0);
            Holder<Integer> maxSize = new Holder<>(0);
            trees.forEach(tree -> tree.acceptDepthFirst(node -> {
                int labelLength = node.getUserObject().getKey().length() + node.getDepth() * 2;
                Integer size = node.getUserObject().getValue();
                if (labelLength > maxLabelLength.getValue()) {
                    maxLabelLength.setValue(labelLength);
                }
                if (size != null && size > maxSize.getValue()) {
                    maxSize.setValue(size);
                }
            }));
            int labelSize = Math.max(maxLabelLength.getValue() + 1, 30);
            int sizeSize = Math.max((int) Math.log10(maxSize.getValue()) + 2, 7);
            int firstPart = labelSize + 3;
            int secondPart = sizeSize + 3;
            String headerFormatString = "  %-" + labelSize + "s | %" + sizeSize + "s  \n";
            String valueFormatString = "  %-" + labelSize + "s | %" + sizeSize + "d  \n";
            //System.out.println("valueFormatString = " + valueFormatString);
            sb.append("\n");
            sb.append(String.format(headerFormatString, "Cache", "Size"));
            sb.append(StringUtils.repeat("=", firstPart)).append("+").append(StringUtils.repeat("=", secondPart)).append("\n");
            trees.forEach(tree -> {
                tree.acceptDepthFirst(node -> printNode(sb, valueFormatString, node));
                sb.append(StringUtils.repeat("-", firstPart)).append("+").append(StringUtils.repeat("-", secondPart)).append("\n");
            });
        }
        return sb.toString();
    }

    private void printNode(StringBuilder sb, String formatString, TreeNode<Pair<String, Integer>> node) {
        Pair<String, Integer> pair = node.getUserObject();
        if (pair != null) {
            int depth = node.getDepth();
            StringBuilder prefix = new StringBuilder();
            if (depth > 0) {
                for (int i = 0; i < depth - 1; i++) {
                    prefix.append("  ");
                }
                prefix.append("- ");
            }
            sb.append(String.format(formatString, prefix + pair.getKey(), pair.getValue()));
        }
    }

    private void addComponents(KeyValueTreeNode<String, Integer> node, List<ComponentSizeInformationType> components) {
        for (ComponentSizeInformationType component : components) {
            KeyValueTreeNode<String, Integer> child = node.createChild(component.getName(), component.getSize());
            addComponents(child, component.getComponent());
        }
    }
}
