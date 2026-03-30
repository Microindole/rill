import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import App from "./App.vue";
import router from "./router";
import { useAuthStore } from "@/stores/auth";
import "element-plus/dist/index.css";
import "@vue-flow/core/dist/style.css";
import "@vue-flow/core/dist/theme-default.css";
import "./styles.css";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

const authStore = useAuthStore();
void authStore.hydrateUser();

app.mount("#app");
