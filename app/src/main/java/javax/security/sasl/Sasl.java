package javax.security.sasl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class Sasl {
    public static final String QOP = "javax.security.sasl.qop";
    public static final String STRENGTH = "javax.security.sasl.strength";
    public static final String SERVER_AUTH = "javax.security.sasl.server.authentication";
    public static final String BOUND_SERVER_NAME = "javax.security.sasl.bound.server.name";
    public static final String MAX_BUFFER = "javax.security.sasl.maxbuffer";
    public static final String RAW_SEND_SIZE = "javax.security.sasl.rawsendsize";
    public static final String REUSE = "javax.security.sasl.reuse";
    public static final String POLICY_NOPLAINTEXT = "javax.security.sasl.policy.noplaintext";
    public static final String POLICY_NOACTIVE = "javax.security.sasl.policy.noactive";
    public static final String POLICY_NODICTIONARY = "javax.security.sasl.policy.nodictionary";
    public static final String POLICY_NOANONYMOUS = "javax.security.sasl.policy.noanonymous";
    public static final String POLICY_FORWARD_SECRECY = "javax.security.sasl.policy.forwardsecrecy";
    public static final String POLICY_PASS_CREDENTIALS = "javax.security.sasl.policy.credentials";
    public static final String CREDENTIALS = "javax.security.sasl.credentials";
    public static final String SEND_BUFFER = "javax.security.sasl.sendmaxbuffer";

    private Sasl() {
    }

    public static SaslClient createSaslClient(
            String[] mechanisms,
            String authorizationId,
            String protocol,
            String serverName,
            Map<String, ?> props,
            Object cbh) throws SaslException {
        return null;
    }

    public static Enumeration<?> getSaslClientFactories() {
        return Collections.emptyEnumeration();
    }
}
