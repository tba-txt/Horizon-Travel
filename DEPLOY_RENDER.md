# Deploy no Render (checklist para amanhã)

> Contexto: removi do código as senhas/chaves que estavam hardcoded (Gmail e Cloudinary).
> Agora **o back só funciona se essas variáveis existirem no Render**. Sem elas, o app não sobe
> (Cloudinary/DB/JWT) ou não envia e-mail (Gmail).

## 0. Antes de tudo

- [ ] **Trocar as credenciais antigas** (ficaram em texto puro no disco e na conversa):
  - Gmail: gerar uma **nova App Password** (Conta Google > Segurança > Verificação em duas etapas > Senhas de app) e revogar a antiga.
  - Cloudinary: no painel, **regenerar o API Secret** (Settings > API Keys).
- [ ] Conferir que o commit do back (`4edcd16`) é o que vai subir: `git log --oneline -1`.
- [ ] Fazer o push só depois de criar as variáveis no Render (ou o deploy automático vai falhar).

## 1. Variáveis de ambiente no Render

Render > serviço do back > **Environment** > Add Environment Variable.

### Obrigatórias (sem default, o app não inicia sem elas)

| Variável | O que é |
|---|---|
| `DB_URL` | URL JDBC do Neon, ex.: `jdbc:postgresql://<host>/neondb?sslmode=require` |
| `DB_USER` | usuário do banco |
| `DB_PASSWORD` | senha do banco |
| `JWT_SECRET` | segredo do JWT (string longa e aleatória; **se mudar, todos os logins atuais caem**) |
| `CLOUDINARY_NAME` | cloud name (antes era `horizon-travel`) |
| `CLOUDINARY_KEY` | API key do Cloudinary |
| `CLOUDINARY_SECRET` | API secret do Cloudinary (**o novo**, depois de regenerar) |

### E-mail / Gmail (NOVAS, ainda não criadas)

| Variável | Valor |
|---|---|
| `MAIL_USERNAME` | o e-mail Gmail que envia (antes era `horizonfatec@gmail.com`) |
| `MAIL_PASSWORD` | a **App Password** de 16 caracteres (pode colar com ou sem espaços, o código remove os espaços) |

`MAIL_HOST` (smtp.gmail.com) e `MAIL_PORT` (587) já têm default, não precisa criar.

### Recomendadas

| Variável | Valor sugerido | Por quê |
|---|---|---|
| `FRONTEND_BASE_URL` | `https://horizon-web-5zhz.onrender.com` | Usada nos links dos e-mails (recuperar senha etc.). **O default é `localhost`, então sem isso os links chegam quebrados.** |
| `JPA_DDL_AUTO` | `update` (ou `validate` quando o schema estiver estável) | Default já é `update` |
| `FEEDBACK_REPORT_EMAIL` | e-mail que recebe o relatório de feedbacks | O default é um e-mail pessoal fixo no código |

### Opcionais (têm default, só crie se precisar)

`PORT` (o Render já injeta), `SHOW_SQL` (false), `HEALTH_SHOW_DETAILS` (never),
`NEW_RELIC_ENABLED` (false), `OPEN_METEO_*`.

## 2. Pontos de atenção

- [ ] **CORS**: o back já libera `https://horizon-web-5zhz.onrender.com`, `localhost:4200` e `localhost:3000`. Se a URL do front no Render for outra, ajustar em `SecurityConfig`/`WebConfig`.
- [ ] **Front aponta para localhost**: o commit do front deixou `src/environments/environment.ts` com `apiUrl: 'http://localhost:8080'` e `production: false`. **Antes de subir o front**, voltar para:
  ```ts
  export const environment = {
    production: true,
    apiUrl: 'https://horizon-back-mfec.onrender.com'
  };
  ```
- [ ] **Java 25**: o Dockerfile usa `eclipse-temurin:25`. O build no Render roda `mvnw clean package -DskipTests` (pode levar alguns minutos).
- [ ] **Banco**: com `ddl-auto: update` o Hibernate cria/altera tabelas sozinho. Se quiser segurança extra, faça um backup/branch no Neon antes do primeiro deploy.
- [ ] **Plano free do Render**: o primeiro request após inatividade demora (cold start). Normal.
- [ ] Ainda existe um default de e-mail (`horizonfatec@gmail.com`) em outro `@Value` do adaptador SMTP. Não é senha, mas se `MAIL_USERNAME` estiver definido ele é ignorado.

## 3. Passo a passo

1. Trocar credenciais (item 0).
2. Criar as variáveis do item 1 no Render.
3. Ajustar `environment.ts` do front (item 2) e commitar.
4. `git push` do back e depois do front.
5. Acompanhar **Logs** do deploy do back no Render. Sucesso = `Started HorizonApiApplication` e `JavaMailSender initialized for host: smtp.gmail.com`.

## 4. Smoke test depois do deploy

- [ ] `GET https://horizon-back-mfec.onrender.com/actuator/health` retorna `{"status":"UP"}`
- [ ] `GET .../destinations` lista os destinos
- [ ] Login funciona no front
- [ ] Cadastro de usuário novo funciona
- [ ] Upload de foto de perfil funciona (testa o Cloudinary)
- [ ] "Esqueci minha senha" envia e-mail e o link abre o front correto (testa Gmail + `FRONTEND_BASE_URL`)

## 5. Se der errado

| Sintoma no log | Causa provável |
|---|---|
| `Could not resolve placeholder 'CLOUDINARY_...'` / `'DB_URL'` / `'JWT_SECRET'` | variável faltando no Render |
| `AuthenticationFailedException` / `535 Username and Password not accepted` | `MAIL_USERNAME`/`MAIL_PASSWORD` errados ou App Password sem verificação em 2 etapas |
| `Must supply a valid cloud_name` / `Invalid api_key` | variáveis do Cloudinary erradas |
| Front com erro de CORS | origem do front não está na lista do `SecurityConfig` |
| Links de e-mail abrindo `localhost` | faltou `FRONTEND_BASE_URL` |
| Todos deslogados após o deploy | `JWT_SECRET` diferente do anterior (esperado se você trocou) |

**Plano B:** no Render > Deploys, dá para fazer **Rollback** para o deploy anterior em um clique.
