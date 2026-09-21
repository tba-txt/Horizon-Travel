package br.com.horizon.horizon_api.application.service;
import br.com.horizon.horizon_api.domain.model.*;
import br.com.horizon.horizon_api.domain.port.*;
import br.com.horizon.horizon_api.domain.exception.ResourceNotFoundException;
import br.com.horizon.horizon_api.domain.exception.BusinessException;
import br.com.horizon.horizon_api.application.dto.request.SubmitQuizRequest;
import br.com.horizon.horizon_api.application.dto.response.*;
import br.com.horizon.horizon_api.application.usecase.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService implements GetActiveQuizUseCase, GetQuizQuestionsUseCase, SubmitQuizUseCase {
    private final QuizPersistencePort quizPort;
    private final QuestionPersistencePort questionPort;
    private final AnswerPersistencePort answerPort;
    private final QuizResponsePersistencePort responsePort;
    private final UserProfileAttributePersistencePort explicitPort;
    private final UserPersistencePort userPort;
    private final UserProfilePersistencePort userProfilePort;

    @Override
    public QuizResponseDTO execute() {
        Quiz q = quizPort.getActiveQuiz().orElseThrow(() -> new ResourceNotFoundException("No active quiz"));
        QuizResponseDTO dto = new QuizResponseDTO();
        dto.setId(q.getId());
        dto.setTitle(q.getTitle());
        dto.setVersion(q.getVersion());
        dto.setQuestions(this.execute(q.getId()));
        return dto;
    }
    
    @Override
    public List<QuestionResponseDTO> execute(Long quizId) {
        List<Question> qs = questionPort.getQuestionsByQuizId(quizId);
        List<QuestionResponseDTO> res = new ArrayList<>();
        for (Question q : qs) {
            QuestionResponseDTO qdto = new QuestionResponseDTO();
            qdto.setId(q.getId());
            qdto.setText(q.getQuestionText());
            qdto.setRequired(q.getQuestionOrder() != null && q.getQuestionOrder() == 1);
            List<Answer> ans = answerPort.getAnswersByQuestionId(q.getId());
            List<AnswerResponseDTO> adtos = new ArrayList<>();
            for (Answer a : ans) {
                AnswerResponseDTO adto = new AnswerResponseDTO();
                adto.setId(a.getId());
                adto.setText(a.getAnswerText());
                adtos.add(adto);
            }
            qdto.setAnswers(adtos);
            res.add(qdto);
        }
        return res;
    }
    
    @Override
    public void execute(SubmitQuizRequest req) {
        if (req.getBudgetPerPerson() == null || 
            req.getBudgetPerPerson().compareTo(new BigDecimal("1000")) < 0 || 
            req.getBudgetPerPerson().compareTo(new BigDecimal("200000")) > 0) {
            throw new BusinessException("O oramento deve estar entre R$ 1.000,00 e R$ 200.000,00.");
        }
        
        // Validate user
        userPort.findById(req.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Quiz q = quizPort.getActiveQuiz().orElseThrow(() -> new ResourceNotFoundException("No active quiz"));
        
        List<Question> qs = questionPort.getQuestionsByQuizId(q.getId());
        Set<Long> validQuestionIds = new HashSet<>();
        Long mandatoryQuestionId = null;
        for (Question question : qs) {
            validQuestionIds.add(question.getId());
            if (question.getQuestionOrder() != null && question.getQuestionOrder() == 1) {
                mandatoryQuestionId = question.getId();
            }
        }
        
        // Mandatory question 1
        if (mandatoryQuestionId != null && !req.getAnswers().containsKey(mandatoryQuestionId)) {
            throw new BusinessException("A pergunta obrigatória deve ser respondida.");
        }
        
        for (Map.Entry<Long, Long> entry : req.getAnswers().entrySet()) {
            Long qId = entry.getKey();
            Long aId = entry.getValue();
            if (!validQuestionIds.contains(qId)) {
                throw new BusinessException("Pergunta inválida para o quiz ativo: " + qId);
            }
            List<Answer> validAnswers = answerPort.getAnswersByQuestionId(qId);
            boolean isValidAnswer = validAnswers.stream().anyMatch(a -> a.getId().equals(aId));
            if (!isValidAnswer) {
                throw new BusinessException("Resposta inválida para a pergunta: " + qId);
            }
        }
        
        UUID attemptId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        List<QuizResponse> history = new ArrayList<>();
        
        List<Long> ansIds = new ArrayList<>(req.getAnswers().values());
        List<Answer> answers = answerPort.getAnswersByIds(ansIds);
        
        if (answers.size() != req.getAnswers().size()) {
            throw new BusinessException("Resposta inválida para a pergunta informada.");
        }
        
        for (Answer a : answers) {
            QuizResponse qr = new QuizResponse();
            qr.setUserId(req.getUserId());
            qr.setQuizId(q.getId());
            qr.setQuestionId(a.getQuestionId());
            qr.setAnswerId(a.getId());
            qr.setAttemptId(attemptId);
            qr.setAnsweredAt(now);
            history.add(qr);
        }
        responsePort.saveAll(history);
        
        // Calculate explicit profile
        explicitPort.deleteByUserId(req.getUserId());
        Map<Long, List<BigDecimal>> attrWeights = new HashMap<>();
        for (Answer a : answers) {
            if (a.getAttributes() != null) {
                for (Map.Entry<Long, BigDecimal> entry : a.getAttributes().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        attrWeights.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                    }
                }
            }
        }
        
        List<UserProfileAttribute> newProfile = new ArrayList<>();
        for (Map.Entry<Long, List<BigDecimal>> entry : attrWeights.entrySet()) {
            BigDecimal sum = entry.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal count = new BigDecimal(entry.getValue().size());
            BigDecimal avg = sum.divide(count, 4, RoundingMode.HALF_UP);
            
            UserProfileAttribute upa = new UserProfileAttribute();
            upa.setUserId(req.getUserId());
            upa.setAttributeId(entry.getKey());
            upa.setScore(avg);
            upa.setUpdatedAt(now);
            newProfile.add(upa);
        }
        
        explicitPort.saveAll(newProfile);
        
        // Save Budget
        UserProfile profile = new UserProfile();
        profile.setUserId(req.getUserId());
        profile.setBudgetPerPerson(req.getBudgetPerPerson());
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        userProfilePort.save(profile);
    }
}
