/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Supports selection from pre-defined samples for {@link PageAuthorizationPlayground} and {@link PageEvaluateMapping}. */
@Experimental
abstract class SamplesChoice extends DropDownChoice<String> {

    private static final Trace LOGGER = TraceManager.getTrace(SamplesChoice.class);

    SamplesChoice(String id, List<String> samples, String keyPrefix) {
        super(id,
                Model.of(""),
                (IModel<List<String>>) () -> samples,
                new StringResourceChoiceRenderer(keyPrefix));

        setNullValid(true);
        add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String sampleName = getModelObject();
                if (StringUtils.isEmpty(sampleName)) {
                    return;
                }
                update(sampleName, target);
            }
        });
    }

    /** Updates the page according to the selected sample. */
    protected abstract void update(String sampleName, AjaxRequestTarget target);

    String readResource(String name) {
        try (InputStream is = SamplesChoice.class.getResourceAsStream(name)) {
            if (is != null) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } else {
                LOGGER.warn("Resource {} containing sample couldn't be found", name);
            }
        } catch (IOException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't read sample from resource {}", e, name);
        }
        return null;
    }
}
