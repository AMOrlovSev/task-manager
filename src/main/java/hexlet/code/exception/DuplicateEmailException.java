package hexlet.code.exception;

public class DuplicateEmailException extends DuplicateResourceException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
