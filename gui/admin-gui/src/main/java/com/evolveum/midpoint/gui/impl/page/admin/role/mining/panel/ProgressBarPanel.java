/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class ProgressBarPanel extends Panel {
    private Label progressLabel;

    AtomicReference<String> progress = new AtomicReference<>("");
    ExecutorService executorService;

    boolean run = true;

    public ProgressBarPanel(String id) {
        super(id);

        Form<Void> form = new Form<>("form");
        add(form);

        progressLabel = new Label("progressLabel", Model.of("Progress: 0%"));
        progressLabel.setOutputMarkupId(true);
        progressLabel.setVisible(true);
        form.add(progressLabel);

        IndicatingAjaxButton button = new IndicatingAjaxButton("button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                progress.set("start");
                run = true;

                execute(target, progress);

                progressLabel.add(new AbstractAjaxTimerBehavior(Duration.ofSeconds(1)) {

                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        progressLabel.setDefaultModelObject("Progress: " + progress.get());
                        target.add(progressLabel);

                        if (!run) {
                            stop(target);
                        }

                    }
                });
                target.add(progressLabel);
            }
        };
        form.add(button);
    }

    public void execute(AjaxRequestTarget target, AtomicReference<String> value) {
//        final Application application = Application.get();
//        final Session session = Session.get();
//
//        executorService = Executors.newFixedThreadPool(1);
//        executorService.execute(() -> {
//            ThreadContext.setApplication(application);
//            ThreadContext.setSession(session);
//

//            run = false;
//        });
//
//        executorService.shutdown();
    }

}
