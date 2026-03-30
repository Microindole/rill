import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "node:path";

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./src")
        }
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes("node_modules")) {
                        if (id.includes("@vue-flow")) {
                            return "vue-flow";
                        }
                        if (id.includes("vue-router")) {
                            return "vue-router";
                        }
                        if (id.includes("pinia")) {
                            return "pinia";
                        }
                        if (
                            id.includes("/vue/") ||
                            id.includes("\\vue\\") ||
                            id.includes("@vue")
                        ) {
                            return "vue-core";
                        }
                    }
                    return undefined;
                }
            }
        }
    },
    server: {
        port: 5173
    }
});
