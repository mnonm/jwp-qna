package qna.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qna.CannotDeleteException;
import qna.NotFoundException;
import qna.domain.Answer;
import qna.domain.AnswerRepository;
import qna.domain.Answers;
import qna.domain.ContentType;
import qna.domain.DeleteHistory;
import qna.domain.Question;
import qna.domain.QuestionRepository;
import qna.domain.User;

@Service
public class QnaService {
    private static final Logger log = LoggerFactory.getLogger(QnaService.class);

    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private DeleteHistoryService deleteHistoryService;

    public QnaService(QuestionRepository questionRepository, AnswerRepository answerRepository,
        DeleteHistoryService deleteHistoryService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.deleteHistoryService = deleteHistoryService;
    }

    @Transactional(readOnly = true)
    public Question findQuestionById(Long id) {
        return questionRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void deleteQuestion(User loginUser, Long questionId) throws CannotDeleteException {
        Question question = findQuestionById(questionId);
        question.delete(loginUser);

        Answers answers = Answers.from(answerRepository.findByQuestionIdAndDeletedFalse(questionId));
        answers.deleteAll(loginUser);

        List<DeleteHistory> deleteHistories = new ArrayList<>();
        deleteHistories.add(new DeleteHistory(ContentType.QUESTION, questionId, question.getWriter()));
        for (Answer answer : answers.getValues()) {
            deleteHistories.add(
                new DeleteHistory(ContentType.ANSWER, answer.getId(), answer.getWriter()));
        }
        deleteHistoryService.saveAll(deleteHistories);
    }

}
