package vavi.net.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import vavi.net.ia.dotnet.QueryHelpers;
import vavi.util.Debug;


public class Client {

    public static final String name = "vavi-nio-file-ia";

    static XmlMapper jaxson = XmlMapper.xmlBuilder().build();

    static Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Integer.class, new JsonConverters.NullableStringToIntConverter())
            .registerTypeAdapter(LocalDateTime.class, new JsonConverters.LocalDateTimeNullableConverter())
            .registerTypeAdapter(ZonedDateTime.class, new JsonConverters.ZonedDateTimeNullableConverter())
//            .registerTypeAdapter(String[].class, new JsonConverters.EnumerableStringConverter())
//            .registerTypeAdapter(Number.class, new JsonConverters.NumberAdapter())
            // TODO It’s too much bother to register generic types.
            //  gson should be able to annotate generics types like "@Generics(types = {"Long", ...})"
            //  and gson should offer deserializers for list and map
            //  and about datetime related also
            .registerTypeAdapter(new TypeToken<List<Changes.GetResponse.Change>>(){}.getType(), new JsonConverters.ListConverter<Changes.GetResponse.Change>())
            .registerTypeAdapter(new TypeToken<List<String>>(){}.getType(), new JsonConverters.ListConverter<List<String>>())
            .registerTypeAdapter(new TypeToken<List<Long>>(){}.getType(), new JsonConverters.ListConverter<List<Long>>())
            .registerTypeAdapter(new TypeToken<List<Search.ScrapeResponseItem>>(){}.getType(), new JsonConverters.ListConverter<List<Search.ScrapeResponseItem>>())
            .registerTypeAdapter(new TypeToken<List<Relationships.GetChildrenResponse.Response_.Doc>>(){}.getType(), new JsonConverters.ListConverter<List<Relationships.GetChildrenResponse.Response_.Doc>>())
            .registerTypeAdapter(new TypeToken<List<ZonedDateTime>>(){}.getType(), new JsonConverters.ListConverter<List<ZonedDateTime>>())
            .registerTypeAdapter(new TypeToken<List<Metadata.ReadResponse.File>>(){}.getType(), new JsonConverters.ListConverter<List<Metadata.ReadResponse.File>>())
            .registerTypeAdapter(new TypeToken<List<Tasks.GetResponse.Value_.HistoryEntry>>(){}.getType(), new JsonConverters.ListConverter<List<Tasks.GetResponse.Value_.HistoryEntry>>())
            .registerTypeAdapter(new TypeToken<Map<String, Views.Summary>>(){}.getType(), new JsonConverters.MapConverter<Views.Summary>())
            .registerTypeAdapter(new TypeToken<Map<String, Relationships.SimpleList>>(){}.getType(), new JsonConverters.MapConverter<Views.Summary>())
            .create();

    final Logger logger = Logger.getLogger(Client.class.getName());

    public Client() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        changes = new Changes(this);
        item = new Item(this);
        metadata = new Metadata(this);
        relationships = new Relationships(this);
        reviews = new Reviews(this);
        search = new Search(this);
        tasks = new Tasks(this);
        views = new Views(this);
        wayback = new Wayback(this);
    }

    private HttpClient httpClient;

    private void initHttpClient(HttpRequest.Builder builder) {
        if (!readOnly && !dryRun) {
            builder.header("authorization", String.format("LOW %s:%s", accessKey, secretKey));
        }

        // TODO
//        String version = Assembly.GetExecutingAssembly().<AssemblyInformationalVersionAttribute>GetCustomAttribute().InformationalVersion;
//        if (version == null) throw new IllegalStateException("Unable to get version");
//
//        ProductInfoHeaderValue productValue = new ProductInfoHeaderValue(name, version);
//        ProductInfoHeaderValue commentValue = new ProductInfoHeaderValue("(+https://github.com/experimentaltvcenter/InternetArchive.NET)");

//        builder.header("UserAgent", productValue);
//        builder.header("UserAgent", commentValue);
        builder.header("Accept-Encoding", "deflate,gzip");
        builder.header("ExpectContinue", "true");
    }

    public boolean readOnly;
    public boolean dryRun;

    String accessKey = null;
    transient String secretKey = null;

    public Changes changes;
    public Item item;
    public Metadata metadata;
    public Relationships relationships;
    public Reviews reviews;
    public Search search;
    public Tasks tasks;
    public Views views;
    public Wayback wayback;

    Set<Consumer<HttpRequest.Builder>> decorators = new HashSet<>(); // TODO hash not works

    public static Client createReadOnly(boolean dryRun /*= false*/) {
        Client client = getClient(/*readOnly:*/true, dryRun);
        client.decorators.add(client::initHttpClient);
        return client;
    }

    public static Client createByKey(String accessKey, String secretKey, boolean readOnly /*= false*/, boolean dryRun /*= false*/) {
        Client client = getClient(readOnly, dryRun);

        client.accessKey = accessKey;
        client.secretKey = secretKey;

        client.decorators.add(client::initHttpClient);
        return client;
    }

    public static Client create(String emailAddress /*= null*/, String password /*= null*/, boolean readOnly /*= false*/, boolean dryRun /*= false*/) throws IOException, InterruptedException {
        Client client = getClient(readOnly, dryRun);

        if (emailAddress == null || password == null) {
            StringBuilder loginPrompt = new StringBuilder("Log in to archive.org");

            List<String> restrictions = new ArrayList<>();
            if (readOnly) restrictions.add("readOnly");
            if (dryRun) restrictions.add("dryRun");
            if (restrictions.size() > 0)
                loginPrompt.append(" [").append(java.lang.String.join(", ", restrictions)).append("]");

            System.out.println(loginPrompt);
            System.out.println();

            if (!readOnly) {
                String message = "email address";
                if (emailAddress == null || emailAddress.isEmpty()) {
                    System.out.printf("%s: ", message);
                    emailAddress = new BufferedReader(new InputStreamReader(System.in)).readLine();
                } else {
                    System.out.printf("%s: %s", message, emailAddress);
                }

                if (emailAddress == null || emailAddress.isEmpty()) {
                    throw new IllegalArgumentException("email address required");
                }

                password = readPasswordFromConsole("Password: ");
            }
        }

        if (!readOnly) {
            client.login(emailAddress, password, readOnly);
        }

        client.decorators.add(client::initHttpClient);
        return client;
    }

    private static Client getClient(boolean readOnly, boolean dryRun) {
        Client client = new Client();

        client.readOnly = readOnly;
        client.dryRun = dryRun;

        return client;
    }

    public void requestInteractivePriority(HttpRequest.Builder builder) {
        builder.header("x-archive-interactive-priority", "1");
    }

    <Response> Response get(String url, Map<String, String> query /*= null*/, Class<Response> c) throws IOException, InterruptedException {
        return get(url, query, c, null);
    }

    <Response> Response get(String url, Map<String, String> query /*= null*/, Class<Response> c, Type t) throws IOException, InterruptedException {
        if (query != null) url = QueryHelpers.addQueryString(url, query);
Debug.println("url: " + url);

        var httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url));

        var response = this.send(httpRequest, c, t);
        if (response == null) throw new IllegalArgumentException("null response from server");
        return response;
    }

    static final String[] readOnlyMethods = new String[] {
            "HEAD", "GET"
    };

    static class HttpException extends IOException {
        int statusCode;
        HttpException(String msg, int statusCode) {
            super(msg);
            this.statusCode = statusCode;
        }
        @Override public String toString() {
            return "HttpException: " + statusCode + ": " + getMessage();
        }
    }

    <Response> Response send(HttpRequest.Builder requestBuilder, Class<Response> c) throws IOException, InterruptedException {
        return send(requestBuilder, c, null);
    }

    @SuppressWarnings("unchecked")
    <Response> Response send(HttpRequest.Builder requestBuilder, Class<Response> c, Type t) throws IOException, InterruptedException {
        decorators.forEach(d -> d.accept(requestBuilder));
        var request = requestBuilder.build();

        log(request);
        if (request.uri().getScheme().equals("http") && !readOnly)
            throw new IllegalArgumentException("Insecure call");

        if (readOnly && !Arrays.asList(readOnlyMethods).contains(request.method())) {
            if (dryRun) {
                logger.fine("dry run");
                return null;
            } else {
                throw new UnsupportedOperationException("Cannot call this function when the client is configured in read-only mode");
            }
        }

        @SuppressWarnings("rawtypes")
        HttpResponse.BodyHandler handler = HttpResponse.BodyHandlers.discarding();
        if (c != HttpResponse.class) {
            handler = HttpResponse.BodyHandlers.ofString();
        }

        HttpResponse<Response> httpResponse = httpClient.send(request, handler);
        logger.fine(httpResponse.toString());

        if (httpResponse.statusCode() != 200) {
            throw new HttpException(request.uri().toString(), httpResponse.statusCode());
        }

        if (c == HttpResponse.class) {
            return (Response) httpResponse;
        }

        String responseString = (String) httpResponse.body();
//Debug.println(Level.FINER, responseString.substring(0, Math.min(responseString.length(), 512)));
Debug.println(Level.FINER, responseString);

        if (c == String.class) {
            return (Response) responseString;
        }

        var contentType = httpResponse.headers().firstValue("Content-Type");
        if (contentType.isPresent()) {
Debug.println(Level.FINE, "contentType: " + contentType.get());
            if (contentType.get().contains("application/xml")) {
                try {
                    XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(responseString));
                    return t != null ? jaxson.readValue(xmlReader, (JavaType) t) : jaxson.readValue(xmlReader, c);
                } catch (XMLStreamException e) {
                    throw new IOException(e);
                }
            } else if (contentType.get().contains("application/json")) {
                return Client.gson.fromJson(responseString, t != null ? t : c);
            } else if (contentType.get().contains("text/html")) {
Debug.println(Level.FINE, httpResponse.uri());
                if (httpResponse.uri().toString().contains("https://archive.org/about/404.html")) {
                    throw new HttpException(request.uri().toString(), 404);
                }
            }
            throw new IllegalStateException("unsupported content type: " + contentType.get());
        }
        throw new IllegalStateException("no content type header");
    }

    /** body is json that is deserialized {@code content} */
    <Response> Response send(String httpMethod, String url, Object content, Class<Response> c) throws IOException, InterruptedException {
        var json = gson.toJson(content);
Debug.println(Level.FINE, content.getClass().getSimpleName() + ": " + json);
        var StringContent = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);

        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method(httpMethod, StringContent);


        return this.send(httpRequest, c, null);
    }

    <Response> Response send(String httpMethod, String url, String mime, HttpRequest.BodyPublisher content, Class<Response> c) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", mime)
                .method(httpMethod, content);

        return this.send(httpRequest, c, null);
    }

    private void log(HttpRequest request) {
        if (!logger.isLoggable(Level.FINE)) return;

        logger.fine(String.format("%s %s", request.method(), request.uri()));

        if (request.headers() != null) {
            logger.fine("Request headers:");
            for (var kvp : request.headers().map().entrySet()) {
                for (var value : kvp.getValue()) {
                    System.err.printf("%s: %s%n", kvp.getKey(), value);
                }
            }
        }
    }

    private HttpResponse<?> log(HttpResponse<?> response) {
        if (logger.isLoggable(Level.FINE)) {
            if (response.headers() != null) {
                logger.fine("response headers:");
                for (var kvp : response.headers().map().entrySet()) {
                    for (var value : kvp.getValue()) {
                        System.err.printf("%s: %s%n", kvp.getKey(), value);
                    }
                }
            }

            String body = response.body().toString();
            logger.log(Level.FINE, "response body: {}", body);
            logger.log(response.statusCode() / 200 == 1 ? Level.FINE : Level.SEVERE, String.format("result: %d %s", response.statusCode(), response.body()));
        }

        return response;
    }

    static class LoginResponse extends ServerResponse {

        public int version;

        public Values_ values;

        static class Values_ {

            public String reason;

            static class Cookies_ {

                @SerializedName("logged-in-sig")
                public String loggedInSig;

                @SerializedName("logged-in-user")
                public String loggedInUser;
            }

            public Cookies_ cookies;

            public String email;
            public String itemName;

            static class S3_ {

                @SerializedName("access")
                public String accessKey = null;

                @SerializedName("secret")
                public String secretKey = null;
            }

            public S3_ s3;

            public String screenName;
        }
    }

    private void login(String emailAddress, String password, boolean readOnly) throws IOException, InterruptedException {
        String url = "https://archive.org/services/xauthn/?op=login";

        httpClient = java.net.http.HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(emailAddress, password.toCharArray());
                    }
                }).build();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(url))
                .build();

        logger.fine("Logging in...");
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log(httpResponse);

        if (httpResponse == null) throw new NullPointerException("httpResponse");

        LoginResponse loginResponse = gson.fromJson(httpResponse.body(), LoginResponse.class);

        if (httpResponse.statusCode() / 200 == 1) {
            if (loginResponse == null || loginResponse.values == null || loginResponse.values.s3 == null)
                throw new NullPointerException("loginResponse");
            loginResponse.ensureSuccess();

            if (!readOnly) {
                accessKey = loginResponse.values.s3.accessKey;
                secretKey = loginResponse.values.s3.secretKey;
            }
        } else if (httpResponse.statusCode() == 401 /*Unauthorized*/) {
            throw new IOException("Login failed: " + loginResponse.values.reason);
        } else {
            throw new IOException(String.valueOf(httpResponse.statusCode()));
        }
    }

    private static String readPasswordFromConsole(String prompt) throws IOException {
        System.out.print(prompt);

        StringBuilder password = new StringBuilder();

        while (true) {
            var keyInfo = System.in.read();

            if (keyInfo == 0x0a) {
                System.out.println();
                break;
            } else if (keyInfo == 0x08 && password.length() > 0) {
                password = new StringBuilder(password.substring(0, password.length() - 1));
            } else if (!Character.isISOControl(keyInfo)) {
                password.append((char) keyInfo);
            }
        }

        return password.toString();
    }
}
