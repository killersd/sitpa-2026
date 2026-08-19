# --- Estagio de build ---
FROM node:22-alpine AS build
WORKDIR /projeto

COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# --- Imagem final ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /projeto/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD ["sh", "-c", "wget -q --spider http://localhost/ || exit 1"]
