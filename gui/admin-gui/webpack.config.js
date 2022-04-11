/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

const CssMinimizerPlugin = require('./node_modules/css-minimizer-webpack-plugin');
const MiniCssExtractPlugin = require('./node_modules/mini-css-extract-plugin');

const path = require('path');

module.exports = {
    entry: {
        midpoint: [
            './src/main/resources/static/scss/midpoint-theme.scss',
            './src/main/resources/static/js/index.js'
        ],
    },
    mode: 'production',
    devtool: false,
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/webpack/static/static'),
        publicPath: '/',
        filename: './[name].bundle.js',
    },
    module: {
        // noParse: /midpoint-theme.js/,
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
                    './node_modules/sass-loader',
                ],
            },
            // {
            //     test: require.resolve('../../../../node_modules/jquery'),
            //     use: [{
            //         loader: './node_modules/expose-loader',
            //         options: 'jQuery'
            //     },
            //         {
            //             loader: './node_modules/expose-loader',
            //             options: '$'
            //         }
            //     ]
            // },
            {
                test: /\.js$/,
                exclude: /node_modules/,
                // type: 'asset/resource',
                use: {
                    loader: "./node_modules/babel-loader"
                }
            },

            // Images: Copy image files to build folder
            { test: /\.(?:ico|gif|png|jpg|jpeg)$/i, type: 'asset/resource' },

            // Fonts and SVGs: Inline files
            { test: /\.(woff(2)?|eot|ttf|otf|svg|)$/, type: 'asset/inline' },
        ],
    },
    plugins: [
        // Extracts CSS into separate files
        new MiniCssExtractPlugin({
            filename: '[name]-theme.css',
            chunkFilename: '[id].css',
        }),
    ],
    optimization: {
        minimize: true,
        minimizer: [new CssMinimizerPlugin(), '...'],
        // runtimeChunk: {
        //     name: 'runtime',
        // },
    },
    performance: {
        hints: false,
        maxEntrypointSize: 512000,
        maxAssetSize: 512000,
    },
}
