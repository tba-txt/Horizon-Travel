# HORIZON TRAVEL - GUIA DE DECISÕES E INTEGRAÇÃO DO FRONTEND

Este documento centraliza todas as decisões técnicas, regras de negócio e contratos de API definidos durante a implementação do backend da Horizon API, servindo como guia completo para a construção do **Frontend**.

---

## 1. Autenticação e Segurança (JWT)

- **Padrão**: JWT (JSON Web Token) via cabeçalho HTTP.
- **Cabeçalho obrigatório**: `Authorization: Bearer <token>`
- **Fluxo de Login**:
  - Endpoint: `POST /auth/login`
  - Body: `{ "email": "...", "password": "..." }`
  - Retorno: `{ "token": "...", "type": "Bearer" }`
- **Armazenamento no Frontend**: Guardar em local seguro (`localStorage`, `sessionStorage` ou `secure cookies`).
- **Swagger**: Configurado com botão verde **Authorize** no topo para testes diretos no navegador.

---

## 2. Cadastro e Dados do Usuário (`/users`)

- **Nome Civil (`name`)**:
  - **Obrigatório**.
  - Utilizado oficialmente para emissão de passagens aéreas e validações fiscais vinculadas ao CPF.
- **Nome Social (`socialName`)**:
  - **Opcional**.
  - Deve ser usado pelo Frontend para saudações ("Olá, [Nome Social]!"), cabeçalho de perfil e e-mails informais. Se não for informado, a interface usa o `name`.
- **E-mail e Confirmação**:
  - A confirmação de e-mail (digitar duas vezes no formulário) é responsabilidade **exclusiva do Frontend** antes de disparar o `POST /users`.
  - A API recebe apenas o campo `"email"`.
- **Foto de Perfil (`profileImageUrl`)**:
  - **Opcional**. Pode ser enviado `null` ou omitido no cadastro inicial.
- **CPF**:
  - Exatamente 11 dígitos numéricos (sem pontos ou traços).
- **Data de Nascimento (`birthDate`)**:
  - Formato ISO: `YYYY-MM-DD` (ex: `2000-05-15`).
- **Isolamento de Contas**:
  - Um usuário autenticado **só pode consultar, atualizar ou desativar o seu próprio ID** (`GET /users/{id}`, `PUT /users/{id}`, `PATCH/DELETE /users/{id}/deactivate`). Se tentar passar o ID de outro usuário, a API responde `403 Forbidden`.
  - O endpoint de busca aberta por e-mail (`GET /users?email=`) foi removido da API pública por segurança.

---

## 3. Upload de Imagens e Cloudinary

- **Arquitetura adotada**: O banco PostgreSQL é relacional/estruturado e **não armazena arquivos binários**. Ele armazena apenas o texto da URL (`TEXT`).
- **Responsabilidade do Frontend**:
  1. O usuário seleciona o arquivo no dispositivo.
  2. O Frontend faz o upload direto para o Cloudinary (via Widget do Cloudinary ou requisição REST direta).
  3. O Cloudinary retorna a URL pública segura (ex: `https://res.cloudinary.com/hatxmvxs/image/upload/.../foto.jpg`).
  4. O Frontend envia essa URL no campo `"profileImageUrl"` para a API Horizon (`POST /users` ou `PUT /users/{id}`).

---

## 4. Superfície da API Pública e Restrições de Negócio

Para manter o produto limpo, seguro e dentro do escopo do MVP, as seguintes regras foram fixadas:

| Recurso | Endpoints Disponíveis para o Frontend | Regras e Decisões |
|---|---|---|
| **Posts / Feed** | `GET /posts`<br>`GET /posts/{id}` | Criação, edição e publicação de posts são exclusivas de seed/admin backend. O usuário comum apenas consome o feed. |
| **Destinos** | `GET /destinations`<br>`GET /destinations/{id}`<br>`GET /destinations/{id}/weather` | O catálogo de destinos é alimentado internamente. Criação manual de destino pelo usuário final foi removida. |
| **Passageiros** | Embutidos no checkout da reserva | Não há CRUD solto de `/passengers`. Os passageiros são vinculados diretamente no `POST /reservations/checkout`. |
| **Actuator / Monitoramento** | Oculto no Swagger | Endpoints de saúde e métricas internas do Spring Boot/New Relic ficam restritos à infraestrutura. |

---

## 5. Roteiro Completo do Fluxo do Usuário (Jornada MVP)

O Frontend deve guiar o usuário na seguinte sequência de telas e chamadas:

```text
[1. Cadastro /users] 
       ↓
[2. Login /auth/login] → Recebe Bearer Token
       ↓
[3. Quiz Ativo /quizzes/active] 
       ↓
[4. Perguntas do Quiz /quizzes/{id}/questions]
       ↓
[5. Submeter Respostas POST /quizzes/submit]
       ↓
[6. Obter Recomendações GET /recommendations]
       ↓
[7. Explorar Destinos GET /destinations & Clima /{id}/weather]
       ↓
[8. Buscar Voos GET /flights?destinationId=...]
       ↓
[9. Checkout da Reserva POST /reservations/checkout] (envia voos e passageiros)
       ↓
[10. Pagamento POST /payments/{reservationId}/confirm]
       ↓
[11. Minhas Viagens GET /reservations/my]
       ↓
[12. Detalhes dos Bilhetes GET /tickets/reservation/{reservationId}]
       ↓
[13. Download do Cartão de Embarque GET /tickets/{ticketNumber}/download] (PDF)
       ↓
[14. Envio de Avaliação POST /feedbacks] (NPS / CSAT)
```

---

## 6. Processos em Segundo Plano (Batch Automático)

Esses processos não dependem de telas do frontend, mas impactam o ciclo de vida dos dados que a interface exibe:

1. **Batch de 5 minutos** (`cancelExpiredReservationsJob`):
   - **Expiração**: Reservas pendentes que passaram do prazo de pagamento são canceladas automaticamente e os assentos são liberados.
   - **Finalização**: Viagens confirmadas cuja data/hora de término já passou são marcadas como concluídas.
2. **Batch Semanal** (`weeklyFeedbackJob` - domingos à meia-noite):
   - Coleta todos os feedbacks dos últimos 7 dias.
   - Calcula métricas de NPS (Net Promoter Score) e CSAT da plataforma e das viagens.
   - Envia o relatório consolidado por e-mail via SMTP.

---

## 7. Variáveis de Ambiente e Versionamento

- **`.env` local**: Contém as senhas e credenciais reais para desenvolvimento local. Está no `.gitignore` e **nunca sobe para o Git**.
- **`.env.example`**: Versionado no repositório com as variáveis vazias (apenas as chaves/aliases) para guiar o deploy na nuvem (ex: Render).
- **Sem senhas hardcoded**: Nem o código Java nem o `application.yaml` possuem senhas em texto puro.
## Dívidas Técnicas (Frontend)
1. **Segurança JWT**: Atualmente o token será armazenado no `sessionStorage`. Deve-se refatorar o backend para suportar Refresh Tokens via **HttpOnly Cookies** e o frontend para armazenar o JWT principal apenas em memória (Signals).
