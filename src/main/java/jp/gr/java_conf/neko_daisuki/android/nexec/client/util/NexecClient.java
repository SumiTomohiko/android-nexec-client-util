package jp.gr.java_conf.neko_daisuki.android.nexec.client.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import jp.gr.java_conf.neko_daisuki.android.nexec.client.share.INexecCallback;
import jp.gr.java_conf.neko_daisuki.android.nexec.client.share.INexecService;
import jp.gr.java_conf.neko_daisuki.android.nexec.client.share.SessionId;

public class NexecClient {

    public static class Settings {

        private static class Pair {

            private String name;
            private String value;

            public Pair(String name, String value) {
                this.name = name;
                this.value = value;
            }
        }

        private static class FileMap {

            private String dest;
            private String src;

            public FileMap(String dest, String src) {
                this.dest = dest;
                this.src = src;
            }
        }

        public String host;
        public int port = 57005;
        public String[] args;
        public String[] files;
        public boolean x;
        public int xWidth;
        public int xHeight;

        private List<Pair> environment;
        private List<FileMap> fileMap;

        public Settings() {
            environment = new ArrayList<Pair>();
            fileMap = new ArrayList<FileMap>();
        }

        public void addLink(String dest, String src) {
            fileMap.add(new FileMap(dest, src));
        }

        public void addEnvironment(String name, String value) {
            environment.add(new Pair(name, value));
        }
    }

    public interface OnErrorListener {

        public static class Nop implements OnErrorListener {

            @Override
            public void onError(NexecClient nexecClient, Throwable e) {
            }

            @Override
            public void onServiceError(NexecClient nexecClient,
                                       String message) {
            }
        }

        public static final OnErrorListener NOP = new Nop();

        public void onError(NexecClient nexecClient, Throwable e);
        public void onServiceError(NexecClient nexecClient, String message);
    }

    public interface OnStdoutListener {

        public static class FakeOnStdoutListener implements OnStdoutListener {

            @Override
            public void onWrite(NexecClient nexecClient, byte[] buf) {
            }
        }

        public static final OnStdoutListener NOP = new FakeOnStdoutListener();

        public void onWrite(NexecClient nexecClient, byte[] buf);
    }

    public interface OnStderrListener {

        public static class FakeOnStderrListener implements OnStderrListener {

            @Override
            public void onWrite(NexecClient nexecClient, byte[] buf) {
            }
        }

        public static final OnStderrListener NOP = new FakeOnStderrListener();

        public void onWrite(NexecClient nexecClient, byte[] buf);
    }

    public interface OnExitListener {

        public class FakeOnExitListener implements OnExitListener {

            @Override
            public void onExit(NexecClient nexecClient, int exitCode) {
            }
        }

        public static final OnExitListener NOP = new FakeOnExitListener();

        public void onExit(NexecClient nexecClient, int exitCode);
    }

    public interface OnXInvalidateListener {

        public static class FakeOnXInvalidateListener implements OnXInvalidateListener {

            @Override
            public void onInvalidate(int left, int top, int right, int bottom) {
            }
        }

        public static final OnXInvalidateListener NOP = new FakeOnXInvalidateListener();

        public void onInvalidate(int left, int top, int right, int bottom);
    }

    private class Callback extends INexecCallback.Stub {

        @Override
        public void writeStdout(byte[] buf) throws RemoteException {
            mOnStdoutListener.onWrite(NexecClient.this, buf);
        }

        @Override
        public void writeStderr(byte[] buf) throws RemoteException {
            mOnStderrListener.onWrite(NexecClient.this, buf);
        }

        @Override
        public void exit(int status) throws RemoteException {
            mOnExitListener.onExit(NexecClient.this, status);
            mSessionId = SessionId.NULL;
        }

        @Override
        public void xInvalidate(int left, int top, int right, int bottom) throws RemoteException {
            mOnXInvalidateListener.onInvalidate(left, top, right, bottom);
        }

        @Override
        public void error(String message) throws RemoteException {
            mOnErrorListener.onServiceError(NexecClient.this, message);
        }
    }

    private abstract class ConnectedProc {

        public final void run() {
            try {
                work();
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public abstract void work() throws RemoteException;
    }

    private class ExecutingConnectedProc extends ConnectedProc {

        @Override
        public void work() throws RemoteException {
            mService.execute(mSessionId, mCallback);
        }
    }

    private class ConnectingConnectedProc extends ConnectedProc {

        @Override
        public void work() throws RemoteException {
            mService.connect(mSessionId, mCallback);
        }
    }

    private class Connection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = INexecService.Stub.asInterface(service);
            mConnectedProc.run();
            mOperations = TRUE_OPERATIONS;

            Log.i(LOG_TAG, "Connected with the service.");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            changeStateToDisconnected();
            Log.i(LOG_TAG, "Disconnected from the service.");
        }
    }

    private abstract class Operations {

        public void disconnect(SessionId sessionId) {
            try {
                doDisconnect(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public void quit(SessionId sessionId) {
            try {
                doQuit(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public Bitmap xDraw(SessionId sessionId) {
            try {
                return doXDraw(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
            return null;
        }

        public void xMotionNotify(SessionId sessionId, int x, int y) {
            try {
                doXMotionNotify(sessionId, x, y);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public void xLeftButtonRelease(SessionId sessionId) {
            try {
                doXLeftButtonRelease(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public void xLeftButtonPress(SessionId sessionId) {
            try {
                doXLeftButtonPress(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public void xRightButtonRelease(SessionId sessionId) {
            try {
                doXRightButtonRelease(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public void xRightButtonPress(SessionId sessionId) {
            try {
                doXRightButtonPress(sessionId);
            }
            catch (RemoteException e) {
                mOnErrorListener.onError(NexecClient.this, e);
            }
        }

        public abstract void doDisconnect(SessionId sessionId) throws RemoteException;
        public abstract void doQuit(SessionId sessionId) throws RemoteException;
        public abstract Bitmap doXDraw(SessionId sessionId) throws RemoteException;
        public abstract void doXLeftButtonPress(SessionId sessionId) throws RemoteException;
        public abstract void doXLeftButtonRelease(SessionId sessionId) throws RemoteException;
        public abstract void doXMotionNotify(SessionId sessionId, int x, int y) throws RemoteException;
        public abstract void doXRightButtonPress(SessionId sessionId) throws RemoteException;
        public abstract void doXRightButtonRelease(SessionId sessionId) throws RemoteException;
    }

    private class NopOperations extends Operations {

        @Override
        public void doDisconnect(SessionId sessionId) throws RemoteException {
        }

        @Override
        public void doQuit(SessionId sessionId) throws RemoteException {
        }

        @Override
        public Bitmap doXDraw(SessionId sessionId) throws RemoteException {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }

        @Override
        public void doXLeftButtonPress(SessionId sessionId) throws RemoteException {
        }

        @Override
        public void doXMotionNotify(SessionId sessionId, int x, int y) throws RemoteException {
        }

        @Override
        public void doXLeftButtonRelease(SessionId sessionId) throws RemoteException {
        }

        @Override
        public void doXRightButtonPress(SessionId sessionId) throws RemoteException {
        }

        @Override
        public void doXRightButtonRelease(SessionId sessionId) throws RemoteException {
        }
    }

    private class TrueOperations extends Operations {

        @Override
        public void doDisconnect(SessionId sessionId) throws RemoteException {
            mService.disconnect(sessionId);
            unbind();
        }

        @Override
        public void doQuit(SessionId sessionId) throws RemoteException {
            mService.quit(sessionId);
            unbind();
        }

        @Override
        public Bitmap doXDraw(SessionId sessionId) throws RemoteException {
            return mService.xDraw(sessionId);
        }

        @Override
        public void doXLeftButtonPress(SessionId sessionId) throws RemoteException {
            mService.xLeftButtonPress(sessionId);
        }

        @Override
        public void doXMotionNotify(SessionId sessionId, int x, int y) throws RemoteException {
            mService.xMotionNotify(sessionId, x, y);
        }

        @Override
        public void doXLeftButtonRelease(SessionId sessionId) throws RemoteException {
            mService.xLeftButtonRelease(sessionId);
        }

        @Override
        public void doXRightButtonPress(SessionId sessionId) throws RemoteException {
            mService.xRightButtonPress(sessionId);
        }

        @Override
        public void doXRightButtonRelease(SessionId sessionId) throws RemoteException {
            mService.xRightButtonRelease(sessionId);
        }
    }

    private static final String PACKAGE = "jp.gr.java_conf.neko_daisuki.android.nexec.client";
    private static final String LOG_TAG = "NexecClient";

    private final Operations TRUE_OPERATIONS = new TrueOperations();
    private final Operations NOP_OPERATIONS = new NopOperations();
    private final ConnectedProc EXECUTING_CONNECTED_PROC = new ExecutingConnectedProc();
    private final ConnectedProc CONNECTING_CONNECTED_PROC = new ConnectingConnectedProc();

    // documents
    private SessionId mSessionId;
    private OnExitListener mOnExitListener = OnExitListener.NOP;
    private OnStdoutListener mOnStdoutListener = OnStdoutListener.NOP;
    private OnStderrListener mOnStderrListener = OnStderrListener.NOP;
    private OnErrorListener mOnErrorListener = OnErrorListener.NOP;
    private OnXInvalidateListener mOnXInvalidateListener = OnXInvalidateListener.NOP;

    // helpers
    private Activity mActivity;
    private Connection mConnection;
    private INexecService mService;
    private Operations mOperations;
    private ConnectedProc mConnectedProc;
    private INexecCallback mCallback = new Callback();

    public NexecClient(Activity activity) {
        mActivity = activity;
        mConnection = new Connection();
        changeStateToDisconnected();
        setOnErrorListener(null);
    }

    public SessionId getSessionId() {
        return mSessionId;
    }

    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l != null ? l : OnErrorListener.NOP;
    }

    public void setOnXInvalidateListener(OnXInvalidateListener l) {
        mOnXInvalidateListener = l != null ? l : OnXInvalidateListener.NOP;
    }

    public void setOnExitListener(OnExitListener l) {
        mOnExitListener = l != null ? l : OnExitListener.NOP;
    }

    public void setOnStdoutListener(OnStdoutListener l) {
        mOnStdoutListener = l != null ? l : OnStdoutListener.NOP;
    }

    public void setOnStderrListener(OnStderrListener l) {
        mOnStderrListener = l != null ? l : OnStderrListener.NOP;
    }

    public void request(Settings settings, int requestCode) {
        Intent intent = new Intent();
        intent.setClassName(PACKAGE, getClassName("MainActivity"));
        intent.putExtra("HOST", settings.host);
        intent.putExtra("PORT", settings.port);
        intent.putExtra("ARGS", settings.args);
        intent.putExtra("ENV", encodeEnvironment(settings.environment));
        intent.putExtra("FILES", settings.files);
        intent.putExtra("FILE_MAP", encodeFileMap(settings.fileMap));
        intent.putExtra("X", settings.x);
        intent.putExtra("X_WIDTH", settings.xWidth);
        intent.putExtra("X_HEIGHT", settings.xHeight);
        mActivity.startActivityForResult(intent, requestCode);
    }

    public boolean execute(SessionId sessionId) {
        return connect(sessionId, EXECUTING_CONNECTED_PROC);
    }

    public boolean connect(SessionId sessionId) {
        return connect(sessionId, CONNECTING_CONNECTED_PROC);
    }

    public boolean connect(SessionId sessionId, ConnectedProc connectedProc) {
        mSessionId = sessionId;
        if (mSessionId.isNull()) {
            return true;
        }

        mConnectedProc = connectedProc;

        Intent intent = new Intent(INexecService.class.getName());
        intent.putExtra("SESSION_ID", mSessionId.toString());
        mActivity.startService(intent);
        int flags = Context.BIND_AUTO_CREATE;
        return mActivity.bindService(intent, mConnection, flags);
    }

    public void disconnect() {
        mOperations.disconnect(mSessionId);
        changeStateToDisconnected();
    }

    public void quit() {
        mOperations.quit(mSessionId);
        changeStateToDisconnected();
    }

    public Bitmap xDraw() {
        return mOperations.xDraw(mSessionId);
    }

    public void xMotionNotify(int x, int y) {
        mOperations.xMotionNotify(mSessionId, x, y);
    }

    public void xLeftButtonRelease() {
        mOperations.xLeftButtonRelease(mSessionId);
    }

    public void xLeftButtonPress() {
        mOperations.xLeftButtonPress(mSessionId);
    }

    public void xRightButtonRelease() {
        mOperations.xRightButtonRelease(mSessionId);
    }

    public void xRightButtonPress() {
        mOperations.xRightButtonPress(mSessionId);
    }

    private String getClassName(String name) {
        return String.format("%s.%s", PACKAGE, name);
    }

    private String[] encodeEnvironment(List<Settings.Pair> environment) {
        List<String> l = new LinkedList<String>();
        for (Settings.Pair pair: environment) {
            l.add(encodePair(pair.name, pair.value));
        }
        return l.toArray(new String[0]);
    }

    private String[] encodeFileMap(List<Settings.FileMap> fileMap) {
        List<String> l = new LinkedList<String>();
        for (Settings.FileMap entry: fileMap) {
            l.add(encodePair(entry.dest, entry.src));
        }
        return l.toArray(new String[0]);
    }

    private String encodePair(String name, String value) {
        return String.format("%s:%s", escape(name), escape(value));
    }

    private String escape(String s) {
        StringBuilder buffer = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            buffer.append((c != ':') && (c != '\\') ? "" : "\\").append(c);
        }
        return buffer.toString();
    }

    private void changeStateToDisconnected() {
        mSessionId = SessionId.NULL;
        mOperations = NOP_OPERATIONS;
    }

    private void unbind() {
        mActivity.unbindService(mConnection);
    }
}

/**
 * vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
 */
