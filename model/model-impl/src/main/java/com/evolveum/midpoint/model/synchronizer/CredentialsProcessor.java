/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.synchronizer;

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor that takes password from user and synchronizes it to accounts.
 * <p/>
 * The implementation is very simple now. It only cares about password value, not
 * expiration or other password facets. It completely ignores other credential types.
 *
 * @author Radovan Semancik
 */
@Component
public class CredentialsProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;

    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;

    public void processCredentials(SyncContext context, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        ObjectDelta<UserType> userDelta = context.getUserDelta();
        if (userDelta == null) {
            LOGGER.trace("userDelta is null, skipping credentials processing");
            return;
        }
        PropertyDelta passwordValueDelta = userDelta.getPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);

        MidPointObject<UserType> userNew = context.getUserNew();
        if (userNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("userNew is null, skipping credentials processing");
            return;
        }
        Property userPasswordNew = context.getUserNew().findProperty(SchemaConstants.PATH_PASSWORD_VALUE);

        Schema commonSchema = schemaRegistry.getObjectSchema();

        ObjectDefinition<AccountShadowType> accountDefinition = commonSchema.findObjectDefinition(AccountShadowType.class);
        PropertyDefinition accountPasswordPropertyDefinition = accountDefinition.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

        for (AccountSyncContext accCtx : context.getAccountContexts()) {
            ResourceAccountType rat = accCtx.getResourceAccountType();

            ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();
            if (accountDelta != null && accountDelta.getChangeType() == ChangeType.ADD) {
                // adding new account, synchronize password regardless whether the password was changed or not.
            } else if (passwordValueDelta != null) {
                // user password was changed. synchronize it regardless of the account change.
            } else {
                LOGGER.trace("No change in password and the account is not added, skipping credentials processing for account " + rat);
                continue;
            }

            ResourceAccountTypeDefinitionType resourceAccountDefType = accCtx.getResourceAccountTypeDefinitionType();
            if (resourceAccountDefType == null) {
                LOGGER.trace("No ResourceAccountTypeDefinition, therefore also no password outbound definition, skipping credentials processing for account " + rat);
                continue;
            }
            ResourceCredentialsDefinitionType credentialsType = resourceAccountDefType.getCredentials();
            if (credentialsType == null) {
                LOGGER.trace("No credentials definition in account type {}, skipping credentials processing", rat);
                continue;
            }
            ResourcePasswordDefinitionType passwordType = credentialsType.getPassword();
            if (passwordType == null) {
                LOGGER.trace("No password definition in credentials in account type {}, skipping credentials processing", rat);
                continue;
            }
            ValueConstructionType outbound = passwordType.getOutbound();
            if (outbound == null) {
                LOGGER.trace("No outbound definition in password definition in credentials in account type {}, skipping credentials processing", rat);
                continue;
            }
            ValueConstruction passwordConstruction = valueConstructionFactory.createValueConstruction(outbound, accountPasswordPropertyDefinition, "outbound password in account type " + rat);
            passwordConstruction.setInput(userPasswordNew);
            passwordConstruction.evaluate(result);
            Property accountPasswordNew = passwordConstruction.getOutput();
            if (accountPasswordNew == null) {
                LOGGER.trace("Credentials 'password' expression resulted in null, skipping credentials processing for {}", rat);
                continue;
            }
            PropertyDelta accountPasswordDelta = new PropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
            accountPasswordDelta.setValuesToReplace(accountPasswordNew.getValues());
            LOGGER.trace("Adding new password delta for account {}", rat);
            accCtx.addToSecondaryDelta(accountPasswordDelta);
        }

    }


}
