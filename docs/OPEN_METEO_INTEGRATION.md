# Integração com Open-Meteo - Horizon API

## Visão Geral

A Horizon API integra-se aos serviços meteorológicos públicos do **Open-Meteo** para fornecer aos viajantes informações climáticas contextuais e transparentes.

Como a plataforma Horizon permite reservas com até **24 meses de antecedência**, a arquitetura diferencia explicitamente:
1. **Previsão Meteorológica Real (`FORECAST`)** para viagens de curto prazo (0 a 16 dias).
2. **Contexto Climático Histórico (`HISTORICAL_CONTEXT`)** para viagens de médio e longo prazo (17 dias a 24 meses).

---

## 1. Arquitetura Hexagonal

O fluxo da funcionalidade respeita rigorosamente o padrão de Portas e Adaptadores:

```
[Cliente HTTP / Frontend]
        │
        ▼ GET /destinations/{id}/weather?travelDate=YYYY-MM-DD
[DestinationController] (interfaces.rest)
        │
        ▼ execute(destinationId, travelDate)
[GetDestinationWeatherUseCase] (application.usecase)
        │
        ▼ WeatherService (application.service)
        │ ├── DestinationPersistencePort.findById(...)
        │ └── Regra Temporal:
        │     • daysUntil <= 16  ──► WeatherPort.getDailyForecast(...)
        │     • daysUntil >= 17  ──► WeatherPort.getHistoricalContext(...)
        │
        ▼ WeatherPort (domain.port)
        │
        ▼ OpenMeteoAdapter (infrastructure.adapter)
        ├── OpenMeteo Forecast API (https://api.open-meteo.com/v1/forecast)
        └── OpenMeteo Archive API  (https://archive-api.open-meteo.com/v1/archive)
```

---

## 2. APIs e Endpoints Consumidos

### A. Previsão Diária (Curto Prazo: 0 a 16 dias)
* **Base URL**: `https://api.open-meteo.com`
* **Endpoint**: `/v1/forecast`
* **Parâmetros**:
  * `latitude`: Latitude do destino (obtida de `destinations`).
  * `longitude`: Longitude do destino (obtida de `destinations`).
  * `daily`: `weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max`
  * `forecast_days`: `16` (limite máximo determinístico do modelo numérico).
  * `timezone`: `auto`

### B. Clima Histórico / Climatologia (Médio e Longo Prazo: 17 dias a 24 meses)
* **Base URL**: `https://archive-api.open-meteo.com`
* **Endpoint**: `/v1/archive`
* **Janela Temporal**: Últimos 3 anos civis completos (`start_date = {currentYear - 3}-01-01` a `end_date = {currentYear - 1}-12-31`).
* **Parâmetros**:
  * `latitude`, `longitude`: Coordenadas do destino.
  * `start_date`, `end_date`: Intervalo de 3 anos completos.
  * `daily`: `temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,snowfall_sum,wind_speed_10m_max`
  * `timezone`: `auto`

---

## 3. Regras de Negócio e Fórmulas Estatísticas

### A. Regra Temporal de Chaveamento
$$\Delta t = \text{ChronoUnit.DAYS.between}(\text{LocalDate.now()}, \text{travelDate})$$
* $\Delta t < 0$: Erro de validação de negócio (`BusinessException: A data da viagem não pode ser no passado`).
* $0 \le \Delta t \le 16$: `WeatherMode.FORECAST` (previsão determinística para a data exata).
* $\Delta t \ge 17$: `WeatherMode.HISTORICAL_CONTEXT` (médias climatológicas para o mês da viagem).

### B. Fórmulas Estatísticas do Contexto Histórico
Para o conjunto $D$ de todos os dias da janela histórica cujo mês seja igual ao mês da viagem ($N_{\text{anos}} = 3$):

1. **Temperatura Média**:
   $$\bar{T} = \frac{1}{|D|} \sum_{d \in D} T_{\text{mean}}(d)$$
2. **Faixa Térmica Típica (Mínima e Máxima)**:
   $$\bar{T}_{\min} = \frac{1}{|D|} \sum_{d \in D} T_{\min}(d), \quad \bar{T}_{\max} = \frac{1}{|D|} \sum_{d \in D} T_{\max}(d)$$
3. **Precipitação Média Acumulada no Mês**:
   $$P_{\text{mês}} = \frac{1}{N_{\text{anos}}} \sum_{d \in D} \text{precip}(d)$$
4. **Dias de Chuva Estimados (`rainyDaysCount`)**:
   Derivado diretamente dos dados diários históricos:
   $$R = \text{round}\left(\frac{|\{d \in D \mid \text{precip}(d) \ge 1.0 \text{ mm}\}|}{N_{\text{anos}}}\right)$$
5. **Dias com Neve Estimados (`snowfallDaysCount`)**:
   Derivado diretamente dos dados diários históricos:
   $$S = \text{round}\left(\frac{|\{d \in D \mid \text{snowfall}(d) > 0.0 \text{ cm}\}|}{N_{\text{anos}}}\right)$$
6. **Vento Médio**:
   $$\bar{W} = \frac{1}{|D|} \sum_{d \in D} W_{\max}(d)$$

---

## 4. Resiliência e Tratamento de Erros

1. **Timeouts**:
   * Timeout de Conexão: **3.000 ms** (3 segundos).
   * Timeout de Leitura: **5.000 ms** (5 segundos).
   * Implementado via `SimpleClientHttpRequestFactory` no bean `RestClient`.
2. **Tratamento de Indisponibilidade**:
   * Caso o Open-Meteo esteja fora do ar, atinja timeout ou retorne erro HTTP, o adapter captura a falha, registra log via Slf4j e lança `WeatherIntegrationException`.
   * O `GlobalExceptionHandler` intercepta a exceção e retorna **HTTP 503 (Service Unavailable)** com corpo padronizado:
     ```json
     {
       "error": "Serviço de previsão meteorológica temporariamente indisponível."
     }
     ```
   * **Nenhum stack trace é exposto ao cliente.**
3. **Validação de Destino e Coordenadas**:
   * Destino inexistente $\rightarrow$ **HTTP 404 (Not Found)** via `ResourceNotFoundException`.
   * Destino sem coordenadas $\rightarrow$ **HTTP 400 (Bad Request)** via `BusinessException`.

---

## 5. Exemplo de Payloads

### Exemplo 1: Previsão Real (`FORECAST` - ex: viagem daqui a 5 dias)
`GET /destinations/56/weather?travelDate=2026-09-15` (Paris)
```json
{
  "destinationId": 56,
  "destinationName": "Paris",
  "city": "Paris",
  "country": "França",
  "latitude": 48.856600,
  "longitude": 2.352200,
  "targetDate": "2026-09-15",
  "mode": "FORECAST",
  "modeDescription": "Previsão meteorológica para a data da viagem",
  "timezone": "Europe/Paris",
  "summary": "Previsão meteorológica para a data da viagem (2026-09-15): máxima de 28.4°C, mínima de 15.2°C, Parcialmente nublado com 10% de probabilidade de chuva.",
  "forecast": {
    "description": "Previsão meteorológica obtida para a data da viagem.",
    "minTemperature": 15.2,
    "maxTemperature": 28.4,
    "apparentTemperature": 29.1,
    "precipitationMm": 0.2,
    "precipitationProbabilityPercent": 10,
    "weatherCode": 2,
    "weatherDescription": "Parcialmente nublado",
    "windSpeedKmH": 14.8
  }
}
```

### Exemplo 2: Contexto Histórico (`HISTORICAL_CONTEXT` - ex: viagem em Outubro de 2027)
`GET /destinations/61/weather?travelDate=2027-10-15` (Barcelona)
```json
{
  "destinationId": 61,
  "destinationName": "Barcelona",
  "city": "Barcelona",
  "country": "Espanha",
  "latitude": 41.385100,
  "longitude": 2.173400,
  "targetDate": "2027-10-15",
  "mode": "HISTORICAL_CONTEXT",
  "modeDescription": "Contexto climático histórico do período, sem tratar como previsão exata",
  "timezone": "Europe/Madrid",
  "summary": "Contexto climático histórico do período: em outubro, a região apresenta tipicamente temperatura média de 19.1°C (faixa esperada de 15.7°C a 23.1°C), cerca de 6 dias de chuva e precipitação acumulada de 44.9 mm. Médias históricas para planejamento da viagem, sem constituir previsão exata.",
  "historicalContext": {
    "referencePeriod": "Outubro (médias históricas 2023-2025)",
    "description": "Contexto climático histórico do período, sem tratar como previsão exata.",
    "meanTemperature": 19.1,
    "minTemperature": 15.7,
    "maxTemperature": 23.1,
    "totalPrecipitationMm": 44.9,
    "rainyDaysCount": 6,
    "snowyDaysCount": 0,
    "avgWindSpeedKmH": 17.3
  }
}
```
