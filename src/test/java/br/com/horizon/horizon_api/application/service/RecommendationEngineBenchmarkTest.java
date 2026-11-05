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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationEngineBenchmarkTest {

    @Mock private UserProfileAttributePersistencePort explicitPort;
    @Mock private BehaviorProfileAttributePersistencePort behaviorPort;
    @Mock private DestinationPersistencePort destinationPort;
    @Mock private UserProfilePersistencePort userProfilePort;

    @InjectMocks private RecommendationEngine engine;

    private List<Destination> testDestinations;

    @BeforeEach
    void setUp() {
        // Dataset realista de 50 destinos com 5 dimensões cada
        testDestinations = new ArrayList<>();
        String[] attributes = {"praia", "natureza", "cultura", "gastronomia", "aventura", "luxo", "frio", "neve"};

        for (int i = 1; i <= 50; i++) {
            Destination d = new Destination();
            d.setId((long) i);
            d.setName("Destino " + i);
            d.setCountry("País " + (i % 10));
            d.setBasePrice(new BigDecimal(1000 + (i * 120)));
            d.setRequiresPassport(i % 3 == 0);

            List<DestinationAttribute> daList = new ArrayList<>();
            for (int a = 0; a < 5; a++) {
                DestinationAttribute da = new DestinationAttribute();
                da.setAttributeName(attributes[(i + a) % attributes.length]);
                da.setScore(new BigDecimal(String.format(Locale.US, "%.2f", 0.5 + ((i * 7 + a * 13) % 50) / 100.0)));
                daList.add(da);
            }
            d.setAttributes(daList);
            testDestinations.add(d);
        }

        // Perfil explícito do usuário de teste
        List<UserProfileAttribute> explicit = List.of(
                createExplicit("praia", "0.9"),
                createExplicit("gastronomia", "0.8"),
                createExplicit("cultura", "0.7")
        );
        when(explicitPort.getUserProfilesByUserId(1L)).thenReturn(explicit);

        // Perfil comportamental do usuário de teste
        List<BehaviorProfileAttribute> behavior = List.of(
                createBehavior("natureza", "0.85"),
                createBehavior("aventura", "0.75")
        );
        when(behaviorPort.getBehaviorProfilesByUserId(1L)).thenReturn(behavior);

        UserProfile profile = new UserProfile();
        profile.setUserId(1L);
        profile.setBudgetPerPerson(new BigDecimal("3500.00"));
        profile.setHasPassport(true);
        when(userProfilePort.findByUserId(1L)).thenReturn(Optional.of(profile));

        when(destinationPort.findActive(Mockito.anyInt(), Mockito.anyInt())).thenReturn(testDestinations);
    }

    private UserProfileAttribute createExplicit(String name, String score) {
        UserProfileAttribute a = new UserProfileAttribute();
        a.setAttributeName(name);
        a.setScore(new BigDecimal(score));
        return a;
    }

    private BehaviorProfileAttribute createBehavior(String name, String score) {
        BehaviorProfileAttribute a = new BehaviorProfileAttribute();
        a.setAttributeName(name);
        a.setScore(new BigDecimal(score));
        return a;
    }

    @Test
    @DisplayName("Benchmark de Performance: Medição reproduzível do RecommendationEngine (Warmup + 1.000 iterações)")
    void benchmarkRecommendationEngineExecution() {
        final int WARMUP_RUNS = 100;
        final int MEASURE_RUNS = 1000;

        // 1. Fase de Warmup (permite compilação e otimização JIT)
        for (int i = 0; i < WARMUP_RUNS; i++) {
            List<RecommendationResponseDTO> warmupResult = engine.execute(1L, 10);
            assertNotNull(warmupResult);
        }

        // 2. Fase de Medição Controlada
        double[] durationsMs = new double[MEASURE_RUNS];
        double sumMs = 0;
        double minMs = Double.MAX_VALUE;
        double maxMs = Double.MIN_VALUE;

        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            List<RecommendationResponseDTO> result = engine.execute(1L, 10);
            long end = System.nanoTime();

            assertNotNull(result);
            assertFalse(result.isEmpty());

            double elapsedMs = (end - start) / 1_000_000.0;
            durationsMs[i] = elapsedMs;
            sumMs += elapsedMs;
            if (elapsedMs < minMs) minMs = elapsedMs;
            if (elapsedMs > maxMs) maxMs = elapsedMs;
        }

        double meanMs = sumMs / MEASURE_RUNS;

        // Cálculo do Desvio Padrão
        double varianceSum = 0;
        for (double d : durationsMs) {
            varianceSum += Math.pow(d - meanMs, 2);
        }
        double stdDevMs = Math.sqrt(varianceSum / MEASURE_RUNS);
        double opsPerSec = 1000.0 / (meanMs > 0 ? meanMs : 0.001);

        System.out.println("==========================================================");
        System.out.println("HORIZON RECOMMENDATION ENGINE - BENCHMARK DE PERFORMANCE");
        System.out.println("==========================================================");
        System.out.printf(Locale.US, "Ambiente: Java %s (%s) / %s (%s)%n",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"));
        System.out.printf("Processadores disponíveis: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.printf("Total de destinos avaliados por execução: %d%n", testDestinations.size());
        System.out.printf("Número de execuções mensuradas: %d%n", MEASURE_RUNS);
        System.out.printf(Locale.US, "Tempo Mínimo: %.4f ms%n", minMs);
        System.out.printf(Locale.US, "Tempo Máximo: %.4f ms%n", maxMs);
        System.out.printf(Locale.US, "Tempo Médio:  %.4f ms%n", meanMs);
        System.out.printf(Locale.US, "Desvio Padrão: %.4f ms%n", stdDevMs);
        System.out.printf(Locale.US, "Throughput Estimado: %.0f operações/segundo%n", opsPerSec);
        System.out.println("==========================================================");

        // Asserções objetivas de sanidade:
        assertTrue(meanMs < 50.0, "O tempo médio de recomendação em memória deve ser inferior a 50ms");
        assertTrue(minMs > 0.0, "O tempo mínimo deve ser positivo");
    }
}
