# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# API base URLs are baked in at build time (Vite inlines VITE_* env).
ARG VITE_SESSION_API=http://localhost:8082
ARG VITE_STATION_API=http://localhost:8081
ARG VITE_WALLET_API=http://localhost:8083
ENV VITE_SESSION_API=$VITE_SESSION_API
ENV VITE_STATION_API=$VITE_STATION_API
ENV VITE_WALLET_API=$VITE_WALLET_API
RUN npm run build

# --- Run stage ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
