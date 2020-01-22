/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Useful base class for creating change processors. Currently this class deals only with keeping the processor
 * configuration and context; everything else has been moved to helpers in order to make the code relatively clean.
 *
 * @author mederly
 */
public abstract class BaseChangeProcessor implements ChangeProcessor, BeanNameAware, BeanFactoryAware {

    private static final Trace LOGGER = TraceManager.getTrace(BaseChangeProcessor.class);

    private String beanName;
    private BeanFactory beanFactory;

    @Autowired protected MiscHelper miscHelper;
    @Autowired protected PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;

    public String getBeanName() {
        return beanName;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public MiscHelper getMiscHelper() {
        return miscHelper;
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public RelationRegistry getRelationRegistry() {
        return relationRegistry;
    }


}
