/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

const {merge} = require('webpack-merge');

const commonConfig = require('./webpack.common');

module.exports = (env) => {

    var suffix = env.production ? 'production' : 'development';

    const config = require('./webpack.' + suffix);
    return merge(commonConfig, config);
};
