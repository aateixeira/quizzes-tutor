package pt.ulisboa.tecnico.socialsoftware.tutor.question;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.Course;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.LatexQuestionExportVisitor;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.QuestionsXmlImport;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.XMLQuestionExportVisitor;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.*;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.*;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.ImageRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.OptionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.repository.QuizQuestionRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.COURSE_NOT_FOUND;
import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.QUESTION_NOT_FOUND;

@Service
public class QuestionService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto findQuestionById(Integer questionId) {
        return questionRepository.findById(questionId).map(QuestionDtoFactory::getQuestionDto)
                .orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto findQuestionByKey(Integer key) {
        return questionRepository.findByKey(key).map(QuestionDtoFactory::getQuestionDto)
                .orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, key));
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<QuestionDto> findQuestions(int courseId) {
        return questionRepository.findQuestions(courseId).stream().map(QuestionDtoFactory::getQuestionDto).collect(Collectors.toList());
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<QuestionDto> findAvailableQuestions(int courseId) {
        return questionRepository.findAvailableQuestions(courseId).stream().map(QuestionDtoFactory::getQuestionDto).collect(Collectors.toList());
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto createQuestion(int courseId, MultipleChoiceQuestionDto questionDto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new TutorException(COURSE_NOT_FOUND, courseId));
        MultipleChoiceQuestion question = new MultipleChoiceQuestion(course, questionDto);
        questionRepository.save(question);
        return new MultipleChoiceQuestionDto(question);
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto createQuestion(int courseId, CodeFillInQuestionDto questionDto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new TutorException(COURSE_NOT_FOUND, courseId));
        CodeFillInQuestion question = new CodeFillInQuestion(course, questionDto);
        questionRepository.save(question);
        return new CodeFillInQuestionDto(question);
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto updateQuestion(Integer questionId, MultipleChoiceQuestionDto questionDto) {
        MultipleChoiceQuestion question = (MultipleChoiceQuestion) questionRepository
                .findById(questionId)
                .orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.update(questionDto);
        return new MultipleChoiceQuestionDto(question);
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto updateQuestion(Integer questionId, CodeFillInQuestionDto questionDto) {
        // todo Should we use a specific query to find the MultipleChoiceQuestion directly from the db?
        CodeFillInQuestion question = (CodeFillInQuestion) questionRepository
                .findById(questionId)
                .orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.update(questionDto);
        return new CodeFillInQuestionDto(question);
    }


    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeQuestion(Integer questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.remove();
        questionRepository.delete(question);
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void questionSetStatus(Integer questionId, Question.Status status) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.setStatus(status);
    }


    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void uploadImage(Integer questionId, String type) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));

        Image image = question.getImage();

        if (image == null) {
            image = new Image();

            question.setImage(image);

            imageRepository.save(image);
        }

        question.getImage().setUrl(question.getCourse().getName().replaceAll("\\s", "") +
                question.getCourse().getType() +
                question.getKey() +
                "." + type);
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateQuestionTopics(Integer questionId, TopicDto[] topics) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));

        question.updateTopics(Arrays.stream(topics).map(topicDto -> topicRepository.findTopicByName(question.getCourse().getId(), topicDto.getName())).collect(Collectors.toSet()));
    }

    public String exportQuestionsToXml() {
        XMLQuestionExportVisitor xmlExporter = new XMLQuestionExportVisitor();

        return xmlExporter.export(questionRepository.findAll());
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void importQuestionsFromXml(String questionsXML) {
        QuestionsXmlImport xmlImporter = new QuestionsXmlImport();

        xmlImporter.importQuestions(questionsXML, this, courseRepository);
    }


    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String exportQuestionsToLatex() {
        LatexQuestionExportVisitor latexExporter = new LatexQuestionExportVisitor();

        return latexExporter.export(questionRepository.findAll());
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ByteArrayOutputStream exportCourseQuestions(int courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new TutorException(COURSE_NOT_FOUND, courseId));

        String name = course.getName();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            List<Question> questions = new ArrayList<>(course.getQuestions());

            XMLQuestionExportVisitor xmlExport = new XMLQuestionExportVisitor();
            InputStream in = IOUtils.toInputStream(xmlExport.export(questions), StandardCharsets.UTF_8);
            zos.putNextEntry(new ZipEntry(name + ".xml"));
            copyToZipStream(zos, in);
            zos.closeEntry();

            LatexQuestionExportVisitor latexExport = new LatexQuestionExportVisitor();
            zos.putNextEntry(new ZipEntry(name + ".tex"));
            in = IOUtils.toInputStream(latexExport.export(questions), StandardCharsets.UTF_8);
            copyToZipStream(zos, in);
            zos.closeEntry();

            baos.flush();

            return baos;
        } catch (IOException ex) {
            throw new TutorException(ErrorMessage.CANNOT_OPEN_FILE);
        }
    }

    private void copyToZipStream(ZipOutputStream zos, InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }
        in.close();
    }


    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteQuizQuestion(QuizQuestion quizQuestion) {
        Question question = quizQuestion.getQuestion();
        quizQuestion.remove();
        quizQuestionRepository.delete(quizQuestion);

        if (question.getQuizQuestions().isEmpty()) {
            this.deleteQuestion(question);
        }
    }

    @Retryable(
            value = {SQLException.class},
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteQuestion(MultipleChoiceQuestion question) {
        for (Option option : question.getOptions()) {
            option.remove();
            optionRepository.delete(option);
        }
        deleteQuestion((Question) question);
    }

    private void deleteQuestion(Question question) {
        if (question.getImage() != null) {
            imageRepository.delete(question.getImage());
        }

        question.getTopics().forEach(topic -> topic.getQuestions().remove(question));
        question.getTopics().clear();

        questionRepository.delete(question);
    }
}

