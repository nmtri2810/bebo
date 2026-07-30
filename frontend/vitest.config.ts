import path from "node:path";

import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  test: {
    environment: "jsdom",
    exclude: ["**/e2e/**", "**/.next/**", "**/node_modules/**"],
    globals: true,
    setupFiles: "./vitest.setup.ts",
  },
});
