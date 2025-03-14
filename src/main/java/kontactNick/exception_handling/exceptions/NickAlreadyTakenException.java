package kontactNick.exception_handling.exceptions;


public class NickAlreadyTakenException extends RuntimeException {
    public NickAlreadyTakenException(String message) {
        super(message);
    }
}
