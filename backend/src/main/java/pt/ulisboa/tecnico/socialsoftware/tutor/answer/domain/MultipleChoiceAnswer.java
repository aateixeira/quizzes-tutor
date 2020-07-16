package pt.ulisboa.tecnico.socialsoftware.tutor.answer.domain;

import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.*;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.Visitor;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.MultipleChoiceQuestion;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Option;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.QUESTION_OPTION_MISMATCH;

@Entity
@DiscriminatorValue(Question.QuestionTypes.MULTIPLE_CHOICE_QUESTION)
public class MultipleChoiceAnswer extends AnswerType {

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option option;

    public MultipleChoiceAnswer() {
        super();
    }

    public MultipleChoiceAnswer(QuestionAnswer questionAnswer){
        super(questionAnswer);
    }

    public MultipleChoiceAnswer(QuestionAnswer questionAnswer, Option option){
        super(questionAnswer);
        this.setOption(option);
    }

    public Option getOption() {
        return option;
    }

    public void setOption(Option option) {
        this.option = option;

        if (option != null)
            option.addQuestionAnswer(this.getQuestionAnswer());
    }

    public void setOption(MultipleChoiceStatementAnswerDto multipleChoiceStatementAnswerDto) {
        if (multipleChoiceStatementAnswerDto.getOptionId() != null) {
            Option option = this.getQuestion().getOptions().stream()
                    .filter(option1 -> option1.getId().equals(multipleChoiceStatementAnswerDto.getOptionId()))
                    .findAny()
                    .orElseThrow(() -> new TutorException(QUESTION_OPTION_MISMATCH, multipleChoiceStatementAnswerDto.getOptionId()));

            if (this.getOption() != null) {
                this.getOption().getQuestionAnswers().remove(this.getQuestionAnswer());
            }

            this.setOption(option);
        } else {
            this.setOption((Option) null);
        }
    }

    // TODO[is->has] -> Understand how to remove this.
    private MultipleChoiceQuestion getQuestion(){
        return (MultipleChoiceQuestion)this.getQuestionAnswer().getQuestion().getQuestion();
    }

    @Override
    public boolean isCorrect() {
        return getOption() != null && getOption().isCorrect();
    }


    public void remove() {
        if (option != null) {
            option.getQuestionAnswers().remove(this.getQuestionAnswer());
            option = null;
        }
    }

    @Override
    public AnswerTypeDto getAnswerTypeDto() {
        return new MultipleChoiceAnswerDto(this);
    }

    @Override
    public StatementAnswerDetailsDto getStatementAnswerDetailsDto() {
        return new MultipleChoiceStatementAnswerDto(this);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitAnswerType(this);
    }
}