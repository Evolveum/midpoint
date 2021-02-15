package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Beans useful for non-Spring components within this package.
 */
@Experimental
@Component
class LocalBeans {

    @Autowired AccessChecker accessChecker;
    @Autowired ShadowedObjectConstructionHelper shadowedObjectConstructionHelper;
    @Autowired ShadowAcquisitionHelper shadowAcquisitionHelper;
    @Autowired CommonHelper commonHelper;
    @Autowired ClassificationHelper classificationHelper;
    @Autowired
    ShadowsFacade shadowsFacade;
    @Autowired ShadowCaretaker shadowCaretaker;
    @Autowired ShadowManager shadowManager;

}
