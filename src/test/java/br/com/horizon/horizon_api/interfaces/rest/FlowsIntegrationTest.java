package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.application.dto.request.*;
import br.com.horizon.horizon_api.application.dto.response.*;
import br.com.horizon.horizon_api.application.service.InteractionService;
import br.com.horizon.horizon_api.application.service.QuizService;
import br.com.horizon.horizon_api.application.usecase.*;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.InteractionType;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.PaymentMethod;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.ReservationStatus;
import br.com.horizon.horizon_api.infrastructure.persistence.entity.enums.TripType;
import br.com.horizon.horizon_api.interfaces.rest.advice.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlowsIntegrationTest {

    private MockMvc mockMvcAuth;
    private MockMvc mockMvcQuiz;
    private MockMvc mockMvcInteraction;
    private MockMvc mockMvcRecommendation;
    private MockMvc mockMvcDestination;
    private MockMvc mockMvcReservation;
    private MockMvc mockMvcPayment;
    private MockMvc mockMvcTicket;

    @Mock private LoginUseCase loginUseCase;
    @Mock private GoogleLoginUseCase googleLoginUseCase;
    @Mock private ForgotPasswordUseCase forgotPasswordUseCase;
    @Mock private ResetPasswordUseCase resetPasswordUseCase;
    @Mock private CreateUserUseCase createUserUseCase;

    @Mock private QuizService quizService;
    @Mock private InteractionService interactionService;
    @Mock private GetRecommendationsUseCase recommendationUseCase;

    @Mock private GetDestinationWeatherUseCase destinationWeatherUseCase;
    @Mock private GetDestinationByIdUseCase destinationByIdUseCase;
    @Mock private ListDestinationsUseCase listDestinationsUseCase;

    @Mock private CreateReservationUseCase createReservationUseCase;
    @Mock private GetReservationByIdUseCase getReservationByIdUseCase;
    @Mock private ListUserReservationsUseCase listUserReservationsUseCase;
    @Mock private CancelReservationUseCase cancelReservationUseCase;
    @Mock private GetReservationTicketsUseCase getReservationTicketsUseCase;

    @Mock private ConfirmPaymentUseCase confirmPaymentUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Contexto autenticado simulando usuário ID 10
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null, Collections.emptyList())
        );

        GlobalExceptionHandler advice = new GlobalExceptionHandler();

        AuthController authController = new AuthController(loginUseCase, googleLoginUseCase, forgotPasswordUseCase, resetPasswordUseCase, createUserUseCase);
        mockMvcAuth = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(advice).build();

        QuizController quizController = new QuizController(quizService);
        mockMvcQuiz = MockMvcBuilders.standaloneSetup(quizController).setControllerAdvice(advice).build();

        InteractionController interactionController = new InteractionController(interactionService);
        mockMvcInteraction = MockMvcBuilders.standaloneSetup(interactionController).setControllerAdvice(advice).build();

        RecommendationController recommendationController = new RecommendationController(recommendationUseCase);
        mockMvcRecommendation = MockMvcBuilders.standaloneSetup(recommendationController).setControllerAdvice(advice).build();

        DestinationController destinationController = new DestinationController(destinationWeatherUseCase, destinationByIdUseCase, listDestinationsUseCase);
        mockMvcDestination = MockMvcBuilders.standaloneSetup(destinationController).setControllerAdvice(advice).build();

        ReservationController reservationController = new ReservationController(createReservationUseCase, getReservationByIdUseCase, listUserReservationsUseCase, cancelReservationUseCase, getReservationTicketsUseCase);
        mockMvcReservation = MockMvcBuilders.standaloneSetup(reservationController).setControllerAdvice(advice).build();

        PaymentController paymentController = new PaymentController(confirmPaymentUseCase);
        mockMvcPayment = MockMvcBuilders.standaloneSetup(paymentController).setControllerAdvice(advice).build();

        TicketController ticketController = new TicketController(null, null, null, null, null, getReservationTicketsUseCase);
        mockMvcTicket = MockMvcBuilders.standaloneSetup(ticketController).setControllerAdvice(advice).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Fluxo 1: Cadastro de Usuário
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 1: Cadastro de usuário via POST /auth/register retorna 201 Created")
    void testUserRegistrationFlow() throws Exception {
        UserRequest request = new UserRequest();
        request.setName("Gabriel Oliveira");
        request.setEmail("gabriel@horizon.com");
        request.setPassword("SenhaForte123!");
        request.setCpf("12345678901");
        request.setBirthDate(LocalDate.of(2000, 5, 20));

        UserResponse mockResponse = new UserResponse();
        mockResponse.setId(10L);
        mockResponse.setName("Gabriel Oliveira");
        mockResponse.setEmail("gabriel@horizon.com");

        when(createUserUseCase.execute(any(UserRequest.class))).thenReturn(mockResponse);

        mockMvcAuth.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Gabriel Oliveira"))
                .andExpect(jsonPath("$.email").value("gabriel@horizon.com"));

        verify(createUserUseCase).execute(any(UserRequest.class));
    }

    // -------------------------------------------------------------------------
    // Fluxo 2: Autenticação / Login
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 2: Login via POST /auth/login retorna 200 OK e token JWT")
    void testLoginFlow() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("gabriel@horizon.com");
        request.setPassword("SenhaForte123!");

        LoginResponse mockResponse = new LoginResponse();
        mockResponse.setToken("mocked-jwt-token-string");

        when(loginUseCase.execute(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvcAuth.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-string"));

        verify(loginUseCase).execute(any(LoginRequest.class));
    }

    // -------------------------------------------------------------------------
    // Fluxo 3: Recuperação de Acesso
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 3: Esqueci minha senha e Redefinição via /auth/forgot-password e /auth/reset-password")
    void testPasswordRecoveryFlow() throws Exception {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("gabriel@horizon.com");

        mockMvcAuth.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk());
        verify(forgotPasswordUseCase).execute(any(ForgotPasswordRequest.class));

        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken("reset-token-xyz");
        resetReq.setNewPassword("NovaSenhaSegura123!");

        mockMvcAuth.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());
        verify(resetPasswordUseCase).execute(any(ResetPasswordRequest.class));
    }

    // -------------------------------------------------------------------------
    // Fluxo 4: Questionário / Quiz
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 4: Obtenção de quiz ativo e submissão com orçamento via /quizzes/active e /quizzes/submit")
    void testQuizFlow() throws Exception {
        QuizResponseDTO quizDto = new QuizResponseDTO();
        quizDto.setId(1L);
        quizDto.setTitle("Quiz de Perfil de Viajante Horizon");
        when(quizService.execute()).thenReturn(quizDto);

        mockMvcQuiz.perform(get("/quizzes/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Quiz de Perfil de Viajante Horizon"));

        SubmitQuizRequest submitReq = new SubmitQuizRequest();
        submitReq.setBudgetPerPerson(new BigDecimal("4500.00"));
        submitReq.setHasPassport(true);
        submitReq.setAnswers(Map.of(1L, 10L, 2L, 20L));

        mockMvcQuiz.perform(post("/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk());

        verify(quizService).execute(any(SubmitQuizRequest.class));
    }

    // -------------------------------------------------------------------------
    // Fluxo 5: Interação no Feed (Like / Dislike)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 5: Interação de like no feed via POST /interactions atualizando perfil comportamental")
    void testFeedInteractionFlow() throws Exception {
        InteractionRequest req = new InteractionRequest();
        req.setPostId(5L);
        req.setInteractionType(InteractionType.LIKE);

        mockMvcInteraction.perform(post("/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(interactionService).execute(any(InteractionRequest.class));
    }

    // -------------------------------------------------------------------------
    // Fluxo 6: Recomendações
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 6: Obtenção de destinos recomendados via GET /recommendations")
    void testRecommendationsFlow() throws Exception {
        RecommendationResponseDTO rec = new RecommendationResponseDTO();
        rec.setDestinationId(1L);
        rec.setDestinationName("Machu Picchu");
        rec.setScore(new BigDecimal("0.9500"));
        rec.setExplanation("Alta compatibilidade com suas preferências de montanha e história.");

        when(recommendationUseCase.execute(eq(10L), anyInt())).thenReturn(List.of(rec));

        mockMvcRecommendation.perform(get("/recommendations").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinationId").value(1))
                .andExpect(jsonPath("$[0].destinationName").value("Machu Picchu"))
                .andExpect(jsonPath("$[0].score").value(0.95))
                .andExpect(jsonPath("$[0].explanation").value("Alta compatibilidade com suas preferências de montanha e história."));

        verify(recommendationUseCase).execute(eq(10L), eq(5));
    }

    // -------------------------------------------------------------------------
    // Fluxo 7: Catálogo de Destinos
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 7: Listagem e detalhes de destinos via GET /destinations")
    void testDestinationsCatalogFlow() throws Exception {
        DestinationResponse d = new DestinationResponse();
        d.setId(1L);
        d.setName("Fernando de Noronha");
        d.setBasePrice(new BigDecimal("2500.00"));

        when(listDestinationsUseCase.execute(anyInt(), anyInt())).thenReturn(List.of(d));
        when(destinationByIdUseCase.execute(1L)).thenReturn(d);

        mockMvcDestination.perform(get("/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fernando de Noronha"));

        mockMvcDestination.perform(get("/destinations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fernando de Noronha"));
    }

    // -------------------------------------------------------------------------
    // Fluxo 8: Consulta Climática
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 8: Consulta de clima do destino via GET /destinations/{id}/weather")
    void testWeatherIntegrationFlow() throws Exception {
        DestinationWeatherResponse weatherResp = new DestinationWeatherResponse();
        weatherResp.setDestinationId(1L);
        weatherResp.setDestinationName("Fernando de Noronha");
        weatherResp.setTargetDate(LocalDate.now().plusDays(10));
        DestinationWeatherResponse.ForecastDto info = DestinationWeatherResponse.ForecastDto.builder()
                .weatherDescription("Ensolarado")
                .maxTemperature(30.5)
                .build();
        weatherResp.setForecast(info);

        when(destinationWeatherUseCase.execute(eq(1L), any())).thenReturn(weatherResp);

        mockMvcDestination.perform(get("/destinations/1/weather")
                        .param("travelDate", LocalDate.now().plusDays(10).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationName").value("Fernando de Noronha"))
                .andExpect(jsonPath("$.forecast.weatherDescription").value("Ensolarado"));

        verify(destinationWeatherUseCase).execute(eq(1L), any());
    }

    // -------------------------------------------------------------------------
    // Fluxo 9: Criação de Reserva (Checkout)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 9: Criação de reserva via POST /reservations/checkout em status PENDENTE")
    void testReservationCheckoutFlow() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setDestinationId(1L);
        req.setTripType(TripType.ONE_WAY);
        req.setOutboundFlightId(10L);

        PassengerRequest p = new PassengerRequest();
        p.setName("Gabriel Oliveira");
        p.setAge(26);
        req.setPassengers(List.of(p));

        ReservationResponse resp = new ReservationResponse();
        resp.setReservationId(100L);
        resp.setStatus(ReservationStatus.PENDENTE);
        resp.setTotalAmount(new BigDecimal("1200.00"));

        when(createReservationUseCase.execute(eq(10L), any(ReservationRequest.class))).thenReturn(resp);

        mockMvcReservation.perform(post("/reservations/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(100))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.totalAmount").value(1200.00));

        verify(createReservationUseCase).execute(eq(10L), any(ReservationRequest.class));
    }

    // -------------------------------------------------------------------------
    // Fluxo 10: Confirmação de Pagamento
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 10: Confirmação de pagamento via POST /payments/{reservationId}/confirm")
    void testPaymentConfirmationFlow() throws Exception {
        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethod(PaymentMethod.PIX);

        mockMvcPayment.perform(post("/payments/100/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(confirmPaymentUseCase).execute(eq(100L), eq(10L), eq(PaymentMethod.PIX));
    }

    // -------------------------------------------------------------------------
    // Fluxo 11: Emissão e Consulta de Bilhetes
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Fluxo 11: Consulta de bilhetes emitidos via GET /tickets/reservation/{id}")
    void testTicketEmissionAndRetrievalFlow() throws Exception {
        TicketResponse ticket = new TicketResponse();
        ticket.setId(500L);
        ticket.setReservationId(100L);
        ticket.setTicketNumber("HZ-2026-R100-P1-9876");
        ticket.setPassengerName("Gabriel Oliveira");

        when(getReservationTicketsUseCase.execute(eq(100L), eq(10L))).thenReturn(List.of(ticket));

        mockMvcTicket.perform(get("/tickets/reservation/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketNumber").value("HZ-2026-R100-P1-9876"))
                .andExpect(jsonPath("$[0].passengerName").value("Gabriel Oliveira"));

        verify(getReservationTicketsUseCase).execute(eq(100L), eq(10L));
    }
}
