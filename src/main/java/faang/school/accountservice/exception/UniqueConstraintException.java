package faang.school.accountservice.exception;

public class UniqueConstraintException extends RuntimeException {

    public UniqueConstraintException(String message, Exception ex) {
        super(message, ex);
    }
}
