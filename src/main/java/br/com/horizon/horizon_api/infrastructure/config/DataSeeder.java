package br.com.horizon.horizon_api.infrastructure.config;

import br.com.horizon.horizon_api.infrastructure.persistence.entity.*;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TourismType;
import br.com.horizon.horizon_api.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Map<String, AttributeEntity> attributes = new HashMap<>();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        fixSequences();
        seedAttributes();
        seedDestinations();
        seedQuiz();
        seedPosts();
        seedFlights();
    }

    private void fixSequences() {
        try {
            jdbcTemplate.execute("SELECT setval('attributes_id_seq', coalesce((SELECT MAX(id)+1 FROM attributes), 1), false);");
            jdbcTemplate.execute("SELECT setval('destinations_id_seq', coalesce((SELECT MAX(id)+1 FROM destinations), 1), false);");
            jdbcTemplate.execute("SELECT setval('quizzes_id_seq', coalesce((SELECT MAX(id)+1 FROM quizzes), 1), false);");
            jdbcTemplate.execute("SELECT setval('questions_id_seq', coalesce((SELECT MAX(id)+1 FROM questions), 1), false);");
            jdbcTemplate.execute("SELECT setval('answers_id_seq', coalesce((SELECT MAX(id)+1 FROM answers), 1), false);");
            jdbcTemplate.execute("SELECT setval('posts_id_seq', coalesce((SELECT MAX(id)+1 FROM posts), 1), false);");
            jdbcTemplate.execute("SELECT setval('flights_id_seq', coalesce((SELECT MAX(id)+1 FROM flights), 1), false);");
        } catch(Exception e) {}
    }

    private void seedAttributes() {
        String[] attrNames = {"FRIO", "NEVE", "VIDA_SELVAGEM", "ECOTURISMO", "NATUREZA", "PRAIA", "CALOR", "MERGULHO", "RELAXAMENTO", "GASTRONOMIA", "CULTURA", "HISTORIA", "VIDA_NOTURNA", "URBANO", "AVENTURA", "ROMANTICO"};
        for (String name : attrNames) {
            attributes.put(name, upsertAttr(name));
        }
    }

    private AttributeEntity upsertAttr(String name) {
        return attributeRepository.findAll().stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    AttributeEntity attr = new AttributeEntity();
                    attr.setName(name);
                    attr.setDescription("Atributo: " + name);
                    return attributeRepository.save(attr);
                });
    }

    private void seedDestinations() {
        List<String> validCities = Arrays.asList("São Paulo", "Rio de Janeiro", "Paris", "Tóquio", "Antártida", "Amazônia", "Serengeti", "Galápagos");
        List<DestinationEntity> existingDestinations = destinationRepository.findAll();
        
        for (DestinationEntity dest : existingDestinations) {
            if (!validCities.contains(dest.getName())) {
                dest.setActive(false);
                destinationRepository.save(dest);
            }
        }

        destinationAttributeRepository.deleteAll();

        // COMUM
        upsertDestination("São Paulo", "Brasil", TourismType.COMUM, Map.of("URBANO", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 1.0, "VIDA_NOTURNA", 1.0));
        upsertDestination("Rio de Janeiro", "Brasil", TourismType.COMUM, Map.of("PRAIA", 1.0, "CALOR", 1.0, "RELAXAMENTO", 1.0));
        upsertDestination("Paris", "França", TourismType.COMUM, Map.of("CULTURA", 1.0, "GASTRONOMIA", 1.0, "HISTORIA", 1.0, "ROMANTICO", 1.0));
        upsertDestination("Tóquio", "Japão", TourismType.COMUM, Map.of("URBANO", 1.0, "GASTRONOMIA", 1.0, "CULTURA", 1.0, "VIDA_NOTURNA", 1.0));

        // ECOTURISMO
        upsertDestination("Antártida", "Antártida", TourismType.ECOTURISMO, Map.of("FRIO", 1.0, "NEVE", 1.0, "VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0));
        upsertDestination("Amazônia", "Brasil", TourismType.ECOTURISMO, Map.of("NATUREZA", 1.0, "ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 1.0));
        upsertDestination("Serengeti", "Tanzânia", TourismType.ECOTURISMO, Map.of("VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "AVENTURA", 1.0, "NATUREZA", 1.0));
        upsertDestination("Galápagos", "Equador", TourismType.ECOTURISMO, Map.of("VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "NATUREZA", 1.0, "MERGULHO", 1.0));
    }

    private void upsertDestination(String city, String country, TourismType type, Map<String, Double> attrScores) {
        DestinationEntity dest = destinationRepository.findAll().stream()
                .filter(d -> d.getName().equals(city))
                .findFirst()
                .orElseGet(() -> {
                    DestinationEntity d = new DestinationEntity();
                    d.setName(city);
                    d.setCity(city);
                    d.setCreatedAt(OffsetDateTime.now());
                    return d;
                });

        dest.setCountry(country);
        dest.setDescription("Um destino incrível em " + city);
        dest.setTourismType(type);
        dest.setImageUrl("https://images.unsplash.com/photo-1469854523086-cc02fe5d8800");
        if (dest.getBasePrice() == null) {
            dest.setBasePrice(BigDecimal.valueOf(5000));
        }
        dest.setActive(true);
        dest = destinationRepository.save(dest);

        for (Map.Entry<String, Double> entry : attrScores.entrySet()) {
            AttributeEntity attr = attributes.get(entry.getKey());
            if (attr != null) {
                upsertDestAttr(dest, attr, BigDecimal.valueOf(entry.getValue()));
            }
        }
    }

    private void upsertDestAttr(DestinationEntity dest, AttributeEntity attr, BigDecimal score) {
        DestinationAttributeEntity da = destinationAttributeRepository.findAll().stream()
                .filter(a -> a.getDestination().getId().equals(dest.getId()) && a.getAttribute().getId().equals(attr.getId()))
                .findFirst()
                .orElseGet(() -> {
                    DestinationAttributeEntity newDa = new DestinationAttributeEntity();
                    newDa.getId().setDestinationId(dest.getId());
                    newDa.getId().setAttributeId(attr.getId());
                    newDa.setDestination(dest);
                    newDa.setAttribute(attr);
                    return newDa;
                });
        da.setScore(score);
        destinationAttributeRepository.save(da);
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

        createAnswer(q1, "Ecoturismo", Map.of("ECOTURISMO", 1.0));
        createAnswer(q1, "Viagem tradicional", Map.of());

        // PERGUNTA 2 - NÃO OBRIGATÓRIA (questionOrder = 2)
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

        createAnswer(q2, "Sol e calor", Map.of("CALOR", 1.0));
        createAnswer(q2, "Neve e frio", Map.of("FRIO", 1.0, "NEVE", 1.0));
        createAnswer(q2, "Clima ameno e fresco", Map.of("FRIO", 0.5, "CALOR", 0.5));
        createAnswer(q2, "Qualquer clima", Map.of());

        // PERGUNTA 3 - NÃO OBRIGATÓRIA (questionOrder = 3)
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
        createAnswer(q3, "Aventura", Map.of("AVENTURA", 1.0));
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
        
        for (Map.Entry<String, Double> entry : attrScores.entrySet()) {
            AttributeEntity attr = attributes.get(entry.getKey());
            if (attr != null) {
                AnswerAttributeEntity aa = answerAttributeRepository.findAll().stream()
                        .filter(x -> x.getAnswer().getId().equals(ans.getId()) && x.getAttribute().getId().equals(attr.getId()))
                        .findFirst()
                        .orElseGet(() -> {
                            AnswerAttributeEntity newAa = new AnswerAttributeEntity();
                            newAa.getId().setAnswerId(ans.getId());
                            newAa.getId().setAttributeId(attr.getId());
                            newAa.setAnswer(ans);
                            newAa.setAttribute(attr);
                            return newAa;
                        });
                aa.setWeight(BigDecimal.valueOf(entry.getValue()));
                answerAttributeRepository.save(aa);
            }
        }
    }

    private void seedPosts() {
        List<PostEntity> oldPosts = postRepository.findAll();
        for(PostEntity p : oldPosts) {
            p.setPublished(false);
            postRepository.save(p);
        }

        postAttributeRepository.deleteAll();

        upsertPost("Pequenos gigantes do gelo", "Observar a vida selvagem da Antártida é uma experiência marcada por paisagens extremas, grandes extensões de gelo e espécies adaptadas a um dos ambientes mais singulares do planeta.", "Antártida", Map.of("FRIO", 1.0, "NEVE", 1.0, "VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0));
        upsertPost("Um mergulho nas águas do Nordeste", "Conhecer praias maravilhosas com muito sol e águas cristalinas.", "Rio de Janeiro", Map.of("PRAIA", 1.0, "CALOR", 1.0, "RELAXAMENTO", 1.0));
        upsertPost("Sabores que contam histórias", "A gastronomia local traz séculos de história em cada prato.", "Paris", Map.of("CULTURA", 1.0, "GASTRONOMIA", 1.0, "HISTORIA", 1.0, "ROMANTICO", 1.0));
        upsertPost("Luzes e vida noturna agitada", "A cidade nunca dorme, oferecendo inúmeras opções de diversão à noite.", "Tóquio", Map.of("URBANO", 1.0, "GASTRONOMIA", 1.0, "CULTURA", 1.0, "VIDA_NOTURNA", 1.0));
        upsertPost("O coração da selva", "Explorar a floresta é descobrir a maior biodiversidade do mundo.", "Amazônia", Map.of("NATUREZA", 1.0, "ECOTURISMO", 1.0, "VIDA_SELVAGEM", 1.0, "AVENTURA", 1.0));
        upsertPost("A grande migração", "Observar milhares de animais em seu habitat natural é algo inesquecível.", "Serengeti", Map.of("VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "AVENTURA", 1.0, "NATUREZA", 1.0));
        upsertPost("Evolução em tempo real", "Ilhas que inspiraram teorias e abrigam espécies únicas.", "Galápagos", Map.of("VIDA_SELVAGEM", 1.0, "ECOTURISMO", 1.0, "NATUREZA", 1.0, "MERGULHO", 1.0));
        upsertPost("A cidade que nunca dorme", "Cultura, gastronomia e ritmo acelerado, tudo no mesmo lugar.", "São Paulo", Map.of("URBANO", 1.0, "CULTURA", 1.0, "GASTRONOMIA", 1.0, "VIDA_NOTURNA", 1.0));
    }

    private void upsertPost(String title, String caption, String destName, Map<String, Double> attrScores) {
        DestinationEntity dest = destinationRepository.findAll().stream()
                .filter(d -> d.getName().equals(destName) && d.getActive())
                .findFirst().orElse(null);
        if (dest == null) return;

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
        post.setImageUrl("https://images.unsplash.com/photo-1469854523086-cc02fe5d8800");
        post.setDestination(dest);
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

    private void upsertPostAttr(PostEntity post, AttributeEntity attr, BigDecimal weight) {
        PostAttributeEntity pa = postAttributeRepository.findAll().stream()
                .filter(p -> p.getPost().getId().equals(post.getId()) && p.getAttribute().getId().equals(attr.getId()))
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
        List<DestinationEntity> activeDestinations = destinationRepository.findAll().stream()
                .filter(DestinationEntity::getActive)
                .toList();

        Map<String, String> airportCodes = Map.of(
                "São Paulo", "CGH",
                "Rio de Janeiro", "GIG",
                "Paris", "CDG",
                "Tóquio", "HND",
                "Antártida", "TNM",
                "Amazônia", "MAO",
                "Serengeti", "JRO",
                "Galápagos", "GPS"
        );

        // Desativar voos legados que não pertençam à grade oficial padronizada (evita códigos antigos como GAL, RIO, etc.)
        List<FlightEntity> legacyFlights = flightRepository.findAll().stream()
                .filter(f -> f.getActive() != null && f.getActive())
                .filter(f -> f.getFlightNumber() == null || !f.getFlightNumber().matches("^HZ\\d+[DR]\\d+$"))
                .toList();
        for (FlightEntity f : legacyFlights) {
            f.setActive(false);
            flightRepository.save(f);
        }

        // Configuração determinística de voos por direção:
        // Dia relativo, horários, número do voo (sufixo), preços (Básico, Executivo, Premium), assentos (total, disponíveis)
        class FlightSchedule {
            final int dayOffset;
            final LocalTime dep;
            final LocalTime arr;
            final String numSuffix;
            final BigDecimal basic;
            final BigDecimal exec;
            final BigDecimal prem;
            final int totalSeats;
            final int availSeats;

            FlightSchedule(int dayOffset, LocalTime dep, LocalTime arr, String numSuffix,
                           double basic, double exec, double execMult, double premMult,
                           int totalSeats, int availSeats) {
                this.dayOffset = dayOffset;
                this.dep = dep;
                this.arr = arr;
                this.numSuffix = numSuffix;
                this.basic = BigDecimal.valueOf(basic);
                this.exec = BigDecimal.valueOf(execMult);
                this.prem = BigDecimal.valueOf(premMult);
                this.totalSeats = totalSeats;
                this.availSeats = availSeats;
            }
        }

        // 7 opções de IDA (GRU -> Destino) distribuídas de 5 dias a 4 meses
        List<FlightSchedule> outboundSchedules = List.of(
                new FlightSchedule(5, LocalTime.of(6, 30), LocalTime.of(12, 45), "01", 1850, 0, 3200, 5100, 120, 85),
                new FlightSchedule(10, LocalTime.of(9, 15), LocalTime.of(15, 30), "03", 2100, 0, 3700, 5900, 150, 42),
                new FlightSchedule(18, LocalTime.of(14, 0), LocalTime.of(20, 15), "05", 2400, 0, 4200, 6800, 100, 96),
                new FlightSchedule(25, LocalTime.of(21, 45), LocalTime.of(4, 0), "07", 1950, 0, 3400, 5400, 140, 20),
                new FlightSchedule(40, LocalTime.of(8, 0), LocalTime.of(14, 15), "09", 2250, 0, 3900, 6200, 120, 110),
                new FlightSchedule(65, LocalTime.of(11, 30), LocalTime.of(17, 45), "11", 2600, 0, 4600, 7400, 160, 145),
                new FlightSchedule(95, LocalTime.of(16, 20), LocalTime.of(22, 35), "13", 2050, 0, 3600, 5700, 130, 75)
        );

        // 7 opções de VOLTA (Destino -> GRU) compatíveis com intervalos de retorno
        List<FlightSchedule> returnSchedules = List.of(
                new FlightSchedule(12, LocalTime.of(7, 0), LocalTime.of(13, 15), "02", 1900, 0, 3300, 5200, 120, 64),
                new FlightSchedule(20, LocalTime.of(10, 30), LocalTime.of(16, 45), "04", 2150, 0, 3800, 6000, 150, 38),
                new FlightSchedule(28, LocalTime.of(15, 10), LocalTime.of(21, 25), "06", 2450, 0, 4300, 6900, 100, 88),
                new FlightSchedule(35, LocalTime.of(22, 0), LocalTime.of(4, 15), "08", 2000, 0, 3500, 5500, 140, 15),
                new FlightSchedule(50, LocalTime.of(8, 45), LocalTime.of(15, 0), "10", 2300, 0, 4000, 6400, 120, 105),
                new FlightSchedule(75, LocalTime.of(13, 15), LocalTime.of(19, 30), "12", 2700, 0, 4750, 7600, 160, 130),
                new FlightSchedule(105, LocalTime.of(18, 0), LocalTime.of(0, 15), "14", 2100, 0, 3650, 5800, 130, 90)
        );

        LocalDate today = LocalDate.now();

        for (DestinationEntity dest : activeDestinations) {
            String destCode = airportCodes.getOrDefault(
                    dest.getName(),
                    dest.getName().length() >= 3 ? dest.getName().substring(0, 3).toUpperCase() : "XYZ"
            );

            String originCode = "GRU";

            // Semear voos de IDA (Origin -> Destino)
            for (FlightSchedule s : outboundSchedules) {
                String flightNumber = "HZ" + dest.getId() + "D" + s.numSuffix;
                LocalDate flightDate = today.plusDays(s.dayOffset);
                upsertFlight(dest, originCode, destCode, flightDate, s.dep, s.arr, flightNumber,
                        s.basic, s.exec, s.prem, s.totalSeats, s.availSeats);
            }

            // Semear voos de VOLTA (Destino -> Origin)
            for (FlightSchedule s : returnSchedules) {
                String flightNumber = "HZ" + dest.getId() + "R" + s.numSuffix;
                LocalDate flightDate = today.plusDays(s.dayOffset);
                upsertFlight(dest, destCode, originCode, flightDate, s.dep, s.arr, flightNumber,
                        s.basic, s.exec, s.prem, s.totalSeats, s.availSeats);
            }
        }
    }

    private void upsertFlight(DestinationEntity dest, String origin, String destination,
                              LocalDate date, LocalTime dep, LocalTime arr, String flightNumber,
                              BigDecimal basic, BigDecimal exec, BigDecimal prem,
                              int totalSeats, int availSeats) {
        FlightEntity flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseGet(() -> {
                    FlightEntity f = new FlightEntity();
                    f.setFlightNumber(flightNumber);
                    return f;
                });

        flight.setDestination(dest);
        flight.setOriginAirportCode(origin);
        flight.setDestinationAirportCode(destination);
        flight.setFlightDate(date);
        flight.setDepartureTime(dep);
        flight.setArrivalTime(arr);
        flight.setPricePerPerson(basic);
        flight.setPriceExecutive(exec);
        flight.setPricePremium(prem);
        flight.setActive(true);
        flight = flightRepository.save(flight);

        final Long flightId = flight.getId();
        FlightAvailabilityEntity availability = flightAvailabilityRepository.findByFlightId(flightId)
                .orElseGet(() -> {
                    FlightAvailabilityEntity a = new FlightAvailabilityEntity();
                    return a;
                });

        availability.setFlight(flight);
        availability.setTotalSeats(totalSeats);
        availability.setAvailableSeats(availSeats);
        availability.setUpdatedAt(OffsetDateTime.now());
        flightAvailabilityRepository.save(availability);
    }
}
