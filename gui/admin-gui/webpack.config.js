/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

const {merge} = require('webpack-merge');

const commonConfig = require('./webpack.common');

module.exports = (env) => {

    var suffix = env.production ? 'production' : 'development';

    const config = require('./webpack.' + suffix);
    return merge(commonConfig, config);
};
