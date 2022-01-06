package com.baidu.idl.face.platform.stat;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetUtil {
    private static final String TAG = "NetUtil";
    private static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static <T> void uploadData(NetUtil.RequestAdapter<T> adapter) {
        boolean requireRetry = false;
        int retryCount = adapter.getRetryCount();
        boolean code = false;
        Object exception = null;
        T result = null;
        HttpsURLConnection conn = null;
        OutputStream out = null;
        InputStream in = null;

        do {
            try {
                boolean var36;
                try {
                    requireRetry = false;
                    var36 = false;
                    exception = null;
                    result = null;
                    URL e = new URL(adapter.getURL());
                    trustAllHosts();
                    conn = (HttpsURLConnection)e.openConnection();
                    conn.setConnectTimeout(adapter.getConnectTimeout());
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setReadTimeout(adapter.getReadTimeout());
                    conn.setRequestMethod(adapter.getRequestMethod());
                    conn.setUseCaches(false);
                    conn.setHostnameVerifier(DO_NOT_VERIFY);
                    out = conn.getOutputStream();
                    out.write(adapter.getRequestString().getBytes("UTF-8"));
                    out.flush();
                    if (conn.getResponseCode() != 200) {
                        var36 = true;
                        new IllegalStateException("ResponseCode: " + conn.getResponseCode());
                    } else {
                        in = conn.getInputStream();
                        adapter.parseResponse(in);
                    }
                } catch (SocketTimeoutException var32) {
                    var32.printStackTrace();
                    requireRetry = true;
                    var36 = true;
                } catch (IOException var33) {
                    var33.printStackTrace();
                    var36 = true;
                } catch (JSONException var34) {
                    var34.printStackTrace();
                    var36 = true;
                } catch (Exception var35) {
                    var35.printStackTrace();
                    var36 = true;
                }
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException var31) {
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException var30) {
                    }
                }

                if (conn != null) {
                    conn.disconnect();
                }

            }
        } while(requireRetry && retryCount-- > 0);

    }

    public static boolean isConnected(Context context) {
        if (context == null) {
            return false;
        } else {
            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo == null ? false : activeNetworkInfo.isConnected();
        }
    }

    private NetUtil() {
        throw new RuntimeException("This class instance can not be created.");
    }

    private static void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init((KeyManager[])null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    public abstract static class RequestAdapter<T> {
        public static final int RESPONSE_STATUS_NORMAL = 0;
        public static final int RESPONSE_STATUS_ERROR_TIMEOUT = 1;
        public static final int RESPONSE_STATUS_ERROR_IO = 2;
        public static final int RESPONSE_STATUS_ERROR_PARSE_JSON = 3;
        public static final int RESPONSE_STATUS_ERROR_RESPONSE_CODE = 4;
        public static final int RESPONSE_STATUS_ERROR_UNKNOWN = 5;
        private static final int RETRY_COUNT = 2;
        private static final int CONNECT_TIMEOUT = 5000;
        private static final int READ_TIMEOUT = 5000;
        private static final String REQUEST_METHOD = "POST";

        public RequestAdapter() {
        }

        public abstract String getURL();

        public abstract String getRequestString();

        public abstract void parseResponse(InputStream var1) throws IOException, JSONException;

        public int getRetryCount() {
            return 0;
        }

        public int getConnectTimeout() {
            return 5000;
        }

        public int getReadTimeout() {
            return 5000;
        }

        public String getRequestMethod() {
            return "POST";
        }
    }
}