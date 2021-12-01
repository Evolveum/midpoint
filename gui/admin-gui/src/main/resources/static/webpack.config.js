const CssMinimizerPlugin = require('../../../../target/node_modules/css-minimizer-webpack-plugin');
const MiniCssExtractPlugin = require('../../../../target/node_modules/mini-css-extract-plugin');

const path = require('path');

module.exports = {
    entry: {
        midpoint: ['./src/main/resources/static/scss/midpoint-theme.scss', './src/main/resources/static/js/midpoint-theme.js'],
    },
    mode: 'production',
    devtool: false,
    output: {
        path: path.resolve(__dirname, 'dist'),
        publicPath: '/',
        filename: './[name].bundle.js',
    },
    module: {
        noParse: /midpoint-theme.js/,
        rules: [
            {
                test: /\.(sass|scss|css)$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: './target/node_modules/css-loader',
                        options: {
                            importLoaders: 2,
                            sourceMap: false,
                            modules: false,
                        },
                    },
                    './target/node_modules/postcss-loader',
                    './target/node_modules/sass-loader',
                ],
            },
            {
                test: /\.js$/,
                exclude: /node_modules/,
                type: 'asset/resource'
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