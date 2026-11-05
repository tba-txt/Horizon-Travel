# Relatório Consolidado da Bateria de Testes — Projeto Horizon
**Evidências Metodológicas e Técnicas para o TCC (Seções 2.6.1 a 2.6.7)**  
**Data:** 31 de Outubro de 2026  
**Projeto:** Horizon (Web & API)  
**Arquitetura:** Hexagonal (Ports & Adapters) — Java 25 / Spring Boot 4.1.0 & Angular 22.1.0 / Vitest

---

## 1. Sumário Executivo e Decisões de Engenharia

O presente documento consolida o planejamento, a implementação e as diretrizes de execução da bateria completa de testes e análises de qualidade do projeto **Horizon**, estruturada para fundamentar as seções 2.6.1 a 2.6.7 do Trabalho de Conclusão de Curso (TCC).

### 1.1 Decisões Críticas Implementadas no Código
1. **Regra de Idade Máxima dos Passageiros (120 Anos):**
   * O código continha a validação legada de 99 anos em `PassengerRequest.java` e `ReservationService.java`.
   * Foram atualizadas as anotações `@Max(value = 120)` e as checagens condicionais `pReq.getAge() > 120` com a mensagem `"A idade do passageiro deve estar entre 1 e 120 anos."`.
   * As regras vigentes do projeto Horizon estão rigorosamente padronizadas:
     * Idade mínima do comprador: **18 anos**;
     * Idade dos passageiros: **1 a 120 anos**;
     * Quantidade de passageiros por reserva: **1 a 5**;
     * Antecedência da partida: **mínimo de 3 dias, máximo de 24 meses**;
     * Janela de pagamento pendente: **24 horas**.
2. **Formalização Matemática do `RecommendationEngine`:**
   * A compatibilidade de atributos segue estritamente a implementação vigente no código:
     $$\text{match}(u, d) = \max(0, 1 - |u - d|)$$
   * A composição híbrida aplica os pesos $0.6$ (explícito) e $0.4$ (comportamental):
     $$\text{score}_{\text{base}} = (\text{score}_{\text{explícito}} \times 0.6) + (\text{score}_{\text{comportamental}} \times 0.4)$$
   * Penalização orçamentária linear para destinos acima do orçamento e descarte estrito de destinos internacionais para viajantes sem passaporte ativo.
3. **Acessibilidade:**
   * Alinhada às recomendações internacionais da **WCAG 2.1 (Nível AA)**, contemplando Web Speech API nativa, VLibras, temas claro/escuro e alto contraste.
4. **Segurança Externa e Varredura:**
   * Estruturada e documentada para utilização com **OWASP ZAP**.

---

## 2. Seção 2.6.1 — Testes Unitários

### 2.1 Backend (Java / Spring Boot / JUnit 5 / Mockito)
Localização: `src/test/java/br/com/horizon/horizon_api/`

| Classe de Teste | Camada / Componente | Cenários Cobertos | Status |
| :--- | :--- | :--- | :--- |
| `RecommendationEngineTest` | Domínio / Motor de Recomendação | 12 testes cobrindo cold start, compatibilidade matemática, composição híbrida 60/40, filtro de passaporte nacional/internacional, penalização orçamentária, ranking, desempate e textos explicativos | **Criado** |
| `ReservationServiceTest` | Aplicação / Serviço de Reservas | 18 testes cobrindo comprador menor de 18 anos, comprador com 18 anos, passageiro com 1 ano, 120 anos, 121 anos, limites de 1 a 5 passageiros, datas de partida (3 dias a 24 meses), viagens América do Sul vs Internacional e reserva atômica de assentos | **Criado** |
| `ReservationRequestValidationTest` | Validação / Bean Validation | 4 testes verificando Bean Validation para limites de passageiros e idade máxima de 120 anos | **Criado** |
| `PaymentAndCancellationTest` | Aplicação / Pagamentos e Cancelamento | 5 testes cobrindo pagamento confirmado com emissão de bilhete e envio de e-mail, pagamento pendente, cancelamento com liberação de assentos e expiração em lote | **Criado** |
| `JwtTokenAdapterTest` | Infraestrutura / Segurança | 4 testes verificando geração de JWT, extração de subject, rejeição de token expirado e token com assinatura inválida | **Criado** |
| `RateLimitingFilterTest` | Interface / Filtros HTTP | 3 testes validando limite restrito de 20 req/min nas rotas de autenticação, 100 req/min nas rotas gerais e resposta HTTP 429 Too Many Requests | **Criado** |

### 2.2 Spring Batch (Tarefas Automatizadas)
Localização: `src/test/java/br/com/horizon/horizon_api/infrastructure/batch/`

| Classe de Teste | Componente Batch | Cenários Cobertos | Status |
| :--- | :--- | :--- | :--- |
| `BatchConfigTest` | Cancelamento e Finalização de Reservas | 6 testes cobrindo ItemReader, ItemProcessor e ItemWriter para cancelamento de reservas pendentes (>24h) e finalização de viagens concluídas | **Criado** |
| `WeeklyFeedbackBatchTest` | Relatório Semanal de CSAT / Feedback | 2 testes validando execução do Tasklet de feedback com consolidação de notas e execução com base sem avaliações | **Criado** |

### 2.3 Frontend (Angular 22.1.0 / Vitest)
Localização: `c:/Users/Gabri/Projetos/horizon-web/src/app/`

| Arquivo de Teste | Módulo / Componente | Cenários Cobertos | Status |
| :--- | :--- | :--- | :--- |
| `auth.service.spec.ts` | Autenticação e Sessão | 5 testes cobrindo login com sucesso, persistência com/sem rememberMe, logout com limpeza de storage e solicitação de recuperação de senha | **Criado** |
| `accessibility.service.spec.ts` | Acessibilidade e Preferências | 4 testes cobrindo valores padrão, ativação de alto contraste no `<html>`, escalonamento de fonte (`fontScale`) e restauração de padrões | **Criado** |
| `speech.service.spec.ts` | Web Speech API | 4 testes cobrindo inicialização, configuração de taxa de fala, interrupção via `stop()` e tratamento de strings vazias | **Criado** |
| `quiz.service.spec.ts` | Quiz de Preferências | 3 testes cobrindo obtenção do quiz ativo via GET e submissão com e sem ID via POST | **Criado** |
| `feed.service.spec.ts` | Feed Interativo | 4 testes cobrindo busca de posts com cache, `forceRefresh`, registro de LIKE e votação em enquete via POST | **Criado** |
| `recommendations.service.spec.ts` | Recomendações de Destino | 2 testes cobrindo consulta com limite padrão (10) e com limite customizado | **Criado** |
| `accessibility-settings.component.spec.ts` | Componente de Configurações | 3 testes cobrindo criação do componente, alternância de tema escuro/claro e alteração de escala tipográfica | **Criado** |
| `speech-control.component.spec.ts` | Dock Flutuante de Acessibilidade | 3 testes cobrindo renderização dos controles, alternância de alto contraste e disparo de tema | **Criado** |
| `app.spec.ts` | Componente Raiz | 3 testes cobrindo criação, título do app e renderização do `<router-outlet>` | **Criado** |

---

## 3. Seção 2.6.2 — Testes de Integração

Localização: `FlowsIntegrationTest.java`

A suíte de integração utiliza o `MockMvc` em modo standalone acoplado aos controllers reais da API e simula os 11 fluxos fundamentais da aplicação:

1. **Cadastro (`POST /api/auth/register`):** Validação de nome, e-mail único, senha e data de nascimento. Retorno esperado: `201 Created`.
2. **Login (`POST /api/auth/login`):** Validação de credenciais e emissão de JWT. Retorno: `200 OK` com token Bearer.
3. **Recuperação de Senha (`POST /api/auth/forgot-password`):** Geração de token temporal e envio de e-mail de recuperação. Retorno: `200 OK`.
4. **Questionário e Perfil Explícito (`POST /api/quizzes/{id}/submit`):** Persistência das respostas e atualização imediata do perfil explícito do usuário. Retorno: `200 OK`.
5. **Interação no Feed (`POST /api/interactions`):** Registro de likes e votos que incrementam o perfil comportamental de afinidades. Retorno: `200 OK`.
6. **Consulta de Recomendações (`GET /api/recommendations?limit=10`):** Execução do `RecommendationEngine` com composição híbrida e retorno de destinos ranqueados. Retorno: `200 OK`.
7. **Consulta de Destinos (`GET /api/destinations`):** Listagem com suporte a filtros de orçamento e documentação. Retorno: `200 OK`.
8. **Integração Climática Open-Meteo (`GET /api/destinations/{id}/weather`):** Integração com previsão do tempo e dados meteorológicos para o destino. Retorno: `200 OK`.
9. **Criação de Reserva (`POST /api/reservations`):** Validação de regras de comprador (>= 18 anos), passageiros (1 a 120 anos), bloqueio de assentos no voo. Retorno: `201 Created` com status `PENDING`.
10. **Processamento de Pagamento (`POST /api/payments`):** Confirmação de pagamento com alteração atômica do status da reserva para `CONFIRMED`. Retorno: `200 OK`.
11. **Emissão e Consulta de Bilhetes (`GET /api/reservations/{id}/tickets`):** Geração do e-ticket com dados do voo, passageiro e código de bilhete. Retorno: `200 OK`.

---

## 4. Seção 2.6.3 — Testes de Performance e Carga

Localização: `RecommendationEngineBenchmarkTest.java`

### 4.1 Benchmark do Algoritmo de Recomendação
A classe de benchmark executou um ensaio controlado com:
* **Ambiente de Medição:** Java 25.0.4.1 (Oracle Corporation) / Windows 10 (amd64) — 4 processadores
* **Escopo:** 50 destinos avaliados e ranqueados por execução
* **Amostragem:** 100 iterações de aquecimento (JIT compiler) + 1.000 iterações mensuradas
* **Métricas Reais Obtidas (saída do surefire XML):**
  * **Tempo Mínimo:** 0.2995 ms
  * **Tempo Máximo:** 73.1245 ms
  * **Tempo Médio:** 1.2448 ms
  * **Desvio Padrão:** 3.8936 ms
  * **Throughput Estimado:** 803 operações/segundo

### 4.2 Protocolo para Testes de Carga (JMeter / K6)
Para aferição de latência dos endpoints expostos sob concorrência, o teste deve ser executado pelo desenvolvedor contra os seguintes cenários:
* **Cenário 1 — Recomendações Concorrentes:**
  * Alvo: `GET /api/recommendations?limit=5`
  * Carga: 50 usuários virtuais concorrentes, rampa de subida de 10s, sustentação por 60s.
  * Meta SLA: Tempo de resposta $P95 < 200\text{ ms}$, taxa de erros $0\%$.
* **Cenário 2 — Catálogo de Destinos sob Carga:**
  * Alvo: `GET /api/destinations`
  * Carga: 100 usuários virtuais concorrentes com cache Redis habilitado.
  * Meta SLA: Tempo de resposta $P95 < 50\text{ ms}$.

---

## 5. Seção 2.6.4 — Testes de Segurança

Localização: `SecuritySuiteIntegrationTest.java`

### 5.1 Testes Automatizados no Backend (MockMvc)
1. **Proteção de Rotas Sensíveis:** Requisições a `/api/reservations` sem header `Authorization` retornam `401 Unauthorized` ou `403 Forbidden`.
2. **Rejeição de Tokens Inválidos e Expirados:** JWTs com assinatura forjada ou timestamp expirado são rejeitados na camada de filtro de segurança.
3. **Credenciais Inválidas:** Tentativas de login com senha incorreta retornam `401 Unauthorized` sem expor detalhes sobre existência prévia do e-mail.
4. **Proteção contra Força Bruta (Rate Limiting):**
   * Rotas de autenticação (`/api/auth/login`, `/api/auth/register`): Bloqueio a partir da 21ª requisição no mesmo minuto (`429 Too Many Requests`).
   * Rotas gerais: Limite de 100 requisições/minuto.
5. **Criptografia de Senhas:** Validação de que senhas de usuários são estritamente armazenadas com hash **BCrypt**.
6. **Sanitização de DTOs e Injeção:** DTOs com anotações `@NotNull`, `@Pattern`, `@Size` rejeitam payloads contendo caracteres maliciosos de XSS ou injeção de parâmetros.

### 5.2 Roteiro de Varredura Externa com OWASP ZAP
Para validação automatizada externa no TCC:
```bash
# Iniciar o OWASP ZAP e rodar verificação da API Horizon
zap.bat -daemon -port 8080
zap-baseline.py -t http://localhost:8080 -r zap-report.html
```

---

## 6. Seção 2.6.5 — Avaliação Formal do Motor de Recomendação

Localização completa: `TEST_SCENARIOS.md`

Foram definidos e verificados 9 cenários formais que comprovam o comportamento determinístico do algoritmo:

| ID | Cenário | Entrada | Cálculo Matemático | Resultado Esperado | Verificação |
| :---: | :--- | :--- | :--- | :---: | :---: |
| **CEN-REC-01** | Alta Compatibilidade | Explícito 0.90 / Comportamental 0.90 | $match=1.0 \implies 1.0 \times 0.6 + 1.0 \times 0.4$ | Score: `1.0000` | Exato |
| **CEN-REC-02** | Baixa Afinidade | Explícito 0.20 / Comportamental 0.80 | $match(\text{exp})=0.4, match(\text{comp})=0.4$ | Score: `0.4000` | Exato |
| **CEN-REC-03** | Cold Start Completo | Perfil explícito e comportamental vazios | Scores padrão $0.5 \times 0.6 + 0.5 \times 0.4$ | Score: `0.5000` | Exato |
| **CEN-REC-04** | Exclusão Passaporte | Viajante sem passaporte + destino internacional | Filtro estrito: `hasPassport = false` | Excluído | Não recomendado |
| **CEN-REC-05** | Destino sem Passaporte | Viajante sem passaporte + destino Mercosul/Brasil | Filtro: `requiresPassport = false` | Score: `0.7000` | Recomendado com aviso de RG |
| **CEN-REC-06** | Destino com Passaporte | Viajante com passaporte + destino internacional | Filtro aprovado (`hasPassport = true`) | Score: `0.8000` | Recomendado com aviso de Passaporte |
| **CEN-REC-07** | Dentro do Orçamento | Preço R$ 2.000 $\le$ Orçamento R$ 2.500 | Fator de penalidade = $1.0000$ | Score: `0.8500` | Sem penalidade |
| **CEN-REC-08** | Acima do Orçamento | Preço R$ 4.000 > Orçamento R$ 2.000 | $ratio=(4000-2000)/2000=1.0 \implies fator=0.0$ | Score: `0.0000` | Penalizado linearmente |
| **CEN-REC-09** | Critério de Desempate | 2 destinos com mesmo FinalScore e ExplicitScore | Desempate por proximidade ao orçamento e ID | Destino A > B | Ordem rigorosa |

---

## 7. Seção 2.6.6 — Avaliação da Acessibilidade (WCAG 2.1 AA)

A acessibilidade da aplicação Horizon foi avaliada e modelada em conformidade com as diretrizes da **WCAG 2.1**, níveis **A** e **AA**:

### 7.1 Recursos Nativos Implementados no Horizon
* **Leitura de Tela e Voz (Web Speech API):** O componente `SpeechControlComponent` permite leitura em voz alta do conteúdo textual da página e leitura automática ao receber foco via teclado (`readOnFocus`).
* **Língua Brasileira de Sinais (VLibras):** Widget oficial do VLibras integrado para tradução em Libras.
* **Alto Contraste:** Classe `.a11y-high-contrast` que redefine cores com contraste superior a $7:1$, superando a exigência de $4.5:1$ da WCAG AA.
* **Modo Escuro / Claro:** Alternância fluida através do atributo `data-bs-theme="dark"` / `data-theme="dark"`.
* **Escalonamento Tipográfico:** Suporte a zoom e redimensionamento proporcional do texto (100%, 115%, 130% e 150%) sem truncamento de layout.
* **Navegação por Teclado e Foco Visível:** Todos os elementos interativos possuem anel de foco evidente (`:focus-visible`) e sequência lógica de tabulação (`tabindex`).
* **Semântica Estrutural:** Uso estrito de landmarks HTML5 (`<header>`, `<nav>`, `<main>`, `<aside>`, `<footer>`) e atributo `lang="pt-BR"` na raiz.
* **Respeito às Preferências do Sistema:** Consulta nativa a `prefers-reduced-motion` e `prefers-color-scheme`.

---

## 8. Seção 2.6.7 — Guia de Execução e Reprodução pelo Usuário

### 8.1 Execução da Bateria de Testes do Backend (Maven)
No terminal PowerShell em `c:\Users\Gabri\Projetos\horizon-api`:

```powershell
# Executar suíte completa de testes unitários e de integração
mvn test

# Executar suíte específica de segurança
mvn test -Dtest=SecuritySuiteIntegrationTest

# Executar benchmark do RecommendationEngine
mvn test -Dtest=RecommendationEngineBenchmarkTest
```

### 8.2 Execução da Bateria de Testes do Frontend (Angular / Vitest)
No terminal PowerShell em `c:\Users\Gabri\Projetos\horizon-web`:

```powershell
# Executar todos os testes unitários com Vitest
npm test -- --watch=false

# Gerar relatório de cobertura de código
npm test -- --coverage --watch=false
```
