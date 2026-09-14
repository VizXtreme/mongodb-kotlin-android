package javax.security.sasl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

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
            CallbackHandler cbh) throws SaslException {
        for (String mech : mechanisms) {
            if ("PLAIN".equalsIgnoreCase(mech)) {
                return new PlainClient(authorizationId, cbh);
            }
        }
        return null;
    }

    public static Enumeration<SaslClientFactory> getSaslClientFactories() {
        return Collections.emptyEnumeration();
    }

    private static class PlainClient implements SaslClient {
        private final String authorizationId;
        private final CallbackHandler cbh;
        private boolean completed = false;

        PlainClient(String authorizationId, CallbackHandler cbh) {
            this.authorizationId = authorizationId;
            this.cbh = cbh;
        }

        @Override
        public String getMechanismName() {
            return "PLAIN";
        }

        @Override
        public boolean hasInitialResponse() {
            return true;
        }

        @Override
        public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
            if (completed) {
                throw new SaslException("PLAIN authentication already completed");
            }
            completed = true;
            try {
                NameCallback nc = new NameCallback("User name:");
                PasswordCallback pc = new PasswordCallback("Password:", false);
                cbh.handle(new Callback[]{nc, pc});

                String authId = authorizationId != null ? authorizationId : "";
                String user = nc.getName() != null ? nc.getName() : "";
                char[] pw = pc.getPassword();
                String pass = pw != null ? new String(pw) : "";
                pc.clearPassword();

                // RFC 4616: [authzid] UTF8NUL authcid UTF8NUL passwd
                byte[] authIdBytes = authId.getBytes("UTF-8");
                byte[] userBytes = user.getBytes("UTF-8");
                byte[] passBytes = pass.getBytes("UTF-8");

                byte[] response = new byte[authIdBytes.length + 1 + userBytes.length + 1 + passBytes.length];
                System.arraycopy(authIdBytes, 0, response, 0, authIdBytes.length);
                response[authIdBytes.length] = 0;
                System.arraycopy(userBytes, 0, response, authIdBytes.length + 1, userBytes.length);
                response[authIdBytes.length + 1 + userBytes.length] = 0;
                System.arraycopy(passBytes, 0, response, authIdBytes.length + 1 + userBytes.length + 1, passBytes.length);

                return response;
            } catch (Exception e) {
                throw new SaslException("PLAIN authentication failed", e);
            }
        }

        @Override
        public boolean isComplete() {
            return completed;
        }

        @Override
        public byte[] unwrap(byte[] incoming, int offset, int len) {
            throw new IllegalStateException("PLAIN does not support integrity or privacy");
        }

        @Override
        public byte[] wrap(byte[] outgoing, int offset, int len) {
            throw new IllegalStateException("PLAIN does not support integrity or privacy");
        }

        @Override
        public Object getNegotiatedProperty(String propName) {
            return null;
        }

        @Override
        public void dispose() {
        }
    }
}
