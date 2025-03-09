package faang.school.accountservice.exception.customExceptions;

public class DataValidationException extends RuntimeException{

    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataValidationException(String message) {
        super(message);
    }
}
