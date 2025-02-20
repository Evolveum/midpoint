/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

const webpack = require('./node_modules/webpack');
const MiniCssExtractPlugin = require('./node_modules/mini-css-extract-plugin');

const path = require('path');

module.exports = {
    entry: {
        vendors: [
            './src/frontend/js/vendors.js',
            './src/frontend/scss/vendors.scss',
        ],
        "vendors-passwords": [
            './src/frontend/js/vendors-passwords.js',
        ],
        "vendors-fonts": [
            './src/frontend/scss/vendors-fonts.scss',
        ],
        midpoint: [
            './src/frontend/js/midpoint.js',
            './src/frontend/scss/midpoint.scss',
        ],
    },
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/webpack/static/static'),
        publicPath: '../static/',
        filename: './[name].js',
        assetModuleFilename: './[name][ext]',
    },
    module: {
        rules: [
            {
                test: /\.(sass|scss|css)$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: './node_modules/css-loader',
                        options: {
                            importLoaders: 2,
                            sourceMap: false,
                            modules: false,
                        },
                    },
                    './node_modules/postcss-loader',
                    {
                        loader: "./node_modules/sass-loader",
                        options: {
                            sassOptions: {
                                outputStyle: "expanded",
                                quietDeps: true,
                                silenceDeprecations: ["abs-percent", "color-4-api", "import", "legacy-js-api", "color-functions", "fs-importer-cwd", "css-function-mixin", "duplicate-var-flags", "feature-exists", "global-builtin", "mixed-decls"]
                            }
                        },
                    },
                ],
            },
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: "./node_modules/babel-loader"
                }
            },
            // Images: Copy image files to build folder
            {
                test: /\.(?:ico|gif|png|jpg|jpeg)$/i,
                type: 'asset/resource',
                generator: {
                    filename: 'img/[name][ext]'
                }
            },
            // Fonts and SVGs: Inline files
            {
                test: /\.(woff(2)?|eot|ttf|otf|svg|)$/,
                type: 'asset/inline'
            },
        ],
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: 'jquery',
            jQuery: 'jquery',
            moment: 'moment',
        }),
    ],
    externals: {
        // external (from webjar) not to collide with wicket headers.
        jquery: 'jQuery',
    },
}
