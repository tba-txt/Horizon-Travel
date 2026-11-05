# Horizon — Avaliação Formal do Motor de Recomendação (RecommendationEngine)
**Documento de Evidência para o TCC — Seção 2.6.5**  
**Data:** 31 de Outubro de 2026  
**Status dos Cenários:** Calculados e Verificados com as Fórmulas Reais da Implementação

---

## 1. Fórmulas Matemáticas Vigentes no Código

1. **Compatibilidade de Atributos:**
   $$\text{match}(u, d) = \max(0, 1 - |u - d|)$$
   $$\text{compatibilidade} = \frac{\sum \text{match}}{|\text{atributos do destino}|}$$
   *(Caso o usuário não possua afinidade com o atributo ($u \le 0$), a contribuição é zero).*

2. **Cold Start (Perfis Vazios):**
   $$\text{score}_{\text{explícito}} = 0.5000 \quad (\text{se explicitMap estiver vazio})$$
   $$\text{score}_{\text{comportamental}} = 0.5000 \quad (\text{se behaviorMap estiver vazio})$$

3. **Composição Híbrida 60/40:**
   $$\text{score}_{\text{base}} = (\text{score}_{\text{explícito}} \times 0.6) + (\text{score}_{\text{comportamental}} \times 0.4)$$

4. **Fator de Penalização Orçamentária:**
   $$\text{Se } \text{preço} > \text{orçamento}: \quad \text{ratio} = \frac{\text{preço} - \text{orçamento}}{\text{orçamento}}, \quad \text{fator} = \max(0, 1 - \text{ratio})$$
   $$\text{Se } \text{preço} \le \text{orçamento} \text{ ou sem orçamento}: \quad \text{fator} = 1.0000$$
   $$\text{score}_{\text{final}} = \min(1.0, \max(0.0, \text{score}_{\text{base}} \times \text{fator}))$$

5. **Critérios de Desempate (Hierarquia Estrita):**
   1. Maior $\text{score}_{\text{final}}$
   2. Maior $\text{score}_{\text{explícito}}$
   3. Menor distância absoluta do preço ao orçamento ($|\text{preço} - \text{orçamento}|$)
   4. Menor ID do destino ($\text{id}_1 < \text{id}_2$)

6. **Restrição Documental:**
   $$\text{Se } (\text{hasPassport} = \text{false} \land \text{requiresPassport} = \text{true}) \implies \text{Exclusão Imediata}$$

---

## 2. Cenários de Teste Controlados

### CENÁRIO A — Alta Compatibilidade Explícita e Comportamental
* **Identificador:** CEN-REC-01
* **Perfil do Usuário:**
  * Explícito: `{praia: 0.90, calor: 0.90}`
  * Comportamental: `{praia: 0.90, calor: 0.90}`
  * Orçamento: R$ 3.000,00 | Passaporte: Ativo (`true`)
* **Destino:** Fernando de Noronha | Atributos: `{praia: 0.90, calor: 0.90}` | Preço: R$ 2.000,00 | Exige Passaporte: `false`
* **Cálculo:**
  * $\text{match}(\text{praia}) = 1 - |0.90 - 0.90| = 1.0000$
  * $\text{match}(\text{calor}) = 1 - |0.90 - 0.90| = 1.0000$
  * $\text{score}_{\text{explícito}} = (1.0 + 1.0) / 2 = 1.0000$
  * $\text{score}_{\text{comportamental}} = (1.0 + 1.0) / 2 = 1.0000$
  * $\text{score}_{\text{base}} = (1.0 \times 0.6) + (1.0 \times 0.4) = 1.0000$
  * $\text{fator orçamento} = 1.0000 \ (\text{R\$ } 2000 \le \text{R\$ } 3000)$
  * $\text{score}_{\text{final}} = 1.0000$
* **Resultado Esperado:** Recomendação com Score 1.0000 (100% de afinidade) e explicação no Top 1.
* **Resultado Obtido:** Score 1.0000 | Status: **APROVADO**

---

### CENÁRIO B — Baixa Compatibilidade (Gostos Opostos)
* **Identificador:** CEN-REC-02
* **Perfil do Usuário:**
  * Explícito: `{frio: 0.90, neve: 0.90}` (Usuário busca destinos gelados)
  * Comportamental: `{}` (Vazio)
  * Orçamento: R$ 3.000,00 | Passaporte: Ativo (`true`)
* **Destino:** Rio de Janeiro | Atributos: `{praia: 0.90, calor: 0.90}` | Preço: R$ 2.000,00 | Exige Passaporte: `false`
* **Cálculo:**
  * $\text{score}_{\text{explícito}} = 0.0000$ (Não possui afinidade com praia nem calor)
  * $\text{score}_{\text{comportamental}} = 0.5000$ (Cold start comportamental)
  * $\text{score}_{\text{base}} = (0.0 \times 0.6) + (0.5 \times 0.4) = 0.2000$
  * $\text{fator orçamento} = 1.0000$
  * $\text{score}_{\text{final}} = 0.2000$
* **Resultado Esperado:** Score 0.2000. O destino não é pontuado positivamente pelas preferências explícitas, retendo apenas a componente neutra.
* **Resultado Obtido:** Score 0.2000 | Status: **APROVADO**

---

### CENÁRIO C — Cold Start Total (Novo Usuário)
* **Identificador:** CEN-REC-03
* **Perfil do Usuário:**
  * Explícito: `{}` (Sem quiz preenchido)
  * Comportamental: `{}` (Sem interações no feed)
  * Orçamento: Não informado (`null`) | Passaporte: `null` (assume `true`)
* **Destino:** Paris | Atributos: `{historia: 0.80, cultura: 0.80}` | Preço: R$ 5.000,00 | Exige Passaporte: `true`
* **Cálculo:**
  * $\text{score}_{\text{explícito}} = 0.5000$ (Regra de Cold Start)
  * $\text{score}_{\text{comportamental}} = 0.5000$ (Regra de Cold Start)
  * $\text{score}_{\text{base}} = (0.5 \times 0.6) + (0.5 \times 0.4) = 0.5000$
  * $\text{fator orçamento} = 1.0000$ (Orçamento nulo não aplica penalidade)
  * $\text{score}_{\text{final}} = 0.5000$
* **Resultado Esperado:** Score 0.5000 e explicação padrão: *"Recomendado como um destino popular e excelente ponto de partida."*
* **Resultado Obtido:** Score 0.5000 | Status: **APROVADO**

---

### CENÁRIO D — Restrição Documental (Destino Internacional sem Passaporte)
* **Identificador:** CEN-REC-04
* **Perfil do Usuário:**
  * Explícito: `{cultura: 1.0, metropole: 1.0}`
  * Orçamento: R$ 10.000,00 | Passaporte: Inativo (`hasPassport = false`)
* **Destino:** Tóquio (Japão) | Exige Passaporte: `true` (`requiresPassport = true`)
* **Cálculo:**
  * Condição: `Boolean.FALSE.equals(hasPassport) && Boolean.TRUE.equals(dest.getRequiresPassport())`
  * Ação: `continue` (Descarte do laço de processamento)
* **Resultado Esperado:** O destino é sumariamente excluído das recomendações e não aparece no ranking retornado ao usuário.
* **Resultado Obtido:** Excluído (0 ocorrências) | Status: **APROVADO**

---

### CENÁRIO E — Destino Elegível sem Passaporte (Acesso com RG)
* **Identificador:** CEN-REC-05
* **Perfil do Usuário:**
  * Explícito: `{natureza: 0.90}` | Passaporte: `false`
  * Orçamento: R$ 3.000,00
* **Destino:** Foz do Iguaçu (Brasil) | Exige Passaporte: `false` | Preço: R$ 1.500,00
* **Cálculo:**
  * Elegível para processamento.
  * $\text{score}_{\text{explícito}} = 1.0000$
  * $\text{score}_{\text{comportamental}} = 0.5000$ (Cold Start)
  * $\text{score}_{\text{base}} = (1.0 \times 0.6) + (0.5 \times 0.4) = 0.8000$
  * $\text{score}_{\text{final}} = 0.8000$
* **Resultado Esperado:** Destino retornado no Top 1 com a frase de acessibilidade documental anexada à explicação: *"Acessível apenas com seu RG!"*.
* **Resultado Obtido:** Score 0.8000 com texto explicativo contendo menção ao RG | Status: **APROVADO**

---

### CENÁRIO F — Destino Dentro do Orçamento Estipulado
* **Identificador:** CEN-REC-06
* **Perfil do Usuário:**
  * Orçamento: R$ 3.000,00
  * Afinidade: 100% de compatibilidade ($\text{score}_{\text{base}} = 1.0000$)
* **Destino:** Bonito (Brasil) | Preço: R$ 2.500,00 ($\text{preço} \le \text{orçamento}$)
* **Cálculo:**
  * $\text{preço} \le \text{orçamento} \implies \text{fator orçamento} = 1.0000$
  * $\text{score}_{\text{final}} = 1.0000 \times 1.0000 = 1.0000$
* **Resultado Esperado:** Nenhuma penalidade aplicada ao score e nenhuma advertência de preço adicionada à justificativa textual.
* **Resultado Obtido:** Score 1.0000 | Status: **APROVADO**

---

### CENÁRIO G — Destino com Excesso de Preço e Penalização Orçamentária
* **Identificador:** CEN-REC-07
* **Perfil do Usuário:**
  * Orçamento: R$ 3.000,00
  * Afinidade: 100% de compatibilidade ($\text{score}_{\text{base}} = 1.0000$)
* **Destino:** Patagônia (Chile) | Preço: R$ 3.600,00 (Excesso de R$ 600,00)
* **Cálculo:**
  * $\text{excess} = 3600 - 3000 = 600$
  * $\text{ratio} = 600 / 3000 = 0.2000$ (20% de sobrepreço)
  * $\text{fator orçamento} = 1 - 0.2000 = 0.8000$
  * $\text{score}_{\text{final}} = 1.0000 \times 0.8000 = 0.8000$
* **Resultado Esperado:** Redução proporcional de 20% no score final (de 1.0000 para 0.8000) e inclusão da observação: *"Apesar de estar um pouco acima do orçamento."* (pois $\text{fator} \ge 0.8$).
* **Resultado Obtido:** Score 0.8000 com advertência moderada | Status: **APROVADO**

---

### CENÁRIO H — Conflito Explícito (60%) vs. Comportamental (40%)
* **Identificador:** CEN-REC-08
* **Contexto:** O usuário respondeu no Quiz que prefere Montanha, mas no Feed interagiu exclusivamente com posts de Praia.
* **Perfil do Usuário:**
  * Explícito: `{montanha: 1.0, praia: 0.0}`
  * Comportamental: `{montanha: 0.0, praia: 1.0}`
  * Orçamento: R$ 5.000,00
* **Destino 1 (Campos do Jordão):** Atributos: `{montanha: 1.0}` | Preço: R$ 2.000,00
  * $\text{score}_{\text{explícito}} = 1.0000$
  * $\text{score}_{\text{comportamental}} = 0.0000$
  * $\text{score}_{\text{final}} = (1.0 \times 0.6) + (0.0 \times 0.4) = 0.6000$
* **Destino 2 (Florianópolis):** Atributos: `{praia: 1.0}` | Preço: R$ 2.000,00
  * $\text{score}_{\text{explícito}} = 0.0000$
  * $\text{score}_{\text{comportamental}} = 1.0000$
  * $\text{score}_{\text{final}} = (0.0 \times 0.6) + (1.0 \times 0.4) = 0.4000$
* **Resultado Esperado:** Campos do Jordão (0.6000) supera Florianópolis (0.4000) no ranking, comprovando o protagonismo do perfil declarado (60%) sobre o perfil comportamental (40%) conforme desenhado na tese.
* **Resultado Obtido:** Campos do Jordão em 1º lugar | Status: **APROVADO**

---

### CENÁRIO I — Desempate Quádruplo em Cascata
* **Identificador:** CEN-REC-09
* **Contexto:** Três destinos possuem empate absoluto em pontuação final e explícita.
* **Orçamento do Usuário:** R$ 3.000,00
* **Candidatos:**
  * Destino X (ID 10): $\text{Score} = 0.5000$ | Preço: R$ 2.900,00 (Distância do orçamento: $|2900 - 3000| = 100$)
  * Destino Y (ID 20): $\text{Score} = 0.5000$ | Preço: R$ 2.500,00 (Distância do orçamento: $|2500 - 3000| = 500$)
  * Destino Z (ID 5): $\text{Score} = 0.5000$ | Preço: R$ 2.900,00 (Distância do orçamento: $|2900 - 3000| = 100$)
* **Critério 1 (Score Final):** Empate (0.5000)
* **Critério 2 (Score Explícito):** Empate (0.5000)
* **Critério 3 (Proximidade ao Orçamento):**
  * Destino X e Destino Z (distância 100) vencem Destino Y (distância 500).
* **Critério 4 (Menor ID Numérico entre X e Z):**
  * $\text{ID } 5 < \text{ID } 10 \implies$ Destino Z vence Destino X.
* **Ordem Final Resultante:**
  1. Destino Z (ID 5)
  2. Destino X (ID 10)
  3. Destino Y (ID 20)
* **Resultado Esperado:** Ordenação exata $[Z, X, Y]$.
* **Resultado Obtido:** Ordem $[Z, X, Y]$ | Status: **APROVADO**
