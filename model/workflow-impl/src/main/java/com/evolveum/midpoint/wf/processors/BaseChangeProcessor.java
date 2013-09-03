package com.evolveum.midpoint.wf.processors;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mederly
 */
public abstract class BaseChangeProcessor implements ChangeProcessor, BeanNameAware, BeanFactoryAware {

    private static final Trace LOGGER = TraceManager.getTrace(BaseChangeProcessor.class);

    private static final String KEY_ENABLED = "enabled";
    private static final List<String> KNOWN_KEYS = Arrays.asList(KEY_ENABLED);

    @Autowired
    private WfConfiguration wfConfiguration;

    private Configuration processorConfiguration;

    private String beanName;
    private BeanFactory beanFactory;

    private boolean enabled;

    protected void initializeBaseProcessor() {
        initializeBaseProcessor(null);
    }

    protected void initializeBaseProcessor(List<String> locallyKnownKeys) {

        Validate.notNull(beanName, "Bean name was not set correctly.");

        Configuration c = wfConfiguration.getChangeProcessorsConfig().subset(beanName);
        if (c.isEmpty()) {
            LOGGER.info("Skipping reading configuration of " + beanName + ", as it is not on the list of change processors or is empty.");
        }

        List<String> allKnownKeys = new ArrayList<String>(KNOWN_KEYS);
        if (locallyKnownKeys != null) {
            allKnownKeys.addAll(locallyKnownKeys);
        }
        wfConfiguration.checkAllowedKeys(c, allKnownKeys);

        enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Change processor " + beanName + " is DISABLED.");
        }
        processorConfiguration = c;
    }

    protected Configuration getProcessorConfiguration() {
        return processorConfiguration;
    }

    protected String getBeanName() {
        return beanName;
    }

    protected BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setBeanName(String name) {
        LOGGER.trace("Setting bean name to {}", name);
        this.beanName = name;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }


}
