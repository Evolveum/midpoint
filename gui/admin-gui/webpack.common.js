/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

const webpack = require('./node_modules/webpack');
const MiniCssExtractPlugin = require('./node_modules/mini-css-extract-plugin');
const CopyPlugin = require('copy-webpack-plugin');

const path = require('path');

module.exports = {
    entry: {
        midpoint: [
            './src/frontend/js/app.js',
            './src/frontend/scss/app.scss',
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
                test: require.resolve('jquery'),
                loader: 'expose-loader',
                options: {
                    exposes: ['$', 'jQuery']
                }
            },
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
                                loadPaths: ['node_modules'],
                                silenceDeprecations: ["extend", "abs-percent", "color-4-api", "import", "legacy-js-api", "color-functions", "fs-importer-cwd", "css-function-mixin", "duplicate-var-flags", "feature-exists", "global-builtin", "mixed-decls"]
                            }
                        },
                    },
                ],
            },
            {
                test: /worker-xml\.js$/,
                type: 'asset/resource',
                generator: {
                    filename: 'worker-xml.js'
                }
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
            }
        ],
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: 'jquery',
            jQuery: 'jquery',
            moment: 'moment',
        }),
        new CopyPlugin({
            patterns: [
                {
                    from: 'node_modules/ace-builds/src-noconflict/worker-xml.js',
                    to: 'worker-xml.js'
                }
            ]
        })
    ],
    externals: {
        // external (from webjar) not to collide with wicket headers.
        jquery: 'jQuery',
    },
}
