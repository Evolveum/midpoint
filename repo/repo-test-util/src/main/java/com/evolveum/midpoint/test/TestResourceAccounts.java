/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Class which represents all accounts on the resource, allowing various operations on them. E.g. it allows to link
 * or correlate them with users on given "correlation" pair of attribute/property.
 */
@Experimental
public class TestResourceAccounts {
    private final ShadowReader shadowReader;
    private final PrismObject<ResourceType> resource;
    private final PrismContext prismContext;

    public TestResourceAccounts(ShadowReader shadowReader, PrismObject<ResourceType> resource,
            PrismContext prismContext) {
        this.shadowReader = shadowReader;
        this.resource = resource;
        this.prismContext = prismContext;
    }

    /**
     * Tries to link accounts from this resource to the provided users.
     *
     * It links the account to a user, if the "correlation" attribute value of the account matches the "correlation"
     * property value of the user.
     *
     * WARNING: This method does not check the consistency of the links, e.g. it does not check, if one account is
     * not linked with more than one user. If more users can be correlated with the account, it will be linked with
     * all of them.
     *
     * @param users The users which should be linked (when correlation condition is met) with the accounts.
     * @param changesExecutor The object which can execute given deltas containing changes with links.
     * @return The instance of the correlation attribute/property pair specifier.
     */
    public CorrelationPairSpecifier linkWithUsers(Collection<PrismObject<UserType>> users,
            ObjectChangesExecutor changesExecutor, Task task, OperationResult result) throws CommonException {
        return processUserShadowCorrelation(users, changesExecutor, this::linkAccountDelta, task, result);
    }

    /**
     * Tries to correlate accounts from this resource to the provided users.
     *
     * It links the account to a user, if the "correlation" attribute value of the account matches the "correlation"
     * property value of the user.
     *
     * The main difference between this method and the
     * {@link #linkWithUsers(Collection, ObjectChangesExecutor, Task, OperationResult)}
     * is that this method does not link the user with the shadow, but only updates/create the correlation "state" of
     * the shadow with the information about found candidate owner.
     *
     * WARNING: This method does not check, if there is only one "resulting owner" of the account. If there are more
     * users which can be correlated with the account, the resulting owner will be overwritten and the last checked
     * user will be used.
     *
     * @param users The users which should be linked (when correlation condition is met) with the accounts.
     * @param changesExecutor The object which can execute given deltas containing changes with links.
     * @return The instance of the correlation attribute/property pair specifier.
     */
    public CorrelationPairSpecifier correlateWithUsers(Collection<PrismObject<UserType>> users,
            ObjectChangesExecutor changesExecutor, Task task, OperationResult result) throws CommonException {
        return processUserShadowCorrelation(users, changesExecutor, this::correlateAccountDelta, task, result);
    }

    private <T extends ObjectType> CorrelationPairSpecifier processUserShadowCorrelation(
            Collection<PrismObject<UserType>> users, ObjectChangesExecutor changesExecutor,
            DeltaProvider<T> deltaCreator, Task task, OperationResult result)
            throws CommonException {
        final Collection<PrismObject<ShadowType>> shadows = this.shadowReader.readShadows(this.resource, task, result);
        return (accountAttrName, userPropertyName) -> {
            final List<PrismObject<ShadowType>> correlatedAccounts = new ArrayList<>();
            for (PrismObject<UserType> user : users) {
                for (PrismObject<ShadowType> shadow : shadows) {
                    final Object userProperty = getPropertyValue(userPropertyName, user);
                    final Object shadowAttribute = ShadowUtil.getAttributeValue(shadow, accountAttrName);
                    if (userProperty == null && shadowAttribute == null) {
                        continue;
                    }
                    if (Objects.equals(userProperty, shadowAttribute)) {
                        changesExecutor.executeChanges(deltaCreator.delta(shadow, user));
                        correlatedAccounts.add(shadow);
                    }
                }
            }
            return correlatedAccounts;
        };
    }

    private ObjectDelta<UserType> linkAccountDelta(PrismObject<ShadowType> shadow, PrismObject<UserType> user)
            throws SchemaException {
        return this.prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(ObjectTypeUtil.createObjectRef(shadow.getOid(), ObjectTypes.SHADOW))
                .asObjectDelta(user.getOid());
    }

    private ObjectDelta<ShadowType> correlateAccountDelta(PrismObject<ShadowType> shadow, PrismObject<UserType> user)
            throws SchemaException {
        return this.prismContext.deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_RESULTING_OWNER))
                .replace(MiscSchemaUtil.createObjectReference(user, UserType.class))
                .asObjectDelta(shadow.getOid());
    }

    private static @Nullable Object getPropertyValue(ItemPath userPropertyName, PrismObject<UserType> user) {
        final Object userProperty = user.findProperty(userPropertyName).getRealValue();
        if (userProperty instanceof PolyString poly) {
            return poly.getOrig();
        }
        return userProperty;
    }

    @FunctionalInterface
    public interface CorrelationPairSpecifier {

        /**
         * Specifies the correlation pair of account attribute and user property.
         *
         * @param accountAttribute The attribute from the account, used to correlate with user.
         * @param userProperty The property from the user, used to correlate with account.
         * @return The shadows of accounts, which where successfully matched with users on given items.
         */
        Collection<PrismObject<ShadowType>> onAttributes(ItemName accountAttribute, ItemPath userProperty)
                throws SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException,
                ObjectAlreadyExistsException;
    }

    private interface DeltaProvider<T extends ObjectType> {
        ObjectDelta<T> delta(PrismObject<ShadowType> shadow, PrismObject<UserType> user) throws SchemaException;
    }
}
