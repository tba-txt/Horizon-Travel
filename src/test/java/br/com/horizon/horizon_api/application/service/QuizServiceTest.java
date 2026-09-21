package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.application.dto.request.SubmitQuizRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class QuizServiceTest {
    @Mock private QuizPersistencePort quizPort;
    @Mock private QuestionPersistencePort questionPort;
    @Mock private AnswerPersistencePort answerPort;
    @Mock private QuizResponsePersistencePort responsePort;
    @Mock private UserProfileAttributePersistencePort explicitPort;
    @Mock private UserPersistencePort userPort;
    @Mock private UserProfilePersistencePort userProfilePort;
    
    @InjectMocks private QuizService service;
    
    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); }
    
    @Test void shouldLoadActiveQuiz() {
        Quiz q = new Quiz(); q.setId(1L);
        when(quizPort.getActiveQuiz()).thenReturn(Optional.of(q));
        assertNotNull(service.execute());
    }
    
    @Test void shouldSubmitQuizAndCalculateExplicitProfile() {
        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(1L);
        Map<Long, Long> answers = new HashMap<>();
        answers.put(10L, 100L);
        answers.put(11L, 101L);
        req.setAnswers(answers);
        req.setBudgetPerPerson(new BigDecimal("1500"));
        
        User u = new User(); u.setId(1L);
        when(userPort.findById(1L)).thenReturn(Optional.of(u));
        
        Quiz q = new Quiz(); q.setId(1L);
        when(quizPort.getActiveQuiz()).thenReturn(Optional.of(q));
        
        Question q1 = new Question(); q1.setId(10L);
        Question q2 = new Question(); q2.setId(11L);
        when(questionPort.getQuestionsByQuizId(1L)).thenReturn(List.of(q1, q2));
        
        Answer a1 = new Answer(); a1.setId(100L); a1.setQuestionId(10L); a1.getAttributes().put(5L, new BigDecimal("0.8"));
        Answer a2 = new Answer(); a2.setId(101L); a2.setQuestionId(11L); a2.getAttributes().put(5L, new BigDecimal("0.4"));
        when(answerPort.getAnswersByIds(anyList())).thenReturn(List.of(a1, a2));
        when(answerPort.getAnswersByQuestionId(10L)).thenReturn(List.of(a1));
        when(answerPort.getAnswersByQuestionId(11L)).thenReturn(List.of(a2));
        
        service.execute(req);
        
        verify(responsePort).saveAll(anyList());
        verify(explicitPort).deleteByUserId(1L); // Ensure old is deleted
        verify(userProfilePort).save(any(UserProfile.class));
        verify(explicitPort).saveAll(argThat(list -> {
            if(list.size() != 1) return false;
            UserProfileAttribute upa = (UserProfileAttribute) list.get(0);
            return upa.getAttributeId().equals(5L) && upa.getScore().compareTo(new BigDecimal("0.6000")) == 0;
        }));
    }
}
