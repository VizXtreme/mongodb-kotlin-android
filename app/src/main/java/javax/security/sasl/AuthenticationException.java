package javax.security.sasl;

public class AuthenticationException extends SaslException {
    private static final long serialVersionUID = -357904090309489547L;

    public AuthenticationException() {
        super();
    }

    public AuthenticationException(String detail) {
        super(detail);
    }

    public AuthenticationException(String detail, Throwable ex) {
        super(detail, ex);
    }
}
