package br.com.horizon.horizon_api.application.service;

import br.com.horizon.horizon_api.application.dto.response.RecommendationResponseDTO;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.BehaviorProfileAttributePersistencePort;
import br.com.horizon.horizon_api.domain.port.DestinationPersistencePort;
import br.com.horizon.horizon_api.domain.port.UserProfileAttributePersistencePort;
import br.com.horizon.horizon_api.domain.port.UserProfilePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationEngineTest {

    @Mock
    private UserProfileAttributePersistencePort explicitPort;

    @Mock
    private BehaviorProfileAttributePersistencePort behaviorPort;

    @Mock
    private DestinationPersistencePort destinationPort;

    @Mock
    private UserProfilePersistencePort userProfilePort;

    @InjectMocks
    private RecommendationEngine engine;

    @BeforeEach
    void setUp() {
        lenient().when(explicitPort.getUserProfilesByUserId(Mockito.anyLong())).thenReturn(Collections.emptyList());
        lenient().when(behaviorPort.getBehaviorProfilesByUserId(Mockito.anyLong())).thenReturn(Collections.emptyList());
        lenient().when(userProfilePort.findByUserId(Mockito.anyLong())).thenReturn(Optional.empty());
        lenient().when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(Collections.emptyList());
    }

    private Destination createDestination(Long id, String name, String country, BigDecimal price, boolean requiresPassport, List<DestinationAttribute> attrs) {
        Destination d = new Destination();
        d.setId(id);
        d.setName(name);
        d.setCountry(country);
        d.setCity(name);
        d.setBasePrice(price);
        d.setRequiresPassport(requiresPassport);
        d.setAttributes(attrs != null ? attrs : new ArrayList<>());
        return d;
    }

    private DestinationAttribute createDestAttr(String name, String score) {
        DestinationAttribute da = new DestinationAttribute();
        da.setAttributeName(name);
        da.setScore(new BigDecimal(score));
        return da;
    }

    private UserProfileAttribute createExplicitAttr(String name, String score) {
        UserProfileAttribute upa = new UserProfileAttribute();
        upa.setAttributeName(name);
        upa.setScore(new BigDecimal(score));
        return upa;
    }

    private BehaviorProfileAttribute createBehaviorAttr(String name, String score) {
        BehaviorProfileAttribute bpa = new BehaviorProfileAttribute();
        bpa.setAttributeName(name);
        bpa.setScore(new BigDecimal(score));
        return bpa;
    }

    // -------------------------------------------------------------------------
    // 1. Cold Start & Orçamento Nulo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Cold Start Total: Quando não há perfil explícito nem comportamental, aplica scores neutros 0.5 e composição 0.5000")
    void testColdStartBothProfilesEmpty() {
        Destination d = createDestination(1L, "Paris", "França", new BigDecimal("5000"), true, List.of(createDestAttr("cultura", "1.0")));
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(d));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);

        assertEquals(1, res.size());
        // explicit = 0.5, behavior = 0.5 -> 0.5 * 0.6 + 0.5 * 0.4 = 0.5000
        assertEquals(new BigDecimal("0.5000"), res.get(0).getScore());
        assertEquals("Recomendado como um destino popular e excelente ponto de partida.", res.get(0).getExplanation());
    }

    @Test
    @DisplayName("Orçamento não informado (null): não penaliza o score base")
    void shouldHandleNullBudget() {
        when(userProfilePort.findByUserId(1L)).thenReturn(Optional.empty()); // null budget

        Destination d = createDestination(10L, "Tóquio", "Japão", new BigDecimal("1000"), false, Collections.emptyList());
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(d));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(1, res.size());
        assertEquals(new BigDecimal("0.5000"), res.get(0).getScore());
    }

    // -------------------------------------------------------------------------
    // 2. Cálculo de Compatibilidade Exata e Composição 60/40
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Composição 60/40 com cálculo exato de compatibilidade: match(u,d) = 1 - |u - d|")
    void testExactCompatibilityAnd6040Composition() {
        // Usuário tem afinidade explícita: praia = 1.0, calor = 0.8
        when(explicitPort.getUserProfilesByUserId(1L)).thenReturn(List.of(
                createExplicitAttr("praia", "1.0"),
                createExplicitAttr("calor", "0.8")
        ));

        // Usuário tem afinidade comportamental: praia = 0.5, gastronomia = 0.9
        when(behaviorPort.getBehaviorProfilesByUserId(1L)).thenReturn(List.of(
                createBehaviorAttr("praia", "0.5"),
                createBehaviorAttr("gastronomia", "0.9")
        ));

        // Destino com 2 atributos: praia = 0.9, calor = 0.7
        // Explicit match:
        //   praia: 1 - |1.0 - 0.9| = 0.9
        //   calor: 1 - |0.8 - 0.7| = 0.9
        //   explicitScore = (0.9 + 0.9) / 2 = 0.9000
        // Behavior match:
        //   praia: 1 - |0.5 - 0.9| = 0.6
        //   calor: userScore é zero (não interagiu com calor) -> soma 0
        //   behaviorScore = (0.6 + 0) / 2 = 0.3000
        // BaseScore = 0.9000 * 0.6 + 0.3000 * 0.4 = 0.5400 + 0.1200 = 0.6600
        Destination d = createDestination(1L, "Rio de Janeiro", "Brasil", new BigDecimal("1500"), false, List.of(
                createDestAttr("praia", "0.9"),
                createDestAttr("calor", "0.7")
        ));
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(d));

        UserProfile up = new UserProfile();
        up.setBudgetPerPerson(new BigDecimal("2000")); // Preço dentro do orçamento
        up.setHasPassport(true);
        when(userProfilePort.findByUserId(1L)).thenReturn(Optional.of(up));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(1, res.size());
        assertEquals(new BigDecimal("0.6600"), res.get(0).getScore());
    }

    // -------------------------------------------------------------------------
    // 3. Regra de Documentação e Passaporte
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Cenário A: Usuário sem passaporte (hasPassport = false) e destino exige passaporte (requiresPassport = true) -> Excluído")
    void testPassportRequiredUserWithoutPassportExcluded() {
        UserProfile profile = new UserProfile();
        profile.setHasPassport(false);
        doReturn(Optional.of(profile)).when(userProfilePort).findByUserId(Mockito.anyLong());

        Destination dEurope = createDestination(1L, "Roma", "Itália", new BigDecimal("4000"), true, List.of(createDestAttr("historia", "1.0")));
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(dEurope));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertTrue(res.isEmpty(), "Destino que exige passaporte deve ser completamente excluído para quem não tem passaporte");
    }

    @Test
    @DisplayName("Cenário B: Usuário sem passaporte e destino NÃO exige passaporte -> Elegível com texto explicativo de RG")
    void testPassportNotRequiredUserWithoutPassportEligibleWithRgText() {
        UserProfile profile = new UserProfile();
        profile.setHasPassport(false);
        doReturn(Optional.of(profile)).when(userProfilePort).findByUserId(Mockito.anyLong());

        UserProfileAttribute upa = new UserProfileAttribute();
        upa.setAttributeName("natureza");
        upa.setScore(new BigDecimal("0.9"));
        doReturn(List.of(upa)).when(explicitPort).getUserProfilesByUserId(Mockito.anyLong());

        Destination dBrazil = createDestination(2L, "Foz do Iguaçu", "Brasil", new BigDecimal("1200"), false, List.of(createDestAttr("natureza", "0.9")));
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(dBrazil));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(1, res.size());
        assertEquals("Foz do Iguaçu", res.get(0).getDestinationName());
        assertTrue(res.get(0).getExplanation().contains("Acessível apenas com seu RG!"), "Deve conter menção de acesso facilitado com RG");
    }

    @Test
    @DisplayName("Cenário C: Usuário com passaporte ativo (hasPassport = true) e destino exige passaporte -> Elegível")
    void testPassportRequiredUserWithPassportEligible() {
        UserProfile profile = new UserProfile();
        profile.setHasPassport(true);
        doReturn(Optional.of(profile)).when(userProfilePort).findByUserId(Mockito.anyLong());

        Destination dNewYork = createDestination(3L, "Nova York", "EUA", new BigDecimal("6000"), true, List.of(createDestAttr("metropole", "1.0")));
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(dNewYork));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(1, res.size());
        assertEquals("Nova York", res.get(0).getDestinationName());
    }

    // -------------------------------------------------------------------------
    // 4. Regras e Fatores de Orçamento
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Fator de Orçamento: Penalização linear proporcional ao excesso de preço")
    void shouldApplyBudgetFactorCorrectly() {
        UserProfileAttribute u1 = createExplicitAttr("natureza", "1.0");
        UserProfileAttribute u2 = createExplicitAttr("aventura", "1.0");
        when(explicitPort.getUserProfilesByUserId(1L)).thenReturn(List.of(u1, u2));

        BehaviorProfileAttribute b1 = createBehaviorAttr("natureza", "1.0");
        BehaviorProfileAttribute b2 = createBehaviorAttr("aventura", "1.0");
        when(behaviorPort.getBehaviorProfilesByUserId(1L)).thenReturn(List.of(b1, b2));

        UserProfile profile = new UserProfile();
        profile.setBudgetPerPerson(new BigDecimal("2000"));
        doReturn(Optional.of(profile)).when(userProfilePort).findByUserId(Mockito.anyLong());

        // Dest 1: Preço 1500 (abaixo do orçamento) -> factor 1.0 -> finalScore 1.0000
        Destination d1 = createDestination(1L, "Bonito", "Brasil", new BigDecimal("1500"), false, List.of(createDestAttr("natureza", "1.0"), createDestAttr("aventura", "1.0")));
        // Dest 2: Preço 2000 (exatamente no orçamento) -> factor 1.0 -> finalScore 1.0000
        Destination d2 = createDestination(2L, "Chapada", "Brasil", new BigDecimal("2000"), false, List.of(createDestAttr("natureza", "1.0"), createDestAttr("aventura", "1.0")));
        // Dest 3: Preço 2500 (excesso 500 / 2000 = 0.25) -> factor 0.75 -> finalScore 0.7500
        Destination d3 = createDestination(3L, "Patagônia", "Chile", new BigDecimal("2500"), false, List.of(createDestAttr("natureza", "1.0"), createDestAttr("aventura", "1.0")));
        // Dest 4: Preço 4000 (excesso 2000 / 2000 = 1.0) -> factor 0.0 -> finalScore 0.0000
        Destination d4 = createDestination(4L, "Islândia", "Islândia", new BigDecimal("4000"), false, List.of(createDestAttr("natureza", "1.0"), createDestAttr("aventura", "1.0")));

        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(d1, d2, d3, d4));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(4, res.size());

        RecommendationResponseDTO r1 = res.stream().filter(r -> r.getDestinationId() == 1L).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1.0000"), r1.getScore());

        RecommendationResponseDTO r2 = res.stream().filter(r -> r.getDestinationId() == 2L).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1.0000"), r2.getScore());

        RecommendationResponseDTO r3 = res.stream().filter(r -> r.getDestinationId() == 3L).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.7500"), r3.getScore());

        RecommendationResponseDTO r4 = res.stream().filter(r -> r.getDestinationId() == 4L).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.0000"), r4.getScore());
    }

    // -------------------------------------------------------------------------
    // 5. Ranking e Critérios de Desempate
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Desempate: 1. FinalScore, 2. ExplicitScore, 3. Menor distância do orçamento, 4. ID do destino")
    void testTieBreakerCriteria() {
        UserProfile profile = new UserProfile();
        profile.setBudgetPerPerson(new BigDecimal("3000"));
        doReturn(Optional.of(profile)).when(userProfilePort).findByUserId(Mockito.anyLong());

        // Dois destinos com mesmo finalScore (ambos 0.5000 cold start, factor 1.0)
        // D1: Preço 2900 (|2900 - 3000| = 100)
        // D2: Preço 2500 (|2500 - 3000| = 500)
        Destination d1 = createDestination(10L, "Destino Próximo Preço", "País A", new BigDecimal("2900"), false, Collections.emptyList());
        Destination d2 = createDestination(20L, "Destino Longe Preço", "País B", new BigDecimal("2500"), false, Collections.emptyList());

        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(d2, d1));

        List<RecommendationResponseDTO> res = engine.execute(1L, 10);
        assertEquals(2, res.size());
        assertEquals(10L, res.get(0).getDestinationId(), "Destino mais próximo do orçamento (diferença de 100 vs 500) deve desempatar na frente");

        // Caso de empate total até o ID: Menor ID vence
        Destination dA = createDestination(5L, "Empate A", "País", new BigDecimal("3000"), false, Collections.emptyList());
        Destination dB = createDestination(8L, "Empate B", "País", new BigDecimal("3000"), false, Collections.emptyList());
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(dB, dA));

        List<RecommendationResponseDTO> resIds = engine.execute(1L, 10);
        assertEquals(5L, resIds.get(0).getDestinationId(), "Em empate total, menor ID numérico deve ser o primeiro");
    }

    // -------------------------------------------------------------------------
    // 6. Limite de Recomendações e Explicações Restritas ao Top 3
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Limite e Explicações: Retorna no máximo 9 itens; gera explicação apenas para o Top 3 e deixa null nos demais")
    void testLimitAndTop3ExplanationsOnly() {
        List<Destination> list = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            list.add(createDestination((long) i, "Destino " + i, "País", new BigDecimal("1000"), false, Collections.emptyList()));
        }
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(list);

        // Usuário pede limite 15 -> deve ser limitado a 9
        List<RecommendationResponseDTO> res = engine.execute(1L, 15);
        assertEquals(9, res.size(), "Deve respeitar o limite máximo estrito de 9 recomendações");

        // Top 3 possuem explicação preenchida
        assertNotNull(res.get(0).getExplanation(), "Top 1 deve ter explicação");
        assertNotNull(res.get(1).getExplanation(), "Top 2 deve ter explicação");
        assertNotNull(res.get(2).getExplanation(), "Top 3 deve ter explicação");

        // Posições 4 em diante devem ter explanation null
        for (int i = 3; i < 9; i++) {
            assertNull(res.get(i).getExplanation(), "Posição " + (i + 1) + " não deve ter explicação textual");
        }
    }

    @Test
    @DisplayName("Explicação Textual: Detecta atributos com alta compatibilidade (score >= 0.5) e penalidade orçamentária moderada")
    void testExplanationGenerationWithHighCompatAndBudgetWarning() {
        when(explicitPort.getUserProfilesByUserId(1L)).thenReturn(List.of(
                createExplicitAttr("praia", "0.9"),
                createExplicitAttr("gastronomia", "0.8")
        ));

        UserProfile profile = new UserProfile();
        profile.setBudgetPerPerson(new BigDecimal("3000"));
        doReturn(Optional.of(profile)).when(userProfilePort).findByUserId(Mockito.anyLong());

        // Preço 3300 -> excesso 300 / 3000 = 0.10 -> budgetFactor = 0.90 (>= 0.8)
        Destination d = createDestination(1L, "Salvador", "Brasil", new BigDecimal("3300"), false, List.of(
                createDestAttr("praia", "0.9"),
                createDestAttr("gastronomia", "0.8")
        ));
        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(List.of(d));

        List<RecommendationResponseDTO> res = engine.execute(1L, 5);
        assertEquals(1, res.size());
        String exp = res.get(0).getExplanation();

        assertTrue(exp.contains("Alta compatibilidade com suas preferências de praia e gastronomia"));
        assertTrue(exp.contains("Apesar de estar um pouco acima do orçamento"));
    }
}
