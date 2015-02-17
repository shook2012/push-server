package es.tor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class ClienteHttp {
    protected final static int TIMEOUT = 120 * 1000;

    public String get(String urlString) {
        return get(urlString, "UTF-8");
    }

    public String get(String urlString, String charset) {
        String html = "";
        try {
            URL url = new URL(urlString);

            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            urlConnection.setInstanceFollowRedirects(true);
            HttpsURLConnection.setFollowRedirects(true);
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(TIMEOUT);
            urlConnection.setReadTimeout(TIMEOUT);
            urlConnection.setRequestMethod("GET");

            try {
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    html = urlConnection.getResponseMessage();
                } else {
                    html = convertStreamToString(
                            urlConnection.getInputStream(), charset);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    urlConnection.disconnect();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            html = null;
        }

        return html;
    }

    public InputStream getStream(String url) {
        try {
            // Control de caracteres blanco
            if (url.contains(" "))
                url = url.replace(" ", "%20");

            URL aURL = new URL(url);
            HttpGet httpRequest = null;

            httpRequest = new HttpGet(aURL.toURI());

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = (HttpResponse) httpClient
                    .execute(httpRequest);

            HttpEntity entity = response.getEntity();
            BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            return bufHttpEntity.getContent();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public InputStream getStreamWithHeaders(String url, HttpServletResponse headers) {
        try {
            // Control de caracteres blanco
            if (url.contains(" "))
                url = url.replace(" ", "%20");

            URL aURL = new URL(url);
            HttpGet httpRequest = null;

            httpRequest = new HttpGet(aURL.toURI());
            
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = (HttpResponse) httpClient
                    .execute(httpRequest);
            
            for (Header header: response.getAllHeaders()) {
                headers.addHeader(header.getName(), header.getValue());
            }

            HttpEntity entity = response.getEntity();
            BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
            return bufHttpEntity.getContent();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String convertStreamToString(InputStream is, String charset) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(is, charset));
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (OutOfMemoryError e) {
            System.gc();

            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(is);
        }
        return sb.toString();
    }

    public void closeStream(InputStream is) {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected HttpURLConnection postToStream(String urlString,
            Map<String, String> params, String method) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }

        byte[] bytes = null;
        if (params != null && params.size() > 0) {
            StringBuilder bodyBuilder = new StringBuilder();
            Iterator<Entry<String, String>> iterator = params.entrySet()
                    .iterator();
            while (iterator.hasNext()) {
                Entry<String, String> param = iterator.next();
                bodyBuilder.append(param.getKey()).append('=')
                        .append(param.getValue());
                if (iterator.hasNext()) {
                    bodyBuilder.append('&');
                }
            }
            String body = bodyBuilder.toString();
            bytes = body.getBytes();
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            if (bytes != null) {
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(bytes.length);
            }
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");

            if (bytes != null) {
                OutputStream out = conn.getOutputStream();
                out.write(bytes);
                out.close();
            }
            return conn;

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    public int post(String urlString, Map<String, String> params,
            String method, File file) {
        HttpURLConnection conn = postToStream(urlString, params, method);
        if (conn == null)
            return -1;

        int response = -1;
        try {
            response = conn.getResponseCode();
        } catch (IOException e1) {
            e1.printStackTrace();
            return response;
        }

        InputStream is;
        try {
            file.getParentFile().mkdirs();

            OutputStream os = new FileOutputStream(file);

            int read = 0;
            byte[] bytes = new byte[1024];

            is = conn.getInputStream();

            while ((read = is.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }

            os.close();

            return response;

        } catch (Exception e) {
            return response == 200 ? -1 : response;
        }
    }

    public String post(String urlString, Map<String, String> params,
            String method) {
        HttpURLConnection conn = postToStream(urlString, params, method);
        if (conn == null)
            return null;

        try {
            if (conn.getResponseCode() != 200)
                return null;
            return convertStreamToString(conn.getInputStream(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HttpResponse post(String url, List<NameValuePair> nameValuePairs) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpContext localContext = new BasicHttpContext();

        try {
            HttpPost httpPost = new HttpPost(url);
            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            
            for (int index = 0; index < nameValuePairs.size(); index++) {
                if (nameValuePairs.get(index).getName()
                        .equalsIgnoreCase("imagen")) {
                    builder.addPart(nameValuePairs.get(index).getName(),
                            new FileBody(new File(nameValuePairs.get(index)
                                    .getValue()), ContentType.APPLICATION_OCTET_STREAM));
                } else {
                    builder.addPart(
                            nameValuePairs.get(index).getName(),
                            new StringBody(nameValuePairs.get(index).getValue(), ContentType.TEXT_PLAIN));
                }
            }

            httpPost.setEntity(builder.build());

            return httpClient.execute(httpPost, localContext);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isPost(String url, List<NameValuePair> nameValuePairs) {
        HttpResponse response = post(url, nameValuePairs);
        return response.getStatusLine().getStatusCode() == 200;
    }
}