/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathParserTemp;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TraceType;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.traces.TraceUtil.asStringList;
import static com.evolveum.midpoint.schema.traces.TraceUtil.selectByKey;

public class TemplateExpander {

    public String expandTemplate(OpNode node, String nameTemplate) {
        return new StringSubstitutor(createResolver(node), "${", "}", '\\')
                .replace(nameTemplate);
    }

    private StringLookup createResolver(OpNode node) {
        return spec -> {
            String prefix;
            String suffix;
            String key;

            String[] parts = spec.split(":");
            if (parts.length == 1) {
                prefix = "";
                key = spec;
                suffix = "";
            } else if (parts.length == 2) {
                prefix = parts[0];
                key = parts[1];
                suffix = "";
            } else if (parts.length == 3) {
                prefix = parts[0];
                key = parts[1];
                suffix = parts[2];
            } else {
                return "???";
            }

            List<String> values = new ArrayList<>();
            if (prefix.isEmpty() || prefix.equals("p")) {
                collectMatchingParams(values, key, node.getResult().getParams());
            }
            if (prefix.isEmpty() || prefix.equals("c")) {
                collectMatchingParams(values, key, node.getResult().getContext());
            }
            if (prefix.isEmpty() || prefix.equals("r")) {
                collectMatchingParams(values, key, node.getResult().getReturns());
            }
            if (prefix.isEmpty() || prefix.equals("t")) {
                collectMatchingValues(values, key, node.getResult().getTrace());
            }
            if (prefix.equals("m")) {
                collectFromMethod(values, key, node);
            }
            return String.join(", ", postprocess(values, suffix));
        };
    }

    private void collectFromMethod(List<String> values, String key, OpNode node) {
        try {
            Object rv = MethodUtils.invokeExactMethod(node, key);
            if (rv instanceof Collection) {
                for (Object o : (Collection) rv) {
                    values.add(String.valueOf(o));
                }
            } else if (rv != null) {
                values.add(String.valueOf(rv));
            }
        } catch (Throwable t) {
            values.add("??? " + t.getMessage());
        }
    }

    private void collectMatchingValues(List<String> values, String path, List<TraceType> traces) {
        UniformItemPath itemPath = ItemPathParserTemp.parseFromString(path); // FIXME (hack)
        for (TraceType trace : traces) {
            PrismProperty property = trace.asPrismContainerValue().findProperty(itemPath);
            if (property != null) {
                for (Object realValue : property.getRealValues()) {
                    values.add(String.valueOf(realValue));
                }
            }
        }
    }

    private void collectMatchingParams(List<String> values, String key, ParamsType params) {
        int colon = key.indexOf(':');
        String processing;
        String realKey;
        if (colon >= 0) {
            realKey = key.substring(0, colon);
            processing = key.substring(colon + 1);
        } else {
            realKey = key;
            processing = "";
        }
        List<String> rawStringValues = asStringList(selectByKey(params, realKey));
        values.addAll(postprocess(rawStringValues, processing));
    }

    private List<String> postprocess(List<String> raw, String processing) {
        return raw.stream()
                .map(s -> postprocess(s, processing))
                .collect(Collectors.toList());
    }

    private String postprocess(String string, String processing) {
        if (processing.contains("L")) {
            string = string.toLowerCase();
        }
        return string;
    }

}
