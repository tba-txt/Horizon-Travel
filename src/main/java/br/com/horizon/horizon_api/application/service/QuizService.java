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
import java.util.stream.Collectors;
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
            throw new BusinessException("O orçamento deve estar entre R$ 1.000,00 e R$ 200.000,00.");
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
        
        // Normalize answers (support single answer or multiple answers per question)
        Map<Long, List<Long>> normalizedAnswers = new HashMap<>();
        if (req.getAnswers() != null) {
            for (Map.Entry<Long, Object> entry : req.getAnswers().entrySet()) {
                Long qId = entry.getKey();
                Object val = entry.getValue();
                List<Long> aIds = new ArrayList<>();
                if (val instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Number n) {
                            aIds.add(n.longValue());
                        } else if (item != null) {
                            try {
                                aIds.add(Long.valueOf(item.toString().trim()));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                } else if (val instanceof Number n) {
                    aIds.add(n.longValue());
                } else if (val != null) {
                    try {
                        aIds.add(Long.valueOf(val.toString().trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (!aIds.isEmpty()) {
                    normalizedAnswers.put(qId, aIds);
                }
            }
        }
        
        // Mandatory question 1
        if (mandatoryQuestionId != null && (!normalizedAnswers.containsKey(mandatoryQuestionId) || normalizedAnswers.get(mandatoryQuestionId).isEmpty())) {
            throw new BusinessException("A pergunta obrigatória deve ser respondida.");
        }
        
        List<Long> allAnswerIds = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : normalizedAnswers.entrySet()) {
            Long qId = entry.getKey();
            List<Long> aIds = entry.getValue();
            if (!validQuestionIds.contains(qId)) {
                throw new BusinessException("Pergunta inválida para o quiz ativo: " + qId);
            }
            List<Answer> validAnswers = answerPort.getAnswersByQuestionId(qId);
            Set<Long> validAnswerIds = validAnswers.stream().map(Answer::getId).collect(Collectors.toSet());
            for (Long aId : aIds) {
                if (!validAnswerIds.contains(aId)) {
                    throw new BusinessException("Resposta inválida para a pergunta: " + qId);
                }
                allAnswerIds.add(aId);
            }
        }
        
        UUID attemptId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        List<QuizResponse> history = new ArrayList<>();
        
        List<Answer> answers = answerPort.getAnswersByIds(allAnswerIds);
        
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
        
        // Save Budget & Passport preference
        UserProfile profile = userProfilePort.findByUserId(req.getUserId()).orElse(new UserProfile());
        profile.setUserId(req.getUserId());
        profile.setBudgetPerPerson(req.getBudgetPerPerson());
        profile.setHasPassport(req.getHasPassport() != null ? req.getHasPassport() : true);
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(now);
        }
        profile.setUpdatedAt(now);
        userProfilePort.save(profile);
    }
}
