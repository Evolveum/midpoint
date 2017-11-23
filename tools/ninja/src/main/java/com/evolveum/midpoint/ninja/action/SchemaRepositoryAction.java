package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.opts.SchemaOptions;
import org.apache.commons.lang.NotImplementedException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchemaRepositoryAction extends RepositoryAction<SchemaOptions> {

    @Override
    public void execute() throws Exception {
        // todo implement import-sql (create schema) or validate-schema

        throw new NotImplementedException("Feel free to create pull request :)");
    }
}
