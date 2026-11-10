# syntax=docker/dockerfile:1.7
# Front-end ProSint: React 19 + Vite 8 compilado e servido por Nginx sem root.

# Vite 8 exige Node 20.19+ ou 22.12+; Node 24 e a LTS ativa.
FROM node:24.22.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
# Mesma origem: sem VITE_API_URL o bundle chama /api no proprio dominio (src/services/api.ts).
# Arquivos .env sao excluidos pelo .dockerignore e a variavel e esvaziada explicitamente.
ENV VITE_API_URL=
# Falha se houver host local com porta (API/servidor de desenvolvimento). O React Router contem
# "http://localhost" sem porta apenas como base interna de parsing de URL, o que nao e uma chamada.
RUN npm run typecheck \
 && npx vite build \
 && if grep -rIoE "(localhost|127\.0\.0\.1|0\.0\.0\.0):[0-9]{2,5}" dist; then echo "ERRO: bundle de producao contem URL local" >&2; exit 1; fi

FROM nginxinc/nginx-unprivileged:1.30.5-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
