package jp.gr.java_conf.neko_daisuki.android.nexec.client.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import android.app.Activity;
import android.content.Intent;
import android.util.JsonReader;
import android.util.JsonWriter;

import jp.gr.java_conf.neko_daisuki.android.nexec.client.share.NexecConstants;
import jp.gr.java_conf.neko_daisuki.android.nexec.client.share.SessionId;

public class NexecUtil {

    private static final String NAME_HOST = "host";
    private static final String NAME_PORT = "port";
    private static final String PACKAGE = "jp.gr.java_conf.neko_daisuki.android.nexec.client";

    public static void startHostPreferenceActivity(Activity activity,
                                                   int requestCode,
                                                   String host, int port) {
        Intent intent = new Intent();
        String clazz = String.format("%s.HostPreferenceActivity", PACKAGE);
        intent.setClassName(PACKAGE, clazz);
        intent.putExtra(NexecConstants.EXTRA_HOST, host);
        intent.putExtra(NexecConstants.EXTRA_PORT, port);
        activity.startActivityForResult(intent, requestCode);
    }

    public static NexecHost getHost(Intent data) {
        String host = data.getStringExtra(NexecConstants.EXTRA_HOST);
        int port = data.getIntExtra(NexecConstants.EXTRA_PORT, 57005);
        return new NexecHost(host, port);
    }

    public static NexecHost readHostFromJson(Reader in) throws IOException {
        JsonReader reader = new JsonReader(in);
        try {
            String host = "";
            int port = 57005;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (NAME_HOST.equals(name)) {
                    host = reader.nextString();
                }
                else if (NAME_PORT.equals(name)) {
                    port = reader.nextInt();
                }
            }
            reader.endObject();

            return new NexecHost(host, port);
        }
        finally {
            reader.close();
        }
    }

    public static NexecHost readHostFromJson(String path) throws FileNotFoundException, IOException {
        Reader in = new FileReader(path);
        try {
            Reader reader = new BufferedReader(in);
            try {
                return readHostFromJson(in);
            }
            finally {
                reader.close();
            }
        }
        finally {
            in.close();
        }
    }

    public static void writeHostToJson(Writer out, NexecHost host) throws IOException {
        JsonWriter writer = new JsonWriter(out);
        try {
            writer.beginObject();
            writer.name(NAME_HOST).value(host.getHost());
            writer.name(NAME_PORT).value(host.getPort());
            writer.endObject();
        }
        finally {
            writer.close();
        }
    }

    public static void writeHostToJson(String path, NexecHost host) throws IOException {
        Writer out = new FileWriter(path);
        try {
            Writer writer = new BufferedWriter(out);
            try {
                writeHostToJson(writer, host);
            }
            finally {
                writer.close();
            }
        }
        finally {
            out.close();
        }
    }

    public static SessionId getSessionId(Intent data) {
        return new SessionId(data.getStringExtra("SESSION_ID"));
    }

    public static SessionId readSessionId(InputStream in) throws IOException {
        try {
            Reader reader = new InputStreamReader(in);
            String line = new BufferedReader(reader).readLine();
            return line != null ? new SessionId(line) : SessionId.NULL;
        }
        finally {
            in.close();
        }
    }

    public static void writeSessionId(OutputStream out, SessionId sessionId) throws IOException {
        PrintWriter writer = new PrintWriter(out);
        try {
            writer.print(sessionId.toString());
        }
        finally {
            writer.close();
        }
    }
}