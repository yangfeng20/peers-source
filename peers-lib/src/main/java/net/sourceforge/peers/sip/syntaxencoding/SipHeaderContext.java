package net.sourceforge.peers.sip.syntaxencoding;

import java.util.HashMap;
import java.util.Map;

public class SipHeaderContext {

    private static final ThreadLocal<Map<String, String>> SIP_HEADER_THREAD_LOCAL = ThreadLocal.withInitial(HashMap::new);

    public static void setSipHeader(Map<String, String> sipHeader) {
        SIP_HEADER_THREAD_LOCAL.set(sipHeader);
    }

    public static Map<String, String> getSipHeader() {
        return SIP_HEADER_THREAD_LOCAL.get();
    }

    public static void clearSipHeader() {
        SIP_HEADER_THREAD_LOCAL.remove();
    }
}
