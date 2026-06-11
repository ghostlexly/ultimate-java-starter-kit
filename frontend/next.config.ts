import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Autorise les origines utilisées en dev derrière un reverse-proxy / tunnel.
  // Sans ça, NextJS bloque les requêtes cross-origin sur le serveur dev,
  // y compris le WebSocket HMR (`wss://tunnel.lunisoft.fr/_next/webpack-hmr`).
  allowedDevOrigins: ["tunnel.lunisoft.fr", "*.lunisoft.fr"],
};

export default nextConfig;
