package jp.gr.java_conf.neko_daisuki.android.nexec.client.util;



public class NexecHost {

    private String mHost;
    private int mPort;

    public NexecHost(String host, int port) {
        mHost = host;
        mPort = port;
    }

    public NexecHost(String host) {
        this(host, 57005);
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public String toString() {
        return String.format("NexecHost(host=%s, port=%d)", mHost, mPort);
    }
}