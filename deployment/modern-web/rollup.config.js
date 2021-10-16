const {
    nodeResolve,
} = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/@rollup/plugin-node-resolve");
const commonjs = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/@rollup/plugin-commonjs");
const clear = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/rollup-plugin-clear");
const esbuild = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/rollup-plugin-esbuild");
const { babel } = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/@rollup/plugin-babel");
const nodePolyfills = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/rollup-plugin-node-polyfills");
const mendixResolve = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/rollup-plugin-mendix-resolve.js");
const css = require("C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/rollup-plugin-import-css");

const JAVASCRIPT_SOURCE_PATH_REGEX = /javascriptsource/;
const PLUGGABLE_WIDGETS_PATH_FILTER = "./widgets/**";

const isProduction = process.env.NODE_ENV === "production";

export default {
    input: "index.js",
    watch: {
        clearScreen: false,
    },
    output: {
        dir: "dist",
        format: "es",
        chunkFileNames: "[name].js",
        sourcemap: true,
        manualChunks(id) {
            if (id.includes("node_modules")) {
                return "commons";
            }
        },
    },
    treeshake: {
        // commented for now, as rollup stripped mobx configuration from index.ts
        // moduleSideEffects: false,
    },
    plugins: [
        mendixResolve(
            "C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/web-resolutions.json",
            "C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules"
        ),
        nodePolyfills(),
        esbuild({
            // All options are optional
            sourceMap: true, // default
            exclude: [JAVASCRIPT_SOURCE_PATH_REGEX, PLUGGABLE_WIDGETS_PATH_FILTER],
            minify: isProduction,
            // Like @rollup/plugin-replace
            define: {
                "process.env.NODE_ENV": JSON.stringify(process.env.NODE_ENV),
            },
            loaders: {
                // Enable JSX in .js files too
                ".js": "jsx",
            },
        }),
        ignore(/react-native/),
        nodeResolve({
            moduleDirectories: ["C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules"]
        }),
        commonjs({ transformMixedEsModules: true, exclude: [/mendix-web/, PLUGGABLE_WIDGETS_PATH_FILTER] }),
        // @rollup/plugin-babel must be placed after @rollup/plugin-commonjs
        babel({
            babelHelpers: "bundled",
            include: JAVASCRIPT_SOURCE_PATH_REGEX,
            presets: [
                [
                    "C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/@babel/preset-env",
                    { targets: { safari: "13" } },
                ],
            ],
            plugins: [
                "C:/Program Files/Mendix/9.6.0.27784/modeler/tools/node/node_modules/@babel/plugin-syntax-dynamic-import",
            ],
        }),
        clear({
            targets: ["dist"],
            // watch: true,
        }),
        css({
            output: "widgets.css",
            alwaysOutput: true,
            minify: isProduction
        })
    ],
};

function ignore(regex) {
    const emptyFile = "export default {}";
    const emptyFileName = "\0rollup_plugin_ignore_empty_module_placeholder";

    return {
        name: "ignore",
        resolveId(importee) {
            return importee === emptyFileName || regex.test(importee) ? emptyFileName : null;
        },
        load(id) {
            return id === emptyFileName ? emptyFile : null;
        },
    };
}
