package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.*;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TourismType;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final AttributeRepository attributeRepository;
    private final DestinationAttributeRepository destinationAttributeRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FlightRepository flightRepository;
    private final FlightAvailabilityRepository flightAvailabilityRepository;
    private final PostRepository postRepository;
    private final PostAttributeRepository postAttributeRepository;
    private final AnswerAttributeRepository answerAttributeRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Map<String, AttributeEntity> attributes = new HashMap<>();

    private static final Set<String> PASSPORT_FREE_COUNTRIES = Set.of(
        "brasil", "argentina", "chile", "uruguai", "paraguai", "peru", "colombia", "colômbia", "bolivia", "bolívia", "equador"
    );

    /**
     * Coordenadas geográficas (lat, lon) de cada destino, usadas pela integração Open-Meteo.
     * Para regiões amplas (ex.: Jalapão, Pantanal, Patagônia) usa-se a cidade-base turística.
     */
    private static final Map<String, double[]> COORDINATES = Map.ofEntries(
        Map.entry("Roma", new double[]{41.9028, 12.4964}),
        Map.entry("Paris", new double[]{48.8566, 2.3522}),
        Map.entry("Atenas", new double[]{37.9838, 23.7275}),
        Map.entry("Istambul", new double[]{41.0082, 28.9784}),
        Map.entry("Quioto", new double[]{35.0116, 135.7681}),
        Map.entry("Londres", new double[]{51.5074, -0.1278}),
        Map.entry("Praga", new double[]{50.0755, 14.4378}),
        Map.entry("Lisboa", new double[]{38.7223, -9.1393}),
        Map.entry("Amsterdã", new double[]{52.3676, 4.9041}),
        Map.entry("Barcelona", new double[]{41.3851, 2.1734}),
        Map.entry("Veneza", new double[]{45.4408, 12.3155}),
        Map.entry("Florença", new double[]{43.7696, 11.2558}),
        Map.entry("Gramado", new double[]{-29.3787, -50.8737}),
        Map.entry("Nova York", new double[]{40.7128, -74.0060}),
        Map.entry("Tóquio", new double[]{35.6762, 139.6503}),
        Map.entry("São Paulo", new double[]{-23.5505, -46.6333}),
        Map.entry("Las Vegas", new double[]{36.1699, -115.1398}),
        Map.entry("Seul", new double[]{37.5665, 126.9780}),
        Map.entry("Madri", new double[]{40.4168, -3.7038}),
        Map.entry("Sydney", new double[]{-33.8688, 151.2093}),
        Map.entry("Toronto", new double[]{43.6532, -79.3832}),
        Map.entry("Bangkok", new double[]{13.7563, 100.5018}),
        Map.entry("Bogotá", new double[]{4.7110, -74.0721}),
        Map.entry("Cancún", new double[]{21.1619, -86.8515}),
        Map.entry("Maldivas", new double[]{4.1755, 73.5093}),
        Map.entry("Punta Cana", new double[]{18.5601, -68.3725}),
        Map.entry("Rio de Janeiro", new double[]{-22.9068, -43.1729}),
        Map.entry("Santorini", new double[]{36.3932, 25.4615}),
        Map.entry("Dubai", new double[]{25.2048, 55.2708}),
        Map.entry("Patagônia", new double[]{-50.3379, -72.2648}),
        Map.entry("Machu Picchu", new double[]{-13.1631, -72.5450}),
        Map.entry("Alpes Suíços", new double[]{46.0207, 7.7491}),
        Map.entry("Banff", new double[]{51.1784, -115.5708}),
        Map.entry("Antártida", new double[]{-64.8251, -63.4960}),
        Map.entry("Bariloche", new double[]{-41.1335, -71.3103}),
        Map.entry("Serengeti", new double[]{-2.3333, 34.8333}),
        Map.entry("Amazônia", new double[]{-3.1190, -60.0217}),
        Map.entry("Galápagos", new double[]{-0.7393, -90.3305}),
        Map.entry("Parque Nacional Kruger", new double[]{-23.9884, 31.5547}),
        Map.entry("Grande Barreira de Corais", new double[]{-16.9186, 145.7781}),
        Map.entry("Reykjavík", new double[]{64.1466, -21.9426}),
        Map.entry("Fernando de Noronha", new double[]{-3.8547, -32.4247}),
        Map.entry("Atacama", new double[]{-22.9087, -68.1997}),
        Map.entry("Bonito", new double[]{-21.1261, -56.4836}),
        Map.entry("Chapada Diamantina", new double[]{-12.5626, -41.3896}),
        Map.entry("Jalapão", new double[]{-10.5469, -46.4167}),
        Map.entry("Pantanal", new double[]{-16.2567, -56.6228}),
        Map.entry("Foz do Iguaçu", new double[]{-25.5163, -54.5854}),
        Map.entry("Costa Rica", new double[]{10.4679, -84.6427}),
        Map.entry("Yellowstone", new double[]{44.4280, -110.5885})
    );

    @Override
    public void run(String... args) {
        log.info("=== INICIANDO DATASEEDER HORIZON OTIMIZADO ===");
        try {
            createPostOptionTablesIfNotExist();
            updateQuizResponsesConstraints();
        } catch (Exception e) {
            log.warn("Tabelas post_options: {}", e.getMessage());
        }

        try {
            seedAttributes();
            log.info("Atributos prontos: {}", attributes.size());
        } catch (Exception e) {
            log.error("Erro atributos: {}", e.getMessage());
        }

        try {
            seedDestinations();
            log.info("Destinos prontos no banco: {}", destinationRepository.count());
        } catch (Exception e) {
            log.error("Erro destinos: {}", e.getMessage());
        }

        try {
            seedQuiz();
            log.info("Quiz pronto.");
        } catch (Exception e) {
            log.error("Erro quiz: {}", e.getMessage());
        }

        try {
            seedPosts();
            log.info("Posts prontos no banco: {}", postRepository.count());
        } catch (Exception e) {
            log.error("Erro posts: {}", e.getMessage());
        }

        try {
            seedFlights();
            log.info("Voos prontos no banco: {}", flightRepository.count());
        } catch (Exception e) {
            log.error("Erro voos: {}", e.getMessage());
        }

        log.info("=== DATASEEDER FINALIZADO COM SUCESSO ===");
    }

    private void createPostOptionTablesIfNotExist() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS post_options (
                id BIGSERIAL PRIMARY KEY,
                post_id BIGINT NOT NULL,
                label VARCHAR(255) NOT NULL,
                image_url TEXT,
                votes_count INT DEFAULT 0
            )
        """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS post_option_attributes (
                option_id BIGINT NOT NULL,
                attribute_id BIGINT NOT NULL,
                weight NUMERIC(3,2) NOT NULL,
                PRIMARY KEY(option_id, attribute_id)
            )
        """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS support_tickets (
                id BIGSERIAL PRIMARY KEY,
                ticket_code VARCHAR(50) NOT NULL UNIQUE,
                user_id BIGINT,
                user_name VARCHAR(150) NOT NULL,
                user_email VARCHAR(150) NOT NULL,
                subject VARCHAR(200) NOT NULL,
                sla_hours INT NOT NULL,
                sla_description VARCHAR(100) NOT NULL,
                description TEXT NOT NULL,
                attachment_name TEXT,
                attachment_size BIGINT,
                attachment_type VARCHAR(255),
                attachment_data TEXT,
                status VARCHAR(30) NOT NULL DEFAULT 'EM_ATENDIMENTO',
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
        """);
        try {
            jdbcTemplate.execute("ALTER TABLE support_tickets ALTER COLUMN attachment_name TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE support_tickets ALTER COLUMN attachment_data TYPE TEXT");
            jdbcTemplate.execute("ALTER TABLE support_tickets ALTER COLUMN attachment_type TYPE VARCHAR(255)");
        } catch (Exception e) {
            log.debug("Colunas de anexos já ajustadas em support_tickets: {}", e.getMessage());
        }
    }

    private void updateQuizResponsesConstraints() {
        try {
            // Drop explicit constraints known to exist or generated by Hibernate
            jdbcTemplate.execute("ALTER TABLE quiz_responses DROP CONSTRAINT IF EXISTS uk87ho3lf713ijr9pjn7ahvci9t");
            jdbcTemplate.execute("ALTER TABLE quiz_responses DROP CONSTRAINT IF EXISTS quiz_responses_user_id_attempt_id_question_id_key");
            jdbcTemplate.execute("DROP INDEX IF EXISTS uk87ho3lf713ijr9pjn7ahvci9t");
            jdbcTemplate.execute("DROP INDEX IF EXISTS quiz_responses_user_id_attempt_id_question_id_key");

            // Drop any legacy unique constraint on quiz_responses that does not include answer_id
            jdbcTemplate.execute("""
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (
                        SELECT conname
                        FROM pg_constraint
                        WHERE conrelid = 'quiz_responses'::regclass
                          AND contype = 'u'
                          AND conname NOT LIKE '%answer%'
                    ) LOOP
                        EXECUTE 'ALTER TABLE quiz_responses DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
                    END LOOP;
                END $$;
            """);

            // Drop any legacy unique index indexing question_id without answer_id
            jdbcTemplate.execute("""
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (
                        SELECT indexname
                        FROM pg_indexes
                        WHERE tablename = 'quiz_responses'
                          AND indexname NOT LIKE '%pkey'
                          AND indexname NOT LIKE '%answer%'
                          AND indexdef LIKE '%question_id%'
                          AND indexdef NOT LIKE '%answer_id%'
                    ) LOOP
                        EXECUTE 'DROP INDEX IF EXISTS ' || quote_ident(r.indexname);
                    END LOOP;
                END $$;
            """);

            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS quiz_responses_user_attempt_question_answer_idx ON quiz_responses (user_id, attempt_id, question_id, answer_id)");
            log.info("Constraints e índices legados de quiz_responses removidos com sucesso.");
        } catch (Exception e) {
            log.warn("Aviso ao atualizar constraints de quiz_responses: {}", e.getMessage());
        }
    }

    private void seedAttributes() {
        Map<String, AttributeEntity> existing = attributeRepository.findAll().stream()
                .collect(Collectors.toMap(AttributeEntity::getName, a -> a, (a, b) -> a));

        String[] attrNames = {
            "FRIO", "NEVE", "VIDA_SELVAGEM", "ECOTURISMO", "NATUREZA", "PRAIA", "CALOR", 
            "MERGULHO", "RELAXAMENTO", "GASTRONOMIA", "CULTURA", "HISTORIA", "VIDA_NOTURNA", 
            "URBANO", "AVENTURA", "ROMANTICO", "MONTANHA", "LUXO"
        };
        for (String name : attrNames) {
            AttributeEntity a = existing.get(name);
            if (a == null) {
                a = new AttributeEntity();
                a.setName(name);
                a.setDescription("Atributo: " + name);
                a = attributeRepository.save(a);
            }
            attributes.put(name, a);
        }
    }

    private record DestSeed(String city, String country, TourismType type, String imageUrl, double price, Map<String, Double> tags) {}

    private void seedDestinations() {
        List<DestSeed> catalog = List.of(
            // COMUM - Cidades Históricas, Românticas e Culturais
            new DestSeed("Roma", "Itália", TourismType.COMUM, "assets/roma.webp", 5200, Map.of("HISTORIA", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 1.0, "ROMANTICO", 0.8)),
            new DestSeed("Paris", "França", TourismType.COMUM, "assets/paris.webp", 5800, Map.of("ROMANTICO", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 0.9, "HISTORIA", 0.8)),
            new DestSeed("Atenas", "Grécia", TourismType.COMUM, "assets/atenas.webp", 4900, Map.of("HISTORIA", 1.0, "CULTURA", 1.0, "CALOR", 0.7)),
            new DestSeed("Istambul", "Turquia", TourismType.COMUM, "assets/istambul.webp", 4600, Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "GASTRONOMIA", 0.9)),
            new DestSeed("Quioto", "Japão", TourismType.COMUM, "assets/quioto.webp", 6400, Map.of("CULTURA", 1.0, "HISTORIA", 0.9, "RELAXAMENTO", 0.8, "NATUREZA", 0.7)),
            new DestSeed("Londres", "Reino Unido", TourismType.COMUM, "assets/londres.webp", 5600, Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "URBANO", 1.0, "VIDA_NOTURNA", 0.8)),
            new DestSeed("Praga", "República Tcheca", TourismType.COMUM, "assets/praga.webp", 4500, Map.of("HISTORIA", 1.0, "ROMANTICO", 0.9, "CULTURA", 0.9)),
            new DestSeed("Lisboa", "Portugal", TourismType.COMUM, "assets/lisboa.webp", 4800, Map.of("HISTORIA", 0.9, "GASTRONOMIA", 1.0, "CULTURA", 0.9, "CALOR", 0.7)),
            new DestSeed("Amsterdã", "Holanda", TourismType.COMUM, "https://images.unsplash.com/photo-1512470876302-972faa2aa9a4?w=800&q=80&auto=format", 5400, Map.of("CULTURA", 1.0, "URBANO", 0.9, "HISTORIA", 0.8, "ROMANTICO", 0.8)),
            new DestSeed("Barcelona", "Espanha", TourismType.COMUM, "assets/barcelona.webp", 5100, Map.of("CULTURA", 1.0, "GASTRONOMIA", 1.0, "PRAIA", 0.8, "VIDA_NOTURNA", 0.9)),
            new DestSeed("Veneza", "Itália", TourismType.COMUM, "assets/veneza.webp", 5500, Map.of("ROMANTICO", 1.0, "HISTORIA", 1.0, "CULTURA", 0.9)),
            new DestSeed("Florença", "Itália", TourismType.COMUM, "https://images.unsplash.com/photo-1543429776-2782fc8e1acd?w=800&q=80&auto=format", 5300, Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "GASTRONOMIA", 0.9, "ROMANTICO", 0.9)),
            new DestSeed("Gramado", "Brasil", TourismType.COMUM, "assets/gramado.webp", 890, Map.of("ROMANTICO", 1.0, "GASTRONOMIA", 1.0, "FRIO", 0.7)),

            // COMUM - Grandes Metrópoles / Vida Noturna
            new DestSeed("Nova York", "EUA", TourismType.COMUM, "assets/nova-york.webp", 6100, Map.of("URBANO", 1.0, "VIDA_NOTURNA", 1.0, "CULTURA", 0.9, "GASTRONOMIA", 0.8)),
            new DestSeed("Tóquio", "Japão", TourismType.COMUM, "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800&q=80&auto=format", 6700, Map.of("URBANO", 1.0, "VIDA_NOTURNA", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 1.0)),
            new DestSeed("Las Vegas", "EUA", TourismType.COMUM, "assets/las-vegas.webp", 5300, Map.of("VIDA_NOTURNA", 1.0, "URBANO", 0.9, "LUXO", 0.8)),
            new DestSeed("Seul", "Coreia do Sul", TourismType.COMUM, "assets/seul.webp", 6200, Map.of("URBANO", 1.0, "VIDA_NOTURNA", 1.0, "GASTRONOMIA", 0.9)),
            new DestSeed("Madri", "Espanha", TourismType.COMUM, "assets/madri.webp", 4900, Map.of("URBANO", 0.9, "VIDA_NOTURNA", 1.0, "GASTRONOMIA", 1.0, "CULTURA", 0.9)),
            new DestSeed("Sydney", "Austrália", TourismType.COMUM, "assets/sydney.webp", 6800, Map.of("URBANO", 0.9, "PRAIA", 0.9, "CALOR", 0.8, "CULTURA", 0.8)),
            new DestSeed("Toronto", "Canadá", TourismType.COMUM, "assets/toronto.webp", 5200, Map.of("URBANO", 1.0, "CULTURA", 0.9, "GASTRONOMIA", 0.8)),
            new DestSeed("Bangkok", "Tailândia", TourismType.COMUM, "assets/bangkok.webp", 4700, Map.of("CULTURA", 1.0, "GASTRONOMIA", 1.0, "URBANO", 0.9, "CALOR", 0.9)),
            new DestSeed("Bogotá", "Colômbia", TourismType.COMUM, "https://images.unsplash.com/photo-1538688525198-9b88f6f53126?w=800&q=80&auto=format", 3400, Map.of("CULTURA", 0.9, "HISTORIA", 0.8, "GASTRONOMIA", 0.8, "URBANO", 0.8)),

            // COMUM - Praias, Relaxamento e Sol
            new DestSeed("Cancún", "México", TourismType.COMUM, "https://images.unsplash.com/photo-1510097467424-192d713fd8b2?w=800&q=80&auto=format", 4300, Map.of("PRAIA", 1.0, "CALOR", 1.0, "VIDA_NOTURNA", 0.9, "RELAXAMENTO", 0.8)),
            new DestSeed("Maldivas", "Maldivas", TourismType.COMUM, "assets/maldivas.webp", 8500, Map.of("PRAIA", 1.0, "CALOR", 1.0, "RELAXAMENTO", 1.0, "LUXO", 1.0, "ROMANTICO", 1.0)),
            new DestSeed("Punta Cana", "República Dominicana", TourismType.COMUM, "assets/punta-cana.webp", 4600, Map.of("PRAIA", 1.0, "CALOR", 1.0, "RELAXAMENTO", 1.0)),
            new DestSeed("Rio de Janeiro", "Brasil", TourismType.COMUM, "assets/rio.webp", 480, Map.of("PRAIA", 1.0, "CALOR", 1.0, "NATUREZA", 0.8, "VIDA_NOTURNA", 0.8)),
            new DestSeed("Santorini", "Grécia", TourismType.COMUM, "assets/santorini.webp", 6300, Map.of("PRAIA", 0.8, "CALOR", 0.9, "ROMANTICO", 1.0, "RELAXAMENTO", 0.9)),
            new DestSeed("Dubai", "Emirados Árabes", TourismType.COMUM, "assets/dubai.webp", 7100, Map.of("URBANO", 1.0, "LUXO", 1.0, "CALOR", 1.0)),

            // ECOTURISMO - Montanhas, Neve e Frio
            new DestSeed("Patagônia", "Argentina", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1527004013197-933c4bb611b3?w=800&q=80&auto=format", 4900, Map.of("FRIO", 1.0, "NEVE", 0.9, "MONTANHA", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "ECOTURISMO", 1.0)),
            new DestSeed("Machu Picchu", "Peru", TourismType.ECOTURISMO, "assets/machu-picchu.webp", 3900, Map.of("HISTORIA", 1.0, "CULTURA", 1.0, "MONTANHA", 1.0, "AVENTURA", 0.9, "ECOTURISMO", 1.0)),
            new DestSeed("Alpes Suíços", "Suíça", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?w=800&q=80&auto=format", 7500, Map.of("FRIO", 1.0, "NEVE", 1.0, "MONTANHA", 1.0, "LUXO", 0.8, "NATUREZA", 1.0, "ECOTURISMO", 1.0)),
            new DestSeed("Banff", "Canadá", TourismType.ECOTURISMO, "assets/banff.webp", 5800, Map.of("FRIO", 1.0, "NEVE", 1.0, "MONTANHA", 1.0, "NATUREZA", 1.0, "ECOTURISMO", 1.0)),
            new DestSeed("Antártida", "Antártida", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1516245834210-c4c142787335?w=800&q=80&auto=format", 9900, Map.of("FRIO", 1.0, "NEVE", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 1.0, "ECOTURISMO", 1.0)),
            new DestSeed("Bariloche", "Argentina", TourismType.ECOTURISMO, "assets/bariloche.webp", 4200, Map.of("FRIO", 1.0, "NEVE", 1.0, "MONTANHA", 0.9, "GASTRONOMIA", 0.8, "ECOTURISMO", 0.9)),

            // ECOTURISMO - Vida Selvagem, Florestas e Águas Naturais
            new DestSeed("Serengeti", "Tanzânia", TourismType.ECOTURISMO, "assets/serengeti.webp", 7200, Map.of("VIDA_SELVAGEM", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "CALOR", 0.8, "ECOTURISMO", 1.0)),
            new DestSeed("Amazônia", "Brasil", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=800&q=80&auto=format", 1250, Map.of("NATUREZA", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 1.0, "CALOR", 1.0, "ECOTURISMO", 1.0)),
            new DestSeed("Galápagos", "Equador", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1568214379698-8aeb8c6c6ac8?w=800&q=80&auto=format", 6800, Map.of("VIDA_SELVAGEM", 1.0, "NATUREZA", 1.0, "MERGULHO", 1.0, "CALOR", 0.8, "ECOTURISMO", 1.0)),
            new DestSeed("Parque Nacional Kruger", "África do Sul", TourismType.ECOTURISMO, "assets/kruger.webp", 6500, Map.of("VIDA_SELVAGEM", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "ECOTURISMO", 1.0)),
            new DestSeed("Grande Barreira de Corais", "Austrália", TourismType.ECOTURISMO, "assets/grande-barreira-de-corais.webp", 7100, Map.of("NATUREZA", 1.0, "MERGULHO", 1.0, "VIDA_SELVAGEM", 1.0, "PRAIA", 0.8, "ECOTURISMO", 1.0)),
            new DestSeed("Reykjavík", "Islândia", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1476610282020-f1361c47db8b?w=800&q=80&auto=format", 6600, Map.of("FRIO", 1.0, "NEVE", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.9, "ECOTURISMO", 1.0)),
            new DestSeed("Fernando de Noronha", "Brasil", TourismType.ECOTURISMO, "assets/noronha.webp", 1950, Map.of("PRAIA", 1.0, "NATUREZA", 1.0, "MERGULHO", 1.0, "RELAXAMENTO", 0.9, "ECOTURISMO", 1.0)),
            new DestSeed("Atacama", "Chile", TourismType.ECOTURISMO, "assets/atacama.webp", 4400, Map.of("NATUREZA", 1.0, "AVENTURA", 1.0, "FRIO", 0.6, "CALOR", 0.6, "ECOTURISMO", 1.0)),
            new DestSeed("Bonito", "Brasil", TourismType.ECOTURISMO, "assets/bonito.webp", 980, Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "MERGULHO", 1.0, "AVENTURA", 0.8)),
            new DestSeed("Chapada Diamantina", "Brasil", TourismType.ECOTURISMO, "assets/chapada-diamantina.webp", 1050, Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "MONTANHA", 0.8)),
            new DestSeed("Jalapão", "Brasil", TourismType.ECOTURISMO, "https://upload.wikimedia.org/wikipedia/commons/6/63/Fervedouro_Jalap%C3%A3o.jpg", 1150, Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "CALOR", 0.9)),
            new DestSeed("Pantanal", "Brasil", TourismType.ECOTURISMO, "assets/pantanal.webp", 1100, Map.of("VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.9)),
            new DestSeed("Foz do Iguaçu", "Brasil", TourismType.ECOTURISMO, "https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f?w=800&q=80&auto=format", 780, Map.of("NATUREZA", 1.0, "ECOTURISMO", 1.0, "AVENTURA", 0.8)),
            new DestSeed("Costa Rica", "Costa Rica", TourismType.ECOTURISMO, "assets/costa-rica.webp", 5100, Map.of("NATUREZA", 1.0, "ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 0.9)),
            new DestSeed("Yellowstone", "EUA", TourismType.ECOTURISMO, "assets/yellowstone.webp", 6200, Map.of("NATUREZA", 1.0, "VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "AVENTURA", 0.8))
        );

        Map<String, DestinationEntity> existingByName = destinationRepository.findAll().stream()
                .collect(Collectors.toMap(DestinationEntity::getName, d -> d, (a, b) -> a));

        Set<String> validCityNames = catalog.stream().map(DestSeed::city).collect(Collectors.toSet());
        for (DestinationEntity dest : existingByName.values()) {
            if (!validCityNames.contains(dest.getName()) && Boolean.TRUE.equals(dest.getActive())) {
                dest.setActive(false);
                destinationRepository.save(dest);
            }
        }

        List<DestinationEntity> toSave = new ArrayList<>();
        for (DestSeed ds : catalog) {
            DestinationEntity d = existingByName.get(ds.city());
            if (d == null) {
                d = new DestinationEntity();
                d.setName(ds.city());
                d.setCity(ds.city());
                d.setCreatedAt(OffsetDateTime.now());
            }
            d.setCountry(ds.country());
            boolean noPassport = ds.country() != null && PASSPORT_FREE_COUNTRIES.contains(ds.country().trim().toLowerCase());
            d.setRequiresPassport(!noPassport);
            d.setDescription("Descubra as maravilhas, paisagens e cultura de " + ds.city() + ", " + ds.country() + ".");
            d.setTourismType(ds.type());
            d.setImageUrl(ds.imageUrl());
            d.setBasePrice(BigDecimal.valueOf(ds.price()));
            double[] coords = COORDINATES.get(ds.city());
            if (coords != null) {
                d.setLatitude(BigDecimal.valueOf(coords[0]));
                d.setLongitude(BigDecimal.valueOf(coords[1]));
            } else {
                log.warn("Destino '{}' sem coordenadas cadastradas: clima indisponível.", ds.city());
            }
            d.setActive(true);
            toSave.add(d);
        }
        List<DestinationEntity> savedDestinations = destinationRepository.saveAll(toSave);
        Map<String, DestinationEntity> savedMap = savedDestinations.stream()
                .collect(Collectors.toMap(DestinationEntity::getName, d -> d, (a, b) -> a));

        Map<String, DestinationAttributeEntity> existingAttrMap = destinationAttributeRepository.findAll().stream()
                .collect(Collectors.toMap(
                    da -> da.getId().getDestinationId() + ":" + da.getId().getAttributeId(),
                    da -> da,
                    (a, b) -> a
                ));

        List<DestinationAttributeEntity> attrToSave = new ArrayList<>();
        for (DestSeed ds : catalog) {
            DestinationEntity dest = savedMap.get(ds.city());
            if (dest == null) continue;
            for (Map.Entry<String, Double> entry : ds.tags().entrySet()) {
                AttributeEntity attr = attributes.get(entry.getKey());
                if (attr == null) continue;
                String key = dest.getId() + ":" + attr.getId();
                DestinationAttributeEntity da = existingAttrMap.get(key);
                if (da == null) {
                    da = new DestinationAttributeEntity();
                    da.getId().setDestinationId(dest.getId());
                    da.getId().setAttributeId(attr.getId());
                    da.setDestination(dest);
                    da.setAttribute(attr);
                }
                da.setScore(BigDecimal.valueOf(entry.getValue()));
                attrToSave.add(da);
            }
        }
        destinationAttributeRepository.saveAll(attrToSave);
    }

    private void seedQuiz() {
        final String quizTitle = "Perfil de Viajante Horizon";
        QuizEntity quiz = quizRepository.findAll().stream()
                .filter(q -> q.getVersion() != null && q.getVersion() == 1)
                .findFirst()
                .orElseGet(() -> {
                    QuizEntity q = new QuizEntity();
                    q.setTitle(quizTitle);
                    q.setVersion(1);
                    q.setCreatedAt(OffsetDateTime.now());
                    return q;
                });

        quiz.setTitle(quizTitle);
        quiz.setActive(true);
        final QuizEntity savedQuiz = quizRepository.save(quiz);

        List<QuestionEntity> existingQuestions = questionRepository.findAll().stream()
                .filter(q -> q.getQuiz() != null && q.getQuiz().getId().equals(savedQuiz.getId()))
                .toList();

        // PERGUNTA 1 - OBRIGATÓRIA (questionOrder = 1)
        QuestionEntity q1 = existingQuestions.stream()
                .filter(q -> Integer.valueOf(1).equals(q.getQuestionOrder()))
                .findFirst()
                .orElseGet(() -> {
                    QuestionEntity q = new QuestionEntity();
                    q.setQuiz(savedQuiz);
                    q.setQuestionOrder(1);
                    return q;
                });
        q1.setQuiz(savedQuiz);
        q1.setQuestionOrder(1);
        q1.setQuestionText("Qual o estilo de viagem você deseja realizar?");
        q1.setActive(true);
        q1 = questionRepository.save(q1);

        createAnswer(q1, "Ecoturismo", Map.of("ECOTURISMO", 1.0, "NATUREZA", 0.8));
        createAnswer(q1, "Viagem tradicional", Map.of("URBANO", 0.8, "CULTURA", 0.8));
        cleanupObsoleteAnswers(q1, List.of("Ecoturismo", "Viagem tradicional"));

        // PERGUNTA 2 - CLIMA (questionOrder = 2)
        QuestionEntity q2 = existingQuestions.stream()
                .filter(q -> Integer.valueOf(2).equals(q.getQuestionOrder()))
                .findFirst()
                .orElseGet(() -> {
                    QuestionEntity q = new QuestionEntity();
                    q.setQuiz(savedQuiz);
                    q.setQuestionOrder(2);
                    return q;
                });
        q2.setQuiz(savedQuiz);
        q2.setQuestionOrder(2);
        q2.setQuestionText("Qual o clima perfeito para sua viagem?");
        q2.setActive(true);
        q2 = questionRepository.save(q2);

        createAnswer(q2, "Sol e calor", Map.of("CALOR", 1.0, "PRAIA", 0.8));
        createAnswer(q2, "Neve e frio", Map.of("FRIO", 1.0, "NEVE", 1.0));
        createAnswer(q2, "Clima ameno e fresco", Map.of("FRIO", 0.4, "CALOR", 0.3));
        createAnswer(q2, "Qualquer clima", Map.of());
        cleanupObsoleteAnswers(q2, List.of("Sol e calor", "Neve e frio", "Clima ameno e fresco", "Qualquer clima"));

        // PERGUNTA 3 - EXPERIÊNCIA (questionOrder = 3)
        QuestionEntity q3 = existingQuestions.stream()
                .filter(q -> Integer.valueOf(3).equals(q.getQuestionOrder()))
                .findFirst()
                .orElseGet(() -> {
                    QuestionEntity q = new QuestionEntity();
                    q.setQuiz(savedQuiz);
                    q.setQuestionOrder(3);
                    return q;
                });
        q3.setQuiz(savedQuiz);
        q3.setQuestionOrder(3);
        q3.setQuestionText("Que tipo de experiência você mais procura?");
        q3.setActive(true);
        q3 = questionRepository.save(q3);

        createAnswer(q3, "Natureza e vida selvagem", Map.of("NATUREZA", 1.0, "VIDA_SELVAGEM", 1.0));
        createAnswer(q3, "Cultura e história", Map.of("CULTURA", 1.0, "HISTORIA", 1.0));
        createAnswer(q3, "Gastronomia e vida urbana", Map.of("GASTRONOMIA", 1.0, "URBANO", 1.0));
        createAnswer(q3, "Aventura", Map.of("AVENTURA", 1.0, "MONTANHA", 0.8));
        cleanupObsoleteAnswers(q3, List.of("Natureza e vida selvagem", "Cultura e história", "Gastronomia e vida urbana", "Aventura"));

        // PERGUNTA 4 - RITMO (questionOrder = 4)
        QuestionEntity q4 = existingQuestions.stream()
                .filter(q -> Integer.valueOf(4).equals(q.getQuestionOrder()))
                .findFirst()
                .orElseGet(() -> {
                    QuestionEntity q = new QuestionEntity();
                    q.setQuiz(savedQuiz);
                    q.setQuestionOrder(4);
                    return q;
                });
        q4.setQuiz(savedQuiz);
        q4.setQuestionOrder(4);
        q4.setQuestionText("Qual o ritmo ideal para seus dias de viagem?");
        q4.setActive(true);
        q4 = questionRepository.save(q4);

        createAnswer(q4, "Desconexão e descanso", Map.of("RELAXAMENTO", 1.0, "PRAIA", 0.7));
        createAnswer(q4, "Explorador de atrações", Map.of("HISTORIA", 1.0, "CULTURA", 0.9, "URBANO", 0.6));
        createAnswer(q4, "Vida noturna e agito", Map.of("VIDA_NOTURNA", 1.0, "URBANO", 0.8));
        createAnswer(q4, "Mergulho e vida marinha", Map.of("MERGULHO", 1.0, "VIDA_SELVAGEM", 0.9, "ECOTURISMO", 0.8));
        cleanupObsoleteAnswers(q4, List.of("Desconexão e descanso", "Explorador de atrações", "Vida noturna e agito", "Mergulho e vida marinha"));

        // PERGUNTA 5 - ATMOSFERA (questionOrder = 5)
        QuestionEntity q5 = existingQuestions.stream()
                .filter(q -> Integer.valueOf(5).equals(q.getQuestionOrder()))
                .findFirst()
                .orElseGet(() -> {
                    QuestionEntity q = new QuestionEntity();
                    q.setQuiz(savedQuiz);
                    q.setQuestionOrder(5);
                    return q;
                });
        q5.setQuiz(savedQuiz);
        q5.setQuestionOrder(5);
        q5.setQuestionText("Qual atmosfera mais combina com o que você busca?");
        q5.setActive(true);
        q5 = questionRepository.save(q5);

        createAnswer(q5, "Romântica a dois", Map.of("ROMANTICO", 1.0, "CULTURA", 0.5));
        createAnswer(q5, "Exclusiva e sofisticada", Map.of("LUXO", 1.0, "RELAXAMENTO", 0.7));
        createAnswer(q5, "Natureza autêntica", Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.7));
        createAnswer(q5, "Grandes metrópoles", Map.of("URBANO", 1.0, "CULTURA", 0.7, "VIDA_NOTURNA", 0.5));
        cleanupObsoleteAnswers(q5, List.of("Romântica a dois", "Exclusiva e sofisticada", "Natureza autêntica", "Grandes metrópoles"));
    }

    private void cleanupObsoleteAnswers(QuestionEntity q, List<String> validAnswerTexts) {
        List<AnswerEntity> currentAnswers = answerRepository.findByQuestion_Id(q.getId());
        for (AnswerEntity a : currentAnswers) {
            boolean isValid = validAnswerTexts.stream().anyMatch(v -> v.equalsIgnoreCase(a.getAnswerText()));
            if (!isValid) {
                log.info("Limpando resposta obsoleta/corrompida do quiz: id={}, text='{}'", a.getId(), a.getAnswerText());
                try {
                    jdbcTemplate.update("DELETE FROM user_quiz_responses WHERE answer_id = ?", a.getId());
                    answerRepository.delete(a);
                } catch (Exception e) {
                    log.warn("Não foi possível excluir resposta obsoleta id={}: {}", a.getId(), e.getMessage());
                }
            }
        }
    }

    private void createAnswer(QuestionEntity q, String text, Map<String, Double> attrScores) {
        AnswerEntity ans = answerRepository.findByQuestion_Id(q.getId()).stream()
                .filter(a -> a.getAnswerText().equalsIgnoreCase(text))
                .findFirst()
                .orElseGet(() -> {
                    AnswerEntity a = new AnswerEntity();
                    a.setQuestion(q);
                    a.setAnswerText(text);
                    return answerRepository.save(a);
                });
        ans.setAnswerText(text);
        final AnswerEntity savedAns = answerRepository.save(ans);
        
        List<AnswerAttributeEntity> existingAnswerAttrs = answerAttributeRepository.findAll();
        for (Map.Entry<String, Double> entry : attrScores.entrySet()) {
            AttributeEntity attr = attributes.get(entry.getKey());
            if (attr != null) {
                AnswerAttributeEntity aa = existingAnswerAttrs.stream()
                        .filter(x -> x.getId().getAnswerId().equals(savedAns.getId()) && x.getId().getAttributeId().equals(attr.getId()))
                        .findFirst()
                        .orElseGet(() -> {
                            AnswerAttributeEntity newAa = new AnswerAttributeEntity();
                            newAa.getId().setAnswerId(savedAns.getId());
                            newAa.getId().setAttributeId(attr.getId());
                            newAa.setAnswer(savedAns);
                            newAa.setAttribute(attr);
                            return newAa;
                        });
                aa.setWeight(BigDecimal.valueOf(entry.getValue()));
                answerAttributeRepository.save(aa);
            }
        }
    }

    private void seedPosts() {
        if (postRepository.count() >= 30) {
            return;
        }
        // 1. Planejamento de Viagem Sozinho
        upsertPost(
            "A arte de viajar sozinho: Desbravando o mundo na sua própria companhia",
            "Planejar uma viagem solo pode parecer assustador no começo, mas é uma das experiências mais libertadoras que você pode ter. Não se trata de solidão, mas de autonomia absoluta. Comece escolhendo destinos amigáveis para 'solo travelers', como a Nova Zelândia, Islândia ou até mesmo roteiros pelo sul do Brasil. A dica de ouro é: monte um roteiro flexível. Reserve apenas os voos e a primeira ou segunda noite de hospedagem. Deixe o resto em aberto para que o destino te surpreenda e você possa mudar de ideia ao conhecer outros viajantes. Faça amizades em hostels, permita-se almoçar num restaurante chique na sua própria companhia e lembre-se: quando você viaja sozinho, o mundo inteiro é sua casa e você é o capitão do seu próprio barco.",
            "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=800&q=80&auto=format",
            "DEFAULT",
            Map.of("AVENTURA", 1.0, "CULTURA", 0.9, "NATUREZA", 0.8, "URBANO", 0.7)
        );

        // 2. Economia e Persistência
        upsertPost(
            "O mapa do tesouro: Como economizar para a viagem dos seus sonhos",
            "Todos já passamos por isso: aquela vontade incontrolável de fazer as malas e sumir no mundo, esbarrando na dura realidade da conta bancária. Mas viajar não precisa ser um privilégio distante. O segredo está na persistência e no planejamento de longo prazo. Transforme a viagem no seu objetivo principal: corte pequenos gastos diários, estipule metas mensais de economia e, o mais importante, monitore passagens aéreas religiosamente em guias anônimas ou com alertas de preço. Se você poupar um valor fixo toda semana, a viagem do ano que vem já começa a virar realidade hoje. Lembre-se, cada café gourmet que você recusa hoje é um drink na beira do mar de Bali ou um ingresso para um museu em Paris amanhã. Persista, o mundo te espera!",
            "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=800&q=80&auto=format",
            "DEFAULT",
            Map.of("CULTURA", 1.0, "URBANO", 0.8, "HISTORIA", 0.8, "RELAXAMENTO", 0.7)
        );

        // 3. Galápagos (Pinguins) e Araras Azuis
        upsertPost(
            "Maravilhas da fauna: Dos Pinguins de Galápagos às Araras-Azuis",
            "Quando pensamos em ecoturismo de alto impacto, o encontro com a vida selvagem é o ápice da jornada. No arquipélago de Galápagos, você não é apenas um observador, mas um visitante na casa de espécies que evoluíram em isolamento. Ver os simpáticos e ágeis pinguins-de-galápagos nadando em águas equatoriais é um lembrete do poder da adaptação da vida. Já no Brasil, a imensidão do Pantanal nos presenteia com o voo majestoso das araras-azuis, cortando o céu com sua plumagem vibrante. Essas viagens não são apenas férias; são expedições de conexão profunda com a Terra. O respeito e o silêncio são as únicas moedas de entrada nesses santuários sagrados. O ecoturismo não só te transforma, como financia a preservação dessas vidas inestimáveis.",
            "https://images.unsplash.com/photo-1568214379698-8aeb8c6c6ac8?w=800&q=80&auto=format",
            "DEFAULT",
            Map.of("VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.9)
        );

        // 4. Tóquio e Cultura Automotiva
        upsertPost(
            "Neon, velocidade e drifting: O universo dos supercarros em Tóquio",
            "Tóquio não é apenas a terra dos templos antigos e cruzamentos lotados; é a capital indiscutível da cultura automotiva mundial. Caminhar por Akihabara ou Shibuya à noite significa ver passar pelo retrovisor bólidos modificados brilhando em cores neon, roncos de motores que soam como sinfonias e a estética inconfundível do JDM (Japanese Domestic Market). Se você é fascinado por velocidade, os encontros em postos de gasolina e as estradas ao redor do Monte Fuji oferecem um cenário de filme cyberpunk que mistura tradição e modernidade absoluta. Tóquio te permite acelerar para o futuro sem esquecer do seu passado glorioso.",
            "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=800&q=80&auto=format",
            "DEFAULT",
            Map.of("URBANO", 1.0, "VIDA_NOTURNA", 1.0, "CULTURA", 0.9, "AVENTURA", 0.8)
        );

        // 5. O Coração da Floresta Viva (Expandido para não quebrar a foto do usuário)
        upsertPost(
            "O coração da floresta viva",
            "Navegar pelos rios da Amazônia e sentir a força da maior biodiversidade terrestre do nosso planeta é uma experiência que altera a nossa percepção sobre a vida. A floresta respira, vibra e se movimenta o tempo todo. Entre o canto dos pássaros rústicos e o som da água cortada por barcos ribeirinhos, encontramos um silêncio que, paradoxalmente, é ensurdecedor de tanta vida. Acordar com a neblina sobre o rio negro e presenciar o pôr do sol dourado refletindo na água escura te ensina que a verdadeira riqueza do mundo não está nos centros urbanos, mas escondida sob o dossel das árvores mais antigas. \"Viajar não é sobre colecionar carimbos no passaporte, mas sobre permitir que cada novo destino transforme seu jeito de olhar o mundo.\"",
            "https://images.unsplash.com/photo-1518182170546-076616fdfaaf?w=800&q=80&auto=format",
            "DEFAULT",
            Map.of("NATUREZA", 1.0, "ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 1.0, "CALOR", 0.8)
        );

        // Posts Gamificados: "Isso ou Aquilo" (THIS_OR_THAT)
        upsertThisOrThatPost(
            "Isso ou Aquilo: Escolha sua Vibe",
            "Qual dessas jornadas combina mais com o seu momento de vida atual? Você busca o extremo gelado ou o frenesi noturno?",
            new OptionSeed("Desbravar a Patagônia Selvagem", "assets/feed/id01-eco.webp", Map.of("ECOTURISMO", 1.0, "AVENTURA", 1.0, "FRIO", 1.0, "MONTANHA", 0.9)),
            new OptionSeed("Viver as Luzes de Tóquio à Noite", "assets/feed/id01-city.webp", Map.of("URBANO", 1.0, "VIDA_NOTURNA", 1.0, "CULTURA", 0.9))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Refúgio dos Sonhos",
            "Onde você recarregaria suas energias no próximo feriado prolongado?",
            new OptionSeed("Mergulho nas Águas de Noronha", "assets/feed/id02-eco.webp", Map.of("ECOTURISMO", 1.0, "PRAIA", 1.0, "MERGULHO", 1.0, "CALOR", 1.0, "RELAXAMENTO", 0.8)),
            new OptionSeed("Chalé com Lareira na Serra Gaúcha", "assets/feed/id02-city.webp", Map.of("FRIO", 1.0, "ROMANTICO", 1.0, "RELAXAMENTO", 1.0, "GASTRONOMIA", 0.9))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Adrenalina e Ritmo",
            "Você prefere a imensidão das dunas e lagoas ou a energia vibrante de uma metrópole?",
            new OptionSeed("Expedição pelos Lençóis Maranhenses", "assets/feed/id03-eco.webp", Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "CALOR", 0.8)),
            new OptionSeed("Fim de Semana em Nova York", "assets/feed/id03-city.webp", Map.of("URBANO", 1.0, "VIDA_NOTURNA", 1.0, "CULTURA", 0.9))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Desconexão ou Cultura",
            "Uma imersão na floresta primária ou uma caminhada pela história milenar?",
            new OptionSeed("Trilha na Floresta Amazônica", "assets/feed/id04-eco.webp", Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 0.9)),
            new OptionSeed("Tour Histórico e Gastronômico em Roma", "assets/feed/id04-city.webp", Map.of("HISTORIA", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 1.0, "ROMANTICO", 0.8))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Noite Estrelada ou Cidade Luz",
            "Qual experiência noturna é o seu sonho de viagem?",
            new OptionSeed("Acampar sob as Estrelas no Atacama", "assets/feed/id05-eco.webp", Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0)),
            new OptionSeed("Jantar Romântico com Luzes de Paris", "assets/feed/id05-city.webp", Map.of("ROMANTICO", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 1.0))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Planeta Selvagem ou Oásis Futurista",
            "Qual contraste mais atrai sua curiosidade?",
            new OptionSeed("Safári Selvagem no Serengeti", "assets/feed/id06-eco.webp", Map.of("ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0)),
            new OptionSeed("Arranha-Céus e Luxo em Dubai", "assets/feed/id06-city.webp", Map.of("LUXO", 1.0, "URBANO", 1.0, "CALOR", 0.8))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Águas Cristalinas",
            "Paz nas nascentes de água doce ou agito nas praias do Caribe?",
            new OptionSeed("Flutuação nas Nascentes de Bonito", "assets/feed/id07-eco.webp", Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "MERGULHO", 1.0)),
            new OptionSeed("Praias e Festas em Cancún", "assets/feed/id07-city.webp", Map.of("PRAIA", 1.0, "CALOR", 1.0, "VIDA_NOTURNA", 1.0))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Desafios e Cidades",
            "Conquistar trilhas em serras tabulares ou explorar capitais lendárias?",
            new OptionSeed("Trilhas na Chapada Diamantina", "assets/feed/id08-eco.webp", Map.of("ECOTURISMO", 1.0, "MONTANHA", 1.0, "AVENTURA", 1.0, "NATUREZA", 1.0)),
            new OptionSeed("Museus e Pubs Históricos em Londres", "assets/feed/id08-city.webp", Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "URBANO", 1.0))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Frio Extremo ou Calor Litorâneo",
            "Geleiras vulcânicas ou um pôr do sol clássico na praia?",
            new OptionSeed("Geleiras e Fiordes em Reykjavík", "assets/feed/id09-eco.webp", Map.of("ECOTURISMO", 1.0, "FRIO", 1.0, "NEVE", 1.0, "NATUREZA", 1.0)),
            new OptionSeed("Calçadão e Pôr do Sol no Rio de Janeiro", "assets/feed/id09-city.webp", Map.of("PRAIA", 1.0, "CALOR", 1.0, "VIDA_NOTURNA", 0.9))
        );

        upsertThisOrThatPost(
            "Isso ou Aquilo: Natureza Pura ou Grandes Espetáculos",
            "O silêncio do maior santuário ecológico ou a cidade que nunca dorme?",
            new OptionSeed("Vida Selvagem e Safári no Pantanal", "assets/feed/id10-eco.webp", Map.of("ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "NATUREZA", 1.0)),
            new OptionSeed("Cassinos e Espetáculos em Las Vegas", "assets/feed/id10-city.webp", Map.of("VIDA_NOTURNA", 1.0, "URBANO", 1.0, "LUXO", 0.9))
        );

        // 25 Artigos Editoriais do Feed (IDs 101..125) com atributos comportamentais completos
        upsertCuratedArticleWithId(101L,
            "Pequenos gigantes do gelo",
            "Desembarcar na Península Antártica desafia qualquer escala humana de proporção. O silêncio absoluto só é rompido pelo som cavernoso do gelo que estala à distância e pelo bater ritmado das asas dos cormorões.\n\nNas colônias de pinguins-gentoo e pinguins-de-adélia, a rotina é frenética e comovente: centenas de indivíduos transitam pelas chamadas 'rodovias de pinguins' cavadas na neve compacta, revezando-se entre a caça voraz de krill nas águas glaciais e o cuidado meticuloso com seus ninhos de seixos.\n\nEstar ali nos ensina sobre resiliência ecológica e respeito absoluto aos limites da natureza. Não há pressa que resista ao vento gelado do Estreito de Gerlache e à visão de um colosso de gelo milenar esculpido em tons de azul elétrico.",
            "assets/feed/id101-1.webp",
            Map.of("FRIO", 1.0, "NEVE", 1.0, "VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "NATUREZA", 0.9, "AVENTURA", 0.9)
        );
        upsertCuratedArticleWithId(102L,
            "O encanto das águas cristalinas de Noronha",
            "Há poucos lugares no globo onde a comunhão entre terra e oceano seja tão intacta quanto em Fernando de Noronha. Do alto da escadaria cravada na fenda rochosa que dá acesso à Baía do Sancho, o tom esmeralda do mar parece quase surreal sob a luz do meio-dia.\n\nAo mergulhar com snorkel na Baía dos Porcos ou no Sueste, você não precisa procurar pela vida: são as tartarugas-verdes, raias-chita e cardumes multicoloridos que cruzam seu caminho com naturalidade desarmante. Ao longe, o Morro do Pico vigia o arquipélago como um farol milenar de origem vulcânica.\n\nA verdadeira magia de Noronha mora no ritmo do arquipélago: acordar antes da alvorada para contemplar os golfinhos-rotadores na Baía dos Golfinhos e terminar a tarde assistindo ao pôr do sol no Mirante do Boldró, com os pés na areia e a sensação rara de desaceleração profunda.",
            "assets/feed/id102-1.webp",
            Map.of("PRAIA", 1.0, "MERGULHO", 1.0, "ECOTURISMO", 1.0, "NATUREZA", 1.0, "CALOR", 0.9, "VIDA_SELVAGEM", 0.9, "RELAXAMENTO", 0.8)
        );
        upsertCuratedArticleWithId(103L,
            "Segredos da Chapada Diamantina: O lendário Vale do Pati",
            "Cruzar o Vale do Pati a pé é considerado por muitos viajantes experientes a travessia mais fascinante e transformadora do Brasil. São dias inteiros de caminhada desconectado de redes telefônicas e cercado pelas imponentes serras tabulares do Parque Nacional da Chapada Diamantina.\n\nO trajeto revela paisagens arrebatadoras: o abismo vertiginoso do Mirante do Cachoeirão, com suas múltiplas quedas d'água após as chuvas, as águas de cor conhaque do Rio Preto e a vastidão silenciosa dos Gerais dos Vieira.\n\nMas o que realmente marca a memória são as noites nas casas dos nativos patizeiros, como a Dona Raquel ou o Seu Wilson. À luz de velas, com refeições caseiras feitas no fogão a lenha e causos contados sob um céu apinhado de estrelas, descobre-se que a simplicidade do sertão baiano é a forma mais refinada de riqueza.",
            "assets/feed/id103-1.webp",
            Map.of("ECOTURISMO", 1.0, "AVENTURA", 1.0, "MONTANHA", 1.0, "NATUREZA", 1.0)
        );
        upsertCuratedArticleWithId(104L,
            "Sob o céu mais limpo do planeta no Deserto do Atacama",
            "A mais de 2.400 metros de altitude, onde o ar é rarefeito e a umidade praticamente inexiste, o Deserto do Atacama ostenta uma das janelas mais límpidas para o cosmos em nosso planeta. Quando a noite cai em San Pedro de Atacama e as luzes da vila se apagam, o firmamento ganha contornos tridimensionais.\n\nOs braços espirais da Via Láctea, as Nuvens de Magalhães e constelações indígenas ancestrais brilham com uma nitidez que hipnotiza astrônomos e viajantes. De dia, a paisagem é igualmente surreal: salares que abrigam bandos de flamingos andinos, lagoas altiplânicas de azul hipnótico a 4.000 metros e os gêiseres de El Tatio fumegando sob o gelo da manhã.\n\nO Atacama exige hidratação constante e passo cadenciado, recompensando cada esforço com a impressão inesquecível de caminhar em outro planeta sem sair da América do Sul.",
            "assets/feed/id104-1.webp",
            Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 1.0, "MONTANHA", 0.8)
        );
        upsertCuratedArticleWithId(105L,
            "Cores, aromas e histórias nos labirintos de Marrakech",
            "Entrar na medina murada de Marrakech é atravessar uma fronteira sensorial absoluta. Os souks são labirintos vivos de ruelas cobertas por esteiras de junco, onde a luz do sol filtra em feixes dourados sobre barris de açafrão, cominho, gengibre e pilhas de tâmaras frescas.\n\nEntre artesãos moldando luminárias de latão martelado e mestres tecelões trabalhando a lã tingida nos históricos curtumes, o tempo parece obedecer a códigos medievais. Nos pátios secretos dos riads, fontes de água murmuram cercadas por mosaicos zellij e laranjeiras aromáticas, oferecendo refúgios de frescor inestimáveis.\n\nAo entardecer, todos os caminhos convergem para a lendária praça Jemaa el-Fna: encantadores de serpentes, músicos gnawa e dezenas de bancas de comida fumegantes de tagines e espetos temperados criam o maior teatro a céu aberto do norte da África.",
            "assets/feed/id105-1.webp",
            Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "GASTRONOMIA", 1.0, "URBANO", 0.8, "CALOR", 0.8)
        );
        upsertCuratedArticleWithId(106L,
            "Amanhecer em Kyoto: Jardins zen e bambuzais milenares",
            "Se Tóquio representa o pulso elétrico do amanhã, Kyoto é o guardião sereno da alma milenar japonesa. O segredo para vivenciar sua atmosfera mais genuína é madrugar: caminhar pela floresta de bambu de Arashiyama antes das sete da manhã é ouvir apenas o estalar oco dos colmos gigantes oscilando com a brisa matinal.\n\nNos templos históricos como Ryoan-ji e Tenryu-ji, os jardins secos de pedra (karesansui) convidam à introspecção e ensinam sobre a estética do ma — o espaço vazio que dá sentido a todas as coisas. Cada rocha cuidadosamente posicionada sobre o cascalho ancinhado conta uma narrativa cósmica.\n\nCruzar as vielas de paralelepípedo de Gion ao crepúsculo, com lanternas de papel iluminando fachadas de cedro escuro e pontes de madeira sobre o canal Shirakawa, transforma a viagem em uma imersão poética na cultura da contemplação.",
            "assets/feed/id106-1.webp",
            Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "NATUREZA", 0.8, "RELAXAMENTO", 0.9)
        );
        upsertCuratedArticleWithId(107L,
            "Lagoas de água doce entre o mar de dunas dos Lençóis",
            "Imagine um deserto imenso de areia alvíssima cortado por centenas de lagoas cristalinas de água doce, formadas exclusivamente pelas chuvas do primeiro semestre. Os Lençóis Maranhenses compõem um ecossistema único no planeta, onde a dinâmica dos ventos redesenha o relevo diariamente.\n\nNadar nas águas mornas da Lagoa Bonita ou da Lagoa Azul após subir as dunas a pé é uma das sensações de alívio e deslumbramento mais marcantes do Nordeste brasileiro. A transição entre o verde-esmeralda da água e o branco puro do quartzo cria um contraste visual desconcertante.\n\nPara os mais aventureiros, a travessia a pé pelo Parque Nacional até o oásis de Queimada dos Britos oferece um mergulho na vida dos moradores locais que vivem em harmonia com as marés de areia, pescando traíras que hibernam sob a lama e celebrando noites de luar inesquecíveis.",
            "assets/feed/id107-1.webp",
            Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "PRAIA", 0.9, "CALOR", 1.0, "AVENTURA", 0.9)
        );
        upsertCuratedArticleWithId(108L,
            "A força estrondosa das Cataratas do Iguaçu",
            "Nenhuma fotografia consegue antecipar o impacto físico de se aproximar da Garganta do Diabo. O rugido de mais de 270 quedas d'água despejando milhões de litros por segundo vibra no peito antes mesmo de o viajante avistar o abismo encoberto por uma névoa permanente de arco-íris.\n\nCaminhar pelas passarelas que avançam sobre o Rio Iguaçu, cercado pela exuberância da Mata Atlântica e por centenas de borboletas de asas iridescentes, é uma lição de imponência natural. A água salpica no rosto e renova as energias em questão de segundos.\n\nTanto pelo lado brasileiro, com sua vista panorâmica cinematográfica, quanto pelo lado argentino, com passarelas que colocam você na beira exata do abismo d'água, Foz do Iguaçu permanece como um dos maiores espetáculos geológicos que a Terra tem a oferecer.",
            "assets/feed/id108-1.webp",
            Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.9, "VIDA_SELVAGEM", 0.8)
        );
        upsertCuratedArticleWithId(109L,
            "Expedição pelo Jalapão: Onde a terra brota água pura",
            "O Jalapão, no coração do cerrado tocantinense, é terra de contrastes dramáticos: chapadões de arenito avermelhado, veredas de buritis reluzentes e estradas de terra batida que desafiam até mesmo os veículos 4x4 mais preparados.\n\nA recompensa máxima da viagem mora nos famosos fervedouros, como o do Ceiça e o Bela Vista. Nessas nascentes subterrâneas de água termal azul-turquesa, o fenômeno da ressurgência cria uma pressão que impede qualquer pessoa de afundar, proporcionando uma sensação deliciosa e quase infantil de flutuação absoluta.\n\nSubir as dunas douradas no fim da tarde para ver o pôr do sol iluminar a Serra do Espírito Santo é o momento culminante da expedição, quando a terra parece pegar fogo em tons de ocre, dourado e lilás.",
            "assets/feed/id109-1.webp",
            Map.of("ECOTURISMO", 1.0, "AVENTURA", 1.0, "NATUREZA", 1.0, "CALOR", 0.9)
        );
        upsertCuratedArticleWithId(110L,
            "Glaciares milenares e ventos bravos da Patagônia",
            "Nas franjas do Campo de Gelo Sul, a Patagônia austral impõe seu temperamento indomável. Diante do colossal glaciar Perito Moreno, em El Calafate, os viajantes permanecem em silêncio absoluto esperando o espetáculo dos desprendimentos: blocos de gelo do tamanho de edifícios despencam nas águas do Lago Argentino com um estrondo que ecoa como um trovão pelos vales.\n\nMais ao norte, nos arredores de El Chaltén, a capital nacional do trekking, trilhas batidas pelo vento patagônico levam à base do Monte Fitz Roy e da Laguna de los Tres, onde agulhas de granito furam as nuvens com uma dramaticidade quase mítica.\n\nViajar pela Patagônia exige roupas técnicas em camadas e disposição para encarar as quatro estações em um único dia. Mas a recompensa de saborear um cordeiro patagônico na brasa e uma taça de vinho Malbec após horas de caminhada no vento gelado não tem preço.",
            "assets/feed/id110-1.webp",
            Map.of("ECOTURISMO", 1.0, "FRIO", 1.0, "NEVE", 1.0, "MONTANHA", 1.0, "AVENTURA", 1.0, "NATUREZA", 1.0)
        );
        upsertCuratedArticleWithId(111L,
            "O silêncio vivo da Floresta Amazônica ao entardecer",
            "A Floresta Amazônica não dorme: ela apenas troca de turno. Quando o sol desce no horizonte sobre o espelho negro do Rio Urubu e a névoa começa a subir das copas das samaúmas, os pássaros diurnos silenciam para dar lugar ao coro orquestrado de sapos, insetos e o sopro misterioso dos botos-cor-de-rosa emergindo para respirar.\n\nNavegar em canoas silenciosas pelos igapós inundados permite avistar preguiças camufladas nas folhas altas, macacos-de-cheiro saltando entre galhos e orquídeas selvagens que exalam perfumes adocicados no ar úmido.\n\nFicar hospedado em um hotel de selva sustentável e subir a uma torre de observação acima do dossel florestal no alvorecer é compreender a imensidão orgânica que regula o clima de todo o nosso continente. É uma experiência de humildade profunda diante da força biológica da Terra.",
            "assets/feed/id111-1.webp",
            Map.of("ECOTURISMO", 1.0, "NATUREZA", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 0.9, "CALOR", 0.8)
        );
        upsertCuratedArticleWithId(112L,
            "Neblina de serra, fondues e araucárias em Gramado",
            "A Serra Gaúcha possui um charme outonal que desacelera os ponteiros do relógio. Em Gramado, o aroma de chocolate artesanal flutua no ar fresco das manhãs, enquanto a neblina rasteira abraça os pinheiros de araucária ao redor do bucólico Lago Negro.\n\nA arquitetura enxaimel, herdada dos imigrantes europeus, emoldura cafeterias aconchegantes e empórios onde queijos coloniais, salames curados e vinhos de altitude convidam a pausas sem hora para terminar.\n\nNas noites frias de inverno, nada se compara a reunir amigos ou a pessoa amada ao redor de uma tradicional sequência de fondue na lareira. É o refúgio perfeito para quem busca sofisticação sem ostentação, conforto acolhedor e a tranquilidade revigorante do clima montanhês.",
            "assets/feed/id112-1.webp",
            Map.of("FRIO", 1.0, "ROMANTICO", 1.0, "GASTRONOMIA", 1.0, "RELAXAMENTO", 0.9, "MONTANHA", 0.8)
        );
        upsertCuratedArticleWithId(113L,
            "Flutuação surreal nos rios transparentes de Bonito",
            "A transparência inacreditável dos rios de Bonito, no Mato Grosso do Sul, não é fruto de mágica, mas de pura geologia: a água da chuva é filtrada pelo solo calcário, que decanta todas as impurezas e transforma os leitos fluviais em verdadeiros aquários naturais gigantescos.\n\nAo colocar a máscara e o snorkel no Rio Sucuri ou no Recanto Ecológico Rio da Prata, você simplesmente deixa a correnteza suave te guiar sem bater pernas. Dourados reluzentes, piraputangas curiosas e plantas aquáticas de verde brilhante passam a centímetros do seu rosto em perfeita harmonia.\n\nO modelo de turismo de Bonito é referência mundial: cada atrativo possui limite rigoroso de visitantes diários e controle ambiental por voucher digital, garantindo que o ecossistema permaneça intocado para as próximas gerações.",
            "assets/feed/id113-1.webp",
            Map.of("ECOTURISMO", 1.0, "MERGULHO", 1.0, "NATUREZA", 1.0, "VIDA_SELVAGEM", 0.9, "RELAXAMENTO", 0.8)
        );
        upsertCuratedArticleWithId(114L,
            "Cânions e águas verdes do Rio São Francisco em Xingó",
            "No sertão sergipano e alagoano, o majestoso Rio São Francisco esculpe um dos cenários mais surpreendentes do Brasil profundo: os Cânions de Xingó. Paredões de arenito avermelhado erguem-se a mais de cinquenta metros de altura, contrastando com o verde-esmeralda intenso das águas plácidas represadas.\n\nO passeio de catamarã pelo leito do rio leva até a Gruta do Talhado, uma fenda estreita onde apenas pequenos botes de madeira conseguem navegar. Ali dentro, o silêncio é sagrado e a temperatura cai bruscamente sob a sombra dos gigantes de rocha.\n\nAlém da beleza cenográfica monumental, a região respira história e cultura nordestina: das lembranças do cangaço de Lampião e Maria Bonita à gastronomia sertaneja farta, com peixes de água doce grelhados, queijo coalho e pirão de mocotó.",
            "assets/feed/id114-1.webp",
            Map.of("NATUREZA", 1.0, "CULTURA", 0.9, "HISTORIA", 0.8, "CALOR", 0.9, "AVENTURA", 0.8)
        );
        upsertCuratedArticleWithId(115L,
            "Trilhas sagradas no Vale Sagrado dos Incas",
            "Caminhar pelas cidades e fortalezas do Vale Sagrado dos Incas, como Pisac, Ollantaytambo e Moray, é mergulhar em uma civilização cuja engenharia agrícola e conhecimento astronômico dialogavam perfeitamente com as montanhas sagradas (Apus).\n\nAo chegar a Machu Picchu, seja de trem panorâmico margeando o agitado Rio Urubamba ou pela clássica Trilha Inca a pé, a sensação de reverência é instantânea. As ruínas de pedra finamente talhada parecem brotar das encostas verdejantes como uma extensão natural da serra.\n\nSubir as escadarias íngremes de Huayna Picchu sob a névoa matinal e observar a cidadela se revelando lá embaixo é um rito de passagem para qualquer amante da história e da espiritualidade andina.",
            "assets/feed/id115-1.webp",
            Map.of("HISTORIA", 1.0, "CULTURA", 1.0, "MONTANHA", 1.0, "AVENTURA", 0.9, "NATUREZA", 0.8)
        );
        upsertCuratedArticleWithId(116L,
            "Colinas douradas e vinhedos seculares na Toscana",
            "As colinas do Val d'Orcia representam a quintnsência da paisagem campestre italiana. Estradas sinuosas ladeadas por ciprestes pontiagudos cortam campos dourados de trigo e vinhedos perfeitamente alinhados que produzem alguns dos vinhos mais reverenciados do mundo, como o Brunello di Montalcino e o Chianti Classico.\n\nVilarejos medievais encarapitados no alto dos morros, como Pienza, Montepulciano e San Gimignano, convidam a caminhar sem destino por vielas de pedra, descobrindo adegas subterrâneas, queijarias que maturam pecorino artesanal e pequenas trattorias com massas frescas ao molho de javali.\n\nA Toscana não é uma viagem para se fazer correndo de ponto turístico em ponto turístico; é um convite aberto para desacelerar o passo, abrir um bom vinho e contemplar o pôr do sol tingindo as colinas de terracota.",
            "assets/feed/id116-1.webp",
            Map.of("GASTRONOMIA", 1.0, "ROMANTICO", 1.0, "CULTURA", 1.0, "HISTORIA", 0.9, "RELAXAMENTO", 0.9)
        );
        upsertCuratedArticleWithId(117L,
            "O santuário de evolução e vida marinha em Galápagos",
            "Localizado a mil quilômetros da costa do Equador, o arquipélago vulcânico de Galápagos opera sob regras próprias da natureza. Como os animais nunca aprenderam a temer a presença humana por predadores terrestres, o visitante vivencia encontros comoventes de proximidade e respeito.\n\nNas praias de areia escura, leões-marinhos brincalhões cochilam lado a lado com iguanas marinhas pré-históricas que espirram sal das narinas após mergulharem em busca de algas. Nos planaltos úmidos de Santa Cruz, tartarugas gigantes com mais de um século de vida pastam vagarosamente entre as samambaias.\n\nMergulhar nas águas de Kicker Rock ou Los Túneles revela tubarões-de-galápagos, tartarugas-marinhas e os ágeis pinguins-de-galápagos voando sob a água. É o museu vivo da teoria de Darwin em sua manifestação mais pura.",
            "assets/feed/id117-1.webp",
            Map.of("ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "MERGULHO", 1.0, "NATUREZA", 1.0, "PRAIA", 0.8)
        );
        upsertCuratedArticleWithId(118L,
            "O ritmo da vida selvagem no coração do Pantanal",
            "Maior planície alagável contínua do mundo, o Pantanal é a meca absoluta da observação de fauna na América do Sul. Ao percorrer a Rodovia Transpantaneira ou navegar pelos meandros dos rios Cuiabá e Piquiri em Porto Jofre, a densidade animal impressiona até os biólogos mais calejados.\n\nBandos de tuiuiús pousam em ninhos gigantescos no topo dos manduviers, enquanto capivaras e jacarés-do-pantanal dividem barrancos sob a vigilância atenta de lontras brincalhonas. Mas o ápice incontestável de qualquer expedição é o encontro com a onça-pintada: avistar o maior felino das Américas caçando silenciosamente na beira d'água é uma imagem que fica gravada na retina para sempre.\n\nO Pantanal exige guias experientes e paciência, retribuindo cada instante de vigília com espetáculos diários de sobrevivência e beleza bruta.",
            "assets/feed/id118-1.webp",
            Map.of("ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.9)
        );
        upsertCuratedArticleWithId(119L,
            "Aurora Boreal: As luzes do norte dançando em Tromsø",
            "Situada bem acima do Círculo Polar Ártico, na Noruega, a cidade de Tromsø é a capital mundial da caçada à Aurora Boreal. Durante a longa noite polar de inverno, quando o sol nem chega a nascer no horizonte, a atmosfera ártica ganha uma tonalidade azulada de mistério e fascínio.\n\nAguardar em meio à neve espessa, ao redor de uma fogueira estalando e saboreando chá quente de frutas silvestres, até que a primeira fita esverdeada comece a serpentear pelo céu estrelado é uma das esperas mais emocionantes da vida.\n\nDe repente, o firmamento explode em cortinas dançantes de verde, violeta e magenta em questão de segundos. A força geomagnética do vento solar interagindo com a atmosfera é pura poesia cósmica em movimento sobre fiordes congelados.",
            "assets/feed/id119-1.webp",
            Map.of("FRIO", 1.0, "NEVE", 1.0, "NATUREZA", 1.0, "AVENTURA", 0.9, "ROMANTICO", 0.8)
        );
        upsertCuratedArticleWithId(120L,
            "Praias rústicas e falésias da Costa do Descobrimento",
            "O trecho litorâneo do sul da Bahia que abriga Trancoso, Caraíva e Arraial d'Ajuda é um manifesto à vida desacelerada e à comunhão com a natureza litorânea. Em Caraíva, onde não entram carros e as ruas continuam sendo de areia fofa, o tempo passa no compasso do encontro entre o rio de águas calmas e o mar aberto.\n\nCaminhar até a Praia do Espelho na maré baixa revela falésias monumentais em tons de ocre e branco debruçadas sobre piscinas naturais de cor turquesa límpida, onde corais formam refúgios de vida submarina.\n\nNo Quadrado de Trancoso, o gramado histórico diante da igrejinha de São João Batista ganha vida ao anoitecer com mesas à luz de velas sob amendoeiras frondosas, combinando o tempero inconfundível da moqueca baiana com uma hospitalidade calorosa e autêntica.",
            "assets/feed/id120-1.webp",
            Map.of("PRAIA", 1.0, "CALOR", 1.0, "RELAXAMENTO", 1.0, "GASTRONOMIA", 0.8, "NATUREZA", 0.9)
        );
        upsertCuratedArticleWithId(121L,
            "A imponência vertical das Dolomitas no norte da Itália",
            "Erguendo-se dramaticamente no norte da Itália, as Dolomitas formam uma das cordilheiras mais impressionantes do continente europeu. Suas paredes verticais de rocha calcária possuem uma composição mineral única que produz o fenômeno da Enrosadira: ao nascer e ao pôr do sol, as montanhas brilham em tons incandescentes de rosa e dourado.\n\nTrilhas lendárias como o circuito ao redor das Três Cimes de Lavaredo ou a passagem pelo refúgio de montanha em Seceda revelam platôs verdes pontilhados por chalés de madeira e rebanhos de vacas alpinas pastando em paz.\n\nA cultura da região mistura com elegância influências austríacas e italianas, refletidas em pratos substanciais como canederli servidos com manteiga de sálvia e apfelstrudel quentinho após dias intensos de trekking e ar puro.",
            "assets/feed/id121-1.webp",
            Map.of("MONTANHA", 1.0, "FRIO", 0.9, "NEVE", 0.8, "AVENTURA", 1.0, "NATUREZA", 1.0, "GASTRONOMIA", 0.8)
        );
        upsertCuratedArticleWithId(122L,
            "Caminhos coloniais de pedra e casarios em Paraty",
            "Caminhar pelas ruas de pedras 'pé-de-moleque' do Centro Histórico de Paraty é fazer uma viagem no tempo diretamente para o século XVIII. Os casarões coloniais de paredes caiadas de branco, com portas e janelas pintadas em cores vibrantes, contam histórias da rota do ouro e do café da Estrada Real.\n\nNas luas cheias, a maré sobe e invade delicadamente as ruas mais próximas ao cais, transformando a vila em um espelho d'água cinematográfico que reflete as fachadas históricas e as montanhas cobertas pela Mata Atlântica ao fundo.\n\nPasseios de traineira ou escuna pelas águas calmas da Baía de Paraty levam a praias secretas como a do Sono e ilhas isoladas de mar calmo e transparente, perfeitas para nadar entre peixes e desfrutar do frescor da serra que encontra o mar.",
            "assets/feed/id122-1.webp",
            Map.of("HISTORIA", 1.0, "CULTURA", 1.0, "PRAIA", 0.8, "NATUREZA", 0.8, "GASTRONOMIA", 0.8, "ROMANTICO", 0.8)
        );
        upsertCuratedArticleWithId(123L,
            "Lagos turquesa e picos nevados no Parque de Banff",
            "O Parque Nacional de Banff, nas Montanhas Rochosas de Alberta, parece ter sido desenhado por um pintor apaixonado pela natureza selvagem. O icônico Lake Louise, alimentado pelo derretimento do Glaciar Victoria, exibe um tom azul-turquesa leitoso tão intenso que parece retocado por filtros digitais.\n\nSubir pelas trilhas que levam ao Lake Agnes Tea House oferece vistas panorâmicas de florestas densas de coníferas e vales glaciares profundos, com paradas para degustar chás fumegantes em cabanas de madeira construídas no início do século passado.\n\nA vida selvagem no parque é abundante e protegida com rigor exemplar: é comum avistar alces pastando em campos abertos, bodes-das-rochosas escalando encostas íngremes e, com sorte e distância respeitosa, ursos-grizzly forrageando frutas silvestres no verão.",
            "assets/feed/id123-1.webp",
            Map.of("ECOTURISMO", 1.0, "MONTANHA", 1.0, "FRIO", 1.0, "NEVE", 0.9, "VIDA_SELVAGEM", 0.9, "NATUREZA", 1.0)
        );
        upsertCuratedArticleWithId(124L,
            "Aromas da cozinha mineira e serras de Monte Verde",
            "Encravada no alto da Serra da Mantiqueira, a charmosa vila de Monte Verde combina o melhor da hospitalidade mineira com o clima europeu de montanha. A mais de 1.500 metros de altitude, o ar é perfumado pelo aroma de pinheiros e pelo aroma tentador de pão de queijo quentinho saindo do forno.\n\nAs trilhas da região, como a subida à Pedra Redonda e ao Chapéu do Bispo, oferecem caminhadas acessíveis que terminam em mirantes espetaculares de 360 graus sobre o mar de morros que divide Minas Gerais e São Paulo.\n\nÀ noite, o centrinho iluminado convida a passear sem pressa entre ateliês de cerâmica, empórios de queijo da canastra curado, cachaças artesanais e restaurantes que servem trutas frescas grelhadas acompanhadas de vinhos locais.",
            "assets/feed/id124-1.webp",
            Map.of("MONTANHA", 1.0, "FRIO", 0.9, "GASTRONOMIA", 1.0, "ROMANTICO", 1.0, "RELAXAMENTO", 0.9)
        );
        upsertCuratedArticleWithId(125L,
            "O espetáculo do céu ao amanhecer na Capadócia",
            "Poucas experiências de viagem no mundo conseguem rivalizar com a poesia visual de acordar antes do sol nascer na Capadócia. No vale de Göreme, o horizonte lentamente se preenche com mais de uma centena de balões de ar quente inflando em uníssono, subindo suavemente ao céu enquanto os primeiros raios solares douram as formações rochosas esculpidas pela erosão e pela cinza vulcânica milenar.\n\nLá de cima, a sensação é de flutuar em silêncio sobre chaminés de fadas, mosteiros escavados na rocha e cidades subterrâneas como Derinkuyu, onde civilizações antigas encontraram refúgio contra invasões.\n\nFicar hospedado em um autêntico hotel-caverna esculpido na encosta da montanha e tomar café da manhã no terraço observando o balé multicolorido dos balões no céu é um daqueles momentos que justificam qualquer viagem.",
            "assets/feed/id125-1.webp",
            Map.of("CULTURA", 1.0, "HISTORIA", 1.0, "ROMANTICO", 1.0, "AVENTURA", 0.9, "LUXO", 0.8)
        );

        List<String> validTitles = List.of(
            "A arte de viajar sozinho: Desbravando o mundo na sua própria companhia",
            "O mapa do tesouro: Como economizar para a viagem dos seus sonhos",
            "Maravilhas da fauna: Dos Pinguins de Galápagos às Araras-Azuis",
            "Neon, velocidade e drifting: O universo dos supercarros em Tóquio",
            "O coração da floresta viva",
            "Pequenos gigantes do gelo",
            "O encanto das águas cristalinas de Noronha",
            "Segredos da Chapada Diamantina: O lendário Vale do Pati",
            "Sob o céu mais limpo do planeta no Deserto do Atacama",
            "Cores, aromas e histórias nos labirintos de Marrakech",
            "Amanhecer em Kyoto: Jardins zen e bambuzais milenares",
            "Lagoas de água doce entre o mar de dunas dos Lençóis",
            "A força estrondosa das Cataratas do Iguaçu",
            "Expedição pelo Jalapão: Onde a terra brota água pura",
            "Glaciares milenares e ventos bravos da Patagônia",
            "O silêncio vivo da Floresta Amazônica ao entardecer",
            "Neblina de serra, fondues e araucárias em Gramado",
            "Flutuação surreal nos rios transparentes de Bonito",
            "Cânions e águas verdes do Rio São Francisco em Xingó",
            "Trilhas sagradas no Vale Sagrado dos Incas",
            "Colinas douradas e vinhedos seculares na Toscana",
            "O santuário de evolução e vida marinha em Galápagos",
            "O ritmo da vida selvagem no coração do Pantanal",
            "Aurora Boreal: As luzes do norte dançando em Tromsø",
            "Praias rústicas e falésias da Costa do Descobrimento",
            "A imponência vertical das Dolomitas no norte da Itália",
            "Caminhos coloniais de pedra e casarios em Paraty",
            "Lagos turquesa e picos nevados no Parque de Banff",
            "Aromas da cozinha mineira e serras de Monte Verde",
            "O espetáculo do céu ao amanhecer na Capadócia",
            "Isso ou Aquilo: Escolha sua Vibe",
            "Isso ou Aquilo: Refúgio dos Sonhos",
            "Isso ou Aquilo: Adrenalina e Ritmo",
            "Isso ou Aquilo: Desconexão ou Cultura",
            "Isso ou Aquilo: Noite Estrelada ou Cidade Luz",
            "Isso ou Aquilo: Planeta Selvagem ou Oásis Futurista",
            "Isso ou Aquilo: Águas Cristalinas",
            "Isso ou Aquilo: Desafios e Cidades",
            "Isso ou Aquilo: Frio Extremo ou Calor Litorâneo",
            "Isso ou Aquilo: Natureza Pura ou Grandes Espetáculos"
        );
        for (PostEntity p : postRepository.findAll()) {
            if (!validTitles.contains(p.getTitle()) && (p.getId() == null || p.getId() < 101 || p.getId() > 125)) {
                p.setPublished(false);
                postRepository.save(p);
            }
        }
        try {
            jdbcTemplate.update("""
                UPDATE posts p SET
                    likes_count = (SELECT count(*) FROM interactions i WHERE i.post_id = p.id AND i.interaction_type = 'LIKE'),
                    dislikes_count = (SELECT count(*) FROM interactions i WHERE i.post_id = p.id AND i.interaction_type = 'DISLIKE')
            """);
            jdbcTemplate.update("""
                UPDATE post_options po SET
                    votes_count = (SELECT count(*) FROM interactions i WHERE i.option_id = po.id AND i.interaction_type = 'VOTE')
            """);
            jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('posts', 'id'), COALESCE((SELECT MAX(id) FROM posts), 1) + 1, false)");
        } catch (Exception e) {
            log.warn("Erro ao sincronizar contadores de posts: {}", e.getMessage());
        }
    }

    private void upsertCuratedArticleWithId(Long id, String title, String caption, String imageUrl, Map<String, Double> attrScores) {
        try {
            jdbcTemplate.update("""
                INSERT INTO posts (id, title, caption, image_url, post_type, published, likes_count, dislikes_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ARTICLE', true, 0, 0, NOW(), NOW())
                ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    caption = EXCLUDED.caption,
                    image_url = EXCLUDED.image_url,
                    post_type = 'ARTICLE',
                    published = true,
                    updated_at = NOW()
            """, id, title, caption, imageUrl);

            for (Map.Entry<String, Double> entry : attrScores.entrySet()) {
                AttributeEntity attr = attributes.get(entry.getKey());
                if (attr != null) {
                    jdbcTemplate.update("""
                        INSERT INTO post_attributes (post_id, attribute_id, weight)
                        VALUES (?, ?, ?)
                        ON CONFLICT (post_id, attribute_id) DO UPDATE SET weight = EXCLUDED.weight
                    """, id, attr.getId(), BigDecimal.valueOf(entry.getValue()));
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao fazer upsert do artigo editorial id={}: {}", id, e.getMessage());
        }
    }

    private void upsertPost(String title, String caption, String imageUrl, String postType, Map<String, Double> attrScores) {
        PostEntity post = postRepository.findAll().stream()
                .filter(p -> p.getTitle().equals(title))
                .findFirst()
                .orElseGet(() -> {
                    PostEntity p = new PostEntity();
                    p.setTitle(title);
                    p.setCreatedAt(OffsetDateTime.now());
                    return p;
                });

        post.setCaption(caption);
        post.setImageUrl(imageUrl);
        post.setPostType(postType);
        if (post.getLikesCount() == null) post.setLikesCount(0L);
        if (post.getDislikesCount() == null) post.setDislikesCount(0L);
        post.setPublished(true);
        post.setUpdatedAt(OffsetDateTime.now());
        post = postRepository.save(post);

        for (Map.Entry<String, Double> entry : attrScores.entrySet()) {
            AttributeEntity attr = attributes.get(entry.getKey());
            if (attr != null) {
                upsertPostAttr(post, attr, BigDecimal.valueOf(entry.getValue()));
            }
        }
    }

    private record OptionSeed(String label, String imageUrl, Map<String, Double> attributes) {}

    private void upsertThisOrThatPost(String title, String caption, OptionSeed opt1, OptionSeed opt2) {
        PostEntity post = postRepository.findAll().stream()
                .filter(p -> p.getTitle().equals(title))
                .findFirst()
                .orElseGet(() -> {
                    PostEntity p = new PostEntity();
                    p.setTitle(title);
                    p.setCreatedAt(OffsetDateTime.now());
                    return p;
                });

        post.setTitle(title);
        post.setCaption(caption);
        post.setImageUrl(opt1.imageUrl());
        post.setPostType("THIS_OR_THAT");
        post.setPublished(true);
        post.setUpdatedAt(OffsetDateTime.now());
        post = postRepository.save(post);

        upsertPostOption(post.getId(), opt1.label(), opt1.imageUrl(), opt1.attributes());
        upsertPostOption(post.getId(), opt2.label(), opt2.imageUrl(), opt2.attributes());
    }

    private void upsertPostOption(Long postId, String label, String imageUrl, Map<String, Double> optAttrs) {
        try {
            Long optionId;
            List<Long> existingIds = jdbcTemplate.query(
                "SELECT id FROM post_options WHERE post_id = ? AND label = ?",
                (rs, rowNum) -> rs.getLong("id"),
                postId, label
            );
            if (!existingIds.isEmpty()) {
                optionId = existingIds.get(0);
                jdbcTemplate.update("UPDATE post_options SET image_url = ? WHERE id = ?", imageUrl, optionId);
            } else {
                optionId = jdbcTemplate.queryForObject(
                    "INSERT INTO post_options (post_id, label, image_url, votes_count) VALUES (?, ?, ?, 0) RETURNING id",
                    Long.class,
                    postId, label, imageUrl
                );
            }

            if (optionId != null) {
                jdbcTemplate.update("DELETE FROM post_option_attributes WHERE option_id = ?", optionId);
                for (Map.Entry<String, Double> e : optAttrs.entrySet()) {
                    AttributeEntity attr = attributes.get(e.getKey());
                    if (attr != null) {
                        jdbcTemplate.update(
                            "INSERT INTO post_option_attributes (option_id, attribute_id, weight) VALUES (?, ?, ?)",
                            optionId, attr.getId(), BigDecimal.valueOf(e.getValue())
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao inserir post_option: {}", e.getMessage());
        }
    }

    private void upsertPostAttr(PostEntity post, AttributeEntity attr, BigDecimal weight) {
        PostAttributeEntity pa = postAttributeRepository.findAll().stream()
                .filter(p -> p.getId().getPostId().equals(post.getId()) && p.getId().getAttributeId().equals(attr.getId()))
                .findFirst()
                .orElseGet(() -> {
                    PostAttributeEntity newPa = new PostAttributeEntity();
                    newPa.getId().setPostId(post.getId());
                    newPa.getId().setAttributeId(attr.getId());
                    newPa.setPost(post);
                    newPa.setAttribute(attr);
                    return newPa;
                });
        pa.setWeight(weight);
        postAttributeRepository.save(pa);
    }

    private void seedFlights() {
        try {
            jdbcTemplate.update(
                "UPDATE flights SET active = false WHERE active = true AND (flight_number IS NULL OR flight_number !~ '^HZ[0-9]+[DR][0-9]{4}(-[0-9])?$')"
            );
            Long validFlightsCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flights WHERE active = true AND flight_number ~ '^HZ[0-9]+[DR][0-9]{4}(-[0-9])?$'",
                Long.class
            );
            if (validFlightsCount != null && validFlightsCount >= 15000) {
                log.info("Malha aérea completa já semeada ({} voos ativos). Pulando recriação pesada.", validFlightsCount);
                return;
            }
        } catch (Exception e) {
            log.warn("Aviso ao verificar malha aérea existente via SQL: {}", e.getMessage());
        }

        List<DestinationEntity> activeDestinations = destinationRepository.findAll().stream()
                .filter(DestinationEntity::getActive)
                .toList();

        Map<String, String> airportCodes = new HashMap<>();
        airportCodes.put("São Paulo", "CGH");
        airportCodes.put("Rio de Janeiro", "GIG");
        airportCodes.put("Paris", "CDG");
        airportCodes.put("Tóquio", "HND");
        airportCodes.put("Antártida", "TNM");
        airportCodes.put("Amazônia", "MAO");
        airportCodes.put("Serengeti", "JRO");
        airportCodes.put("Galápagos", "GPS");
        airportCodes.put("Roma", "FCO");
        airportCodes.put("Atenas", "ATH");
        airportCodes.put("Istambul", "IST");
        airportCodes.put("Quioto", "KIX");
        airportCodes.put("Londres", "LHR");
        airportCodes.put("Praga", "PRG");
        airportCodes.put("Nova York", "JFK");
        airportCodes.put("Las Vegas", "LAS");
        airportCodes.put("Seul", "ICN");
        airportCodes.put("Madri", "MAD");
        airportCodes.put("Cancún", "CUN");
        airportCodes.put("Maldivas", "MLE");
        airportCodes.put("Punta Cana", "PUJ");
        airportCodes.put("Santorini", "JTR");
        airportCodes.put("Dubai", "DXB");
        airportCodes.put("Lisboa", "LIS");
        airportCodes.put("Amsterdã", "AMS");
        airportCodes.put("Barcelona", "BCN");
        airportCodes.put("Veneza", "VCE");
        airportCodes.put("Florença", "FLR");
        airportCodes.put("Sydney", "SYD");
        airportCodes.put("Toronto", "YYZ");
        airportCodes.put("Bangkok", "BKK");
        airportCodes.put("Bogotá", "BOG");
        airportCodes.put("Gramado", "POA");
        airportCodes.put("Patagônia", "USH");
        airportCodes.put("Machu Picchu", "CUZ");
        airportCodes.put("Alpes Suíços", "ZRH");
        airportCodes.put("Banff", "YYC");
        airportCodes.put("Bariloche", "BRC");
        airportCodes.put("Parque Nacional Kruger", "JNB");
        airportCodes.put("Grande Barreira de Corais", "CNS");
        airportCodes.put("Reykjavík", "KEF");
        airportCodes.put("Fernando de Noronha", "FEN");
        airportCodes.put("Atacama", "CJC");
        airportCodes.put("Bonito", "BYO");
        airportCodes.put("Chapada Diamantina", "LEC");
        airportCodes.put("Jalapão", "PMW");
        airportCodes.put("Pantanal", "CGB");
        airportCodes.put("Foz do Iguaçu", "IGU");
        airportCodes.put("Costa Rica", "SJO");
        airportCodes.put("Yellowstone", "BZN");

        class FlightSlot {
            final LocalDate date;
            final LocalTime outDep;
            final LocalTime outArr;
            final LocalTime retDep;
            final LocalTime retArr;
            final String suffix;
            final double priceRatio;
            final int totalSeats;
            final int availSeats;

            FlightSlot(LocalDate date, LocalTime outDep, LocalTime outArr, LocalTime retDep, LocalTime retArr,
                       String suffix, double priceRatio, int totalSeats, int availSeats) {
                this.date = date;
                this.outDep = outDep;
                this.outArr = outArr;
                this.retDep = retDep;
                this.retArr = retArr;
                this.suffix = suffix;
                this.priceRatio = priceRatio;
                this.totalSeats = totalSeats;
                this.availSeats = availSeats;
            }
        }

        int year = LocalDate.now().getYear();
        List<LocalDate> targetDates = new ArrayList<>(List.of(
                // Outubro: 16, 17, 19, 20, 21, 22, 24, 27, 29, 30, 31
                LocalDate.of(year, 10, 16),
                LocalDate.of(year, 10, 17),
                LocalDate.of(year, 10, 19),
                LocalDate.of(year, 10, 20),
                LocalDate.of(year, 10, 21),
                LocalDate.of(year, 10, 22),
                LocalDate.of(year, 10, 24),
                LocalDate.of(year, 10, 27),
                LocalDate.of(year, 10, 29),
                LocalDate.of(year, 10, 30),
                LocalDate.of(year, 10, 31),
                // Novembro: 05, 07, 13, 15, 16, 17, 18, 23, 25, 27, 28, 29, 30
                LocalDate.of(year, 11, 5),
                LocalDate.of(year, 11, 7),
                LocalDate.of(year, 11, 13),
                LocalDate.of(year, 11, 15),
                LocalDate.of(year, 11, 16),
                LocalDate.of(year, 11, 17),
                LocalDate.of(year, 11, 18),
                LocalDate.of(year, 11, 23),
                LocalDate.of(year, 11, 25),
                LocalDate.of(year, 11, 27),
                LocalDate.of(year, 11, 28),
                LocalDate.of(year, 11, 29),
                LocalDate.of(year, 11, 30),
                // Dezembro: 02, 03, 04, 09, 12, 13, 15, 17, 20, 22, 24, 26, 27, 29, 30
                LocalDate.of(year, 12, 2),
                LocalDate.of(year, 12, 3),
                LocalDate.of(year, 12, 4),
                LocalDate.of(year, 12, 9),
                LocalDate.of(year, 12, 12),
                LocalDate.of(year, 12, 13),
                LocalDate.of(year, 12, 15),
                LocalDate.of(year, 12, 17),
                LocalDate.of(year, 12, 20),
                LocalDate.of(year, 12, 22),
                LocalDate.of(year, 12, 24),
                LocalDate.of(year, 12, 26),
                LocalDate.of(year, 12, 27),
                LocalDate.of(year, 12, 29),
                LocalDate.of(year, 12, 30),
                // Janeiro: 04 até 15 contínuos, depois 18, 20, 23, 24, 25, 27, 30
                LocalDate.of(year + 1, 1, 4),
                LocalDate.of(year + 1, 1, 5),
                LocalDate.of(year + 1, 1, 6),
                LocalDate.of(year + 1, 1, 7),
                LocalDate.of(year + 1, 1, 8),
                LocalDate.of(year + 1, 1, 9),
                LocalDate.of(year + 1, 1, 10),
                LocalDate.of(year + 1, 1, 11),
                LocalDate.of(year + 1, 1, 12),
                LocalDate.of(year + 1, 1, 13),
                LocalDate.of(year + 1, 1, 14),
                LocalDate.of(year + 1, 1, 15),
                LocalDate.of(year + 1, 1, 18),
                LocalDate.of(year + 1, 1, 20),
                LocalDate.of(year + 1, 1, 23),
                LocalDate.of(year + 1, 1, 24),
                LocalDate.of(year + 1, 1, 25),
                LocalDate.of(year + 1, 1, 27),
                LocalDate.of(year + 1, 1, 30),
                // Fevereiro: ~10 dias sortidos
                LocalDate.of(year + 1, 2, 2),
                LocalDate.of(year + 1, 2, 5),
                LocalDate.of(year + 1, 2, 8),
                LocalDate.of(year + 1, 2, 11),
                LocalDate.of(year + 1, 2, 14),
                LocalDate.of(year + 1, 2, 17),
                LocalDate.of(year + 1, 2, 20),
                LocalDate.of(year + 1, 2, 22),
                LocalDate.of(year + 1, 2, 25),
                LocalDate.of(year + 1, 2, 28),
                // Março: ~10 dias sortidos (total 20 dias em Fev/Mar)
                LocalDate.of(year + 1, 3, 3),
                LocalDate.of(year + 1, 3, 6),
                LocalDate.of(year + 1, 3, 9),
                LocalDate.of(year + 1, 3, 12),
                LocalDate.of(year + 1, 3, 15),
                LocalDate.of(year + 1, 3, 18),
                LocalDate.of(year + 1, 3, 21),
                LocalDate.of(year + 1, 3, 24),
                LocalDate.of(year + 1, 3, 27),
                LocalDate.of(year + 1, 3, 30)
        ));

        // Grade de horários de partida possíveis. Em cada data o destino recebe 2 ou 3 voos
        // em horários diferentes, todos com a MESMA duração fixa da rota GRU <-> destino.
        LocalTime[] outboundSlots = {
                LocalTime.of(6, 15), LocalTime.of(8, 0), LocalTime.of(10, 30), LocalTime.of(13, 45),
                LocalTime.of(16, 20), LocalTime.of(19, 30), LocalTime.of(21, 45)
        };
        LocalTime[] returnSlots = {
                LocalTime.of(7, 0), LocalTime.of(9, 40), LocalTime.of(12, 15), LocalTime.of(15, 0),
                LocalTime.of(17, 50), LocalTime.of(20, 10), LocalTime.of(22, 30)
        };
        // Fatores de preço por horário: o 1º voo mantém a tarifa base ("a partir de").
        double[] slotPriceFactors = {1.0, 1.12, 1.06};

        Map<String, FlightEntity> existingFlights = flightRepository.findAll().stream()
                .filter(f -> f.getFlightNumber() != null)
                .collect(Collectors.toMap(FlightEntity::getFlightNumber, f -> f, (a, b) -> a));

        Map<Long, FlightAvailabilityEntity> existingAvailByFlightId = flightAvailabilityRepository.findAll().stream()
                .filter(a -> a.getFlight() != null && a.getFlight().getId() != null)
                .collect(Collectors.toMap(a -> a.getFlight().getId(), a -> a, (a, b) -> a));

        List<FlightEntity> flightsToSave = new ArrayList<>();
        record PendingAvail(FlightEntity flight, int totalSeats, int availSeats) {}
        List<PendingAvail> pendingAvails = new ArrayList<>();

        for (DestinationEntity dest : activeDestinations) {
            String destCode = airportCodes.getOrDefault(
                    dest.getName(),
                    dest.getName().length() >= 3 ? dest.getName().substring(0, 3).toUpperCase() : "XYZ"
            );

            String originCode = "GRU";
            BigDecimal base = dest.getBasePrice() != null ? dest.getBasePrice() : BigDecimal.valueOf(1500);
            BigDecimal minOutbound = base.divide(BigDecimal.valueOf(2), 0, RoundingMode.FLOOR);
            BigDecimal minReturn = base.subtract(minOutbound);

            BigDecimal outBasic = minOutbound;
            BigDecimal outExec = outBasic.multiply(BigDecimal.valueOf(1.75)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal outPrem = outBasic.multiply(BigDecimal.valueOf(2.50)).setScale(0, RoundingMode.HALF_UP);

            BigDecimal retBasic = minReturn;
            BigDecimal retExec = retBasic.multiply(BigDecimal.valueOf(1.75)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal retPrem = retBasic.multiply(BigDecimal.valueOf(2.50)).setScale(0, RoundingMode.HALF_UP);

            // Duração fixa da rota GRU <-> destino (igual para todos os horários do dia)
            int durationMinutes = getRealisticFlightDurationMinutes(dest.getName(), dest.getCountry());

            for (int i = 0; i < targetDates.size(); i++) {
                LocalDate d = targetDates.get(i);
                String suffix = String.format("%02d%02d", d.getMonthValue(), d.getDayOfMonth());
                int flightsPerDay = 2 + ((i + dest.getId().intValue()) % 2);

                List<LocalTime> outTimes = new ArrayList<>();
                List<LocalTime> retTimes = new ArrayList<>();
                for (int s = 0; s < flightsPerDay; s++) {
                    outTimes.add(outboundSlots[(i + dest.getId().intValue() + s * 2) % outboundSlots.length]);
                    retTimes.add(returnSlots[(i + dest.getId().intValue() + s * 3) % returnSlots.length]);
                }
                outTimes = outTimes.stream().distinct().sorted().toList();
                retTimes = retTimes.stream().distinct().sorted().toList();

                for (int s = 0; s < outTimes.size(); s++) {
                    LocalTime outDep = outTimes.get(s);
                    String slotSuffix = s == 0 ? "" : "-" + (s + 1);
                    BigDecimal factor = BigDecimal.valueOf(slotPriceFactors[s % slotPriceFactors.length]);
                    int totalSeats = 120 + ((i + s) % 4) * 10;
                    int availSeats = 45 + ((i + s) * 7) % 65;

                    // Voo de Ida (GRU -> Destino)
                    String outNumber = "HZ" + dest.getId() + "D" + suffix + slotSuffix;
                    FlightEntity outFlight = prepareFlightEntity(existingFlights, dest, originCode, destCode,
                            d, outDep, outDep.plusMinutes(durationMinutes), durationMinutes, outNumber,
                            scale(outBasic, factor), scale(outExec, factor), scale(outPrem, factor));
                    flightsToSave.add(outFlight);
                    pendingAvails.add(new PendingAvail(outFlight, totalSeats, availSeats));
                }

                for (int s = 0; s < retTimes.size(); s++) {
                    LocalTime retDep = retTimes.get(s);
                    String slotSuffix = s == 0 ? "" : "-" + (s + 1);
                    BigDecimal factor = BigDecimal.valueOf(slotPriceFactors[s % slotPriceFactors.length]);
                    int totalSeats = 120 + ((i + s + 1) % 4) * 10;
                    int availSeats = 45 + ((i + s + 3) * 7) % 65;

                    // Voo de Volta (Destino -> GRU)
                    String retNumber = "HZ" + dest.getId() + "R" + suffix + slotSuffix;
                    FlightEntity retFlight = prepareFlightEntity(existingFlights, dest, destCode, originCode,
                            d, retDep, retDep.plusMinutes(durationMinutes), durationMinutes, retNumber,
                            scale(retBasic, factor), scale(retExec, factor), scale(retPrem, factor));
                    flightsToSave.add(retFlight);
                    pendingAvails.add(new PendingAvail(retFlight, totalSeats, availSeats));
                }
            }
        }

        flightRepository.saveAll(flightsToSave);

        List<FlightAvailabilityEntity> availToSave = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (PendingAvail pa : pendingAvails) {
            Long fId = pa.flight().getId();
            FlightAvailabilityEntity availability = fId != null ? existingAvailByFlightId.get(fId) : null;
            if (availability == null) {
                availability = new FlightAvailabilityEntity();
                availability.setFlight(pa.flight());
            }
            availability.setTotalSeats(pa.totalSeats());
            if (availability.getAvailableSeats() == null || availability.getAvailableSeats() <= 0) {
                availability.setAvailableSeats(pa.availSeats());
            }
            availability.setUpdatedAt(now);
            availToSave.add(availability);
        }
        flightAvailabilityRepository.saveAll(availToSave);
    }

    private FlightEntity prepareFlightEntity(Map<String, FlightEntity> existingFlights,
                                             DestinationEntity dest, String origin, String destination,
                                             LocalDate date, LocalTime dep, LocalTime arr, int durationMinutes, String flightNumber,
                                             BigDecimal basic, BigDecimal exec, BigDecimal prem) {
        FlightEntity flight = existingFlights.get(flightNumber);
        if (flight == null) {
            flight = new FlightEntity();
            flight.setFlightNumber(flightNumber);
        }

        flight.setDestination(dest);
        flight.setOriginAirportCode(origin);
        flight.setDestinationAirportCode(destination);
        flight.setFlightDate(date);
        flight.setDepartureTime(dep);
        flight.setArrivalTime(arr);
        flight.setDurationMinutes(durationMinutes);
        flight.setPricePerPerson(basic);
        flight.setPriceExecutive(exec);
        flight.setPricePremium(prem);
        flight.setActive(true);
        return flight;
    }

    private BigDecimal scale(BigDecimal price, BigDecimal factor) {
        return price.multiply(factor).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Duração oficial (em minutos) do voo direto entre São Paulo (GRU) e cada destino.
     * Todos os voos de uma rota, independentemente do horário de partida, usam esta duração.
     */
    private static final Map<String, Integer> ROUTE_DURATION_MINUTES = Map.ofEntries(
        // Brasil
        Map.entry("Rio de Janeiro", 60),
        Map.entry("Gramado", 100),
        Map.entry("Foz do Iguaçu", 100),
        Map.entry("Bonito", 115),
        Map.entry("Pantanal", 140),
        Map.entry("Chapada Diamantina", 140),
        Map.entry("Jalapão", 165),
        Map.entry("Amazônia", 240),
        Map.entry("Fernando de Noronha", 260),
        // América do Sul
        Map.entry("Bariloche", 230),
        Map.entry("Atacama", 270),
        Map.entry("Patagônia", 280),
        Map.entry("Machu Picchu", 330),
        Map.entry("Bogotá", 340),
        Map.entry("Galápagos", 420),
        // Américas Central e do Norte
        Map.entry("Punta Cana", 400),
        Map.entry("Costa Rica", 420),
        Map.entry("Cancún", 560),
        Map.entry("Nova York", 590),
        Map.entry("Toronto", 600),
        Map.entry("Las Vegas", 720),
        Map.entry("Banff", 720),
        Map.entry("Yellowstone", 780),
        // Europa
        Map.entry("Lisboa", 590),
        Map.entry("Madri", 630),
        Map.entry("Barcelona", 650),
        Map.entry("Paris", 680),
        Map.entry("Londres", 690),
        Map.entry("Alpes Suíços", 690),
        Map.entry("Amsterdã", 700),
        Map.entry("Roma", 715),
        Map.entry("Veneza", 720),
        Map.entry("Florença", 740),
        Map.entry("Praga", 760),
        Map.entry("Atenas", 780),
        Map.entry("Istambul", 790),
        Map.entry("Santorini", 840),
        Map.entry("Reykjavík", 840),
        // África / Oriente Médio
        Map.entry("Parque Nacional Kruger", 600),
        Map.entry("Serengeti", 840),
        Map.entry("Dubai", 865),
        // Ásia / Oceania / Polar
        Map.entry("Antártida", 540),
        Map.entry("Maldivas", 1200),
        Map.entry("Sydney", 1260),
        Map.entry("Bangkok", 1380),
        Map.entry("Grande Barreira de Corais", 1380),
        Map.entry("Tóquio", 1440),
        Map.entry("Quioto", 1440),
        Map.entry("Seul", 1500)
    );

    private int getRealisticFlightDurationMinutes(String name, String country) {
        if (name != null && ROUTE_DURATION_MINUTES.containsKey(name.trim())) {
            return ROUTE_DURATION_MINUTES.get(name.trim());
        }
        if (country == null) country = "";
        if (name == null) name = "";
        String c = country.trim().toLowerCase();
        String n = name.trim().toLowerCase();

        // Brasil / Nacional
        if (c.equals("brasil")) {
            if (n.contains("rio") || n.contains("gramado") || n.contains("foz") || n.contains("bonito")) return 80; // 1h 20m
            if (n.contains("pantanal") || n.contains("chapada") || n.contains("jalapão")) return 140; // 2h 20m
            if (n.contains("amazônia") || n.contains("noronha")) return 230; // 3h 50m
            return 90;
        }

        // América do Sul
        if (c.contains("argentina") || c.contains("chile") || c.contains("peru") || c.contains("colômbia") || c.contains("equador")) {
            if (n.contains("bariloche") || n.contains("patagônia")) return 310; // 5h 10m
            if (n.contains("machu picchu") || n.contains("atacama") || n.contains("galápagos")) return 350; // 5h 50m
            return 260; // ~4h 20m (Bogotá, etc)
        }

        // América Central / Caribe / México
        if (c.contains("méxico") || c.contains("república dominicana") || c.contains("costa rica") || c.contains("caribe")) {
            return 510; // 8h 30m
        }

        // América do Norte (EUA / Canadá)
        if (c.contains("eua") || c.contains("canadá")) {
            if (n.contains("nova york")) return 580; // 9h 40m
            if (n.contains("las vegas") || n.contains("banff") || n.contains("yellowstone") || n.contains("toronto")) return 650; // 10h 50m
            return 600; // 10h
        }

        // Europa
        if (c.contains("itália") || c.contains("frança") || c.contains("grécia") || c.contains("turquia") ||
            c.contains("reino unido") || c.contains("república tcheca") || c.contains("portugal") ||
            c.contains("holanda") || c.contains("espanha") || c.contains("suíça") || c.contains("islândia")) {
            if (n.contains("lisboa")) return 580; // 9h 40m
            if (n.contains("madri") || n.contains("paris") || n.contains("londres")) return 660; // 11h
            if (n.contains("roma") || n.contains("atenas") || n.contains("istambul") || n.contains("reykjavík")) return 730; // 12h 10m
            return 680; // ~11h 20m
        }

        // África / Oriente Médio
        if (c.contains("emirados") || c.contains("dubai") || c.contains("tanzânia") || c.contains("áfrica do sul")) {
            if (n.contains("dubai")) return 860; // 14h 20m
            return 720; // 12h
        }

        // Ásia / Oceania
        if (c.contains("japão") || c.contains("coreia") || c.contains("tailândia") || c.contains("austrália") || c.contains("maldivas")) {
            if (n.contains("tóquio") || n.contains("quioto")) return 1440; // 24h (com conexão padrão de SP ao Japão)
            if (n.contains("seul") || n.contains("bangkok") || n.contains("sydney")) return 1380; // 23h
            if (n.contains("maldivas")) return 1140; // 19h
            return 1320; // 22h
        }

        // Antártida
        if (n.contains("antártida") || c.contains("antártida")) {
            return 540; // 9h
        }

        return 480; // Default 8h
    }
}
