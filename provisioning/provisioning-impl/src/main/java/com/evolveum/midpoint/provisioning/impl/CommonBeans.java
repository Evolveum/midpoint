package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Experimental
public class CommonBeans {

    @Autowired public MatchingRuleRegistry matchingRuleRegistry;
    @Autowired public RelationRegistry relationRegistry;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired public PrismContext prismContext;
    @Autowired public ShadowsFacade shadowsFacade;
    @Autowired public ShadowManager shadowManager;
    @Autowired public ResourceObjectConverter resourceObjectConverter;

}
