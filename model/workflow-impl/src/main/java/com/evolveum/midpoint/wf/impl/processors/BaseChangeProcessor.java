package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import org.apache.commons.configuration.Configuration;
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

    private Configuration processorConfiguration;

    private String beanName;
    private BeanFactory beanFactory;

    @Autowired
    private MiscDataUtil miscDataUtil;

	@Autowired
	private PrismContext prismContext;

	private boolean enabled = false;

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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Configuration getProcessorConfiguration() {
        return processorConfiguration;
    }

    protected void setProcessorConfiguration(Configuration c) {
        processorConfiguration = c;
    }

	@Override
    public MiscDataUtil getMiscDataUtil() {
		return miscDataUtil;
	}

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }
}
