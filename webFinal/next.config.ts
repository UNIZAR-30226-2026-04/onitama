import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  turbopack: {
    // Force module resolution to start at the app folder.
    root: path.join(__dirname),
  },
};

export default nextConfig;
