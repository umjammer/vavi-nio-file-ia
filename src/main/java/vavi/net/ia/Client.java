package vavi.net.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import vavi.net.ia.dotnet.QueryHelpers;


public class Client {

    public final String Name = "InternetArchive.NET";
    static Gson _json = new Gson().newBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    final Logger _logger = Logger.getLogger(Client.class.getName());

    public Client() {
        if (this.httpClient == null) {
            this.httpClient = HttpClient.newHttpClient();
            //throw new IOException("Must pass an httpClient or an HttpClientFactory");
        }

        Changes = new Changes(this);
        Item = new Item(this);
        Metadata = new Metadata(this);
        Relationships = new Relationships(this);
        Reviews = new Reviews(this);
        Search = new Search(this);
        Tasks = new Tasks(this);
        Views = new Views(this);
        Wayback = new Wayback(this);
    }

    private HttpClient httpClient;

    private void InitHttpClient(HttpRequest.Builder builder) {
        if (!ReadOnly && !DryRun) {
            builder.setHeader("authorization", String.format("LOW %s:%s", AccessKey, SecretKey));
        }

        // TODO
//        String version = Assembly.GetExecutingAssembly().<AssemblyInformationalVersionAttribute>GetCustomAttribute().InformationalVersion;
//        if (version == null) throw new IllegalStateException("Unable to get version");
//
//        ProductInfoHeaderValue productValue = new ProductInfoHeaderValue(Name, version);
//        ProductInfoHeaderValue commentValue = new ProductInfoHeaderValue("(+https://github.com/experimentaltvcenter/InternetArchive.NET)");

//        builder.setHeader("UserAgent", productValue);
//        builder.setHeader("UserAgent", commentValue);
        builder.setHeader("ExpectContinue", "true");
    }

    public boolean ReadOnly;
    public boolean DryRun;

    String AccessKey = null;
    transient String SecretKey = null;

    public Changes Changes;
    public Item Item;
    public Metadata Metadata;
    public Relationships Relationships;
    public Reviews Reviews;
    public Search Search;
    public Tasks Tasks;
    public Views Views;
    public Wayback Wayback;

    static Set<Consumer<HttpRequest.Builder>> decorators = new HashSet<>();

    public static Client CreateReadOnly(boolean dryRun /*= false*/) {
        Client client = GetClient(/*readOnly:*/true, dryRun);
        decorators.add(client::InitHttpClient);
        return client;
    }

    public static Client Create(String accessKey, String secretKey, boolean readOnly /*= false*/, boolean dryRun /*= false*/) {
        Client client = GetClient(readOnly, dryRun);

        client.AccessKey = accessKey;
        client.SecretKey = secretKey;

        decorators.add(client::InitHttpClient);
        return client;
    }

    public static Client CreateAsync(String emailAddress /*= null*/, String password /*= null*/, boolean readOnly /*= false*/, boolean dryRun /*= false*/) throws IOException, InterruptedException {
        Client client = GetClient(readOnly, dryRun);

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
                String message = "Email address";
                if (emailAddress == null || emailAddress.isEmpty()) {
                    System.out.printf("%s: ", message);
                    emailAddress = new BufferedReader(new InputStreamReader(System.in)).readLine();
                } else {
                    System.out.printf("%s: %s", message, emailAddress);
                }

                if (emailAddress == null || emailAddress.isEmpty()) {
                    throw new IllegalArgumentException("Email address required");
                }

                password = ReadPasswordFromConsole("Password: ");
            }
        }

        if (!readOnly) {
            client.LoginAsync(emailAddress, password, readOnly);
        }

        decorators.add(client::InitHttpClient);
        return client;
    }

    private static Client GetClient(boolean readOnly, boolean dryRun) {
        Client client = new Client();

        client.ReadOnly = readOnly;
        client.DryRun = dryRun;

        return client;
    }

    public void RequestInteractivePriority(HttpRequest.Builder builder) {
        builder.header("x-archive-interactive-priority", "1");
    }

    <Response> Response GetAsync(String url, Map<String, String> query /*= null*/, Class<Response> c) throws IOException, InterruptedException {
        if (query != null) url = QueryHelpers.AddQueryString(url, query);

        var httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url));

        var response = this.SendAsync(httpRequest, c);
        if (response == null) throw new IllegalArgumentException("null response from server");
        return response;
    }

    static final String[] _readOnlyMethods = new String[] {
            "HEAD", "GET"
    };

    static class HttpException extends IOException {
        int statusCode;
        HttpException(String msg, int statusCode) {
            super(msg);
            this.statusCode = statusCode;
        }
    }

    <Response> Response SendAsync(HttpRequest.Builder requestBuilder, Class<Response> c) throws IOException, InterruptedException {
        decorators.forEach(d -> d.accept(requestBuilder));
        var request = requestBuilder.build();

        log(request);
        if (request.uri().getScheme().equals("http") && !ReadOnly)
            throw new IllegalArgumentException("Insecure call");

        if (ReadOnly && !Arrays.asList(_readOnlyMethods).contains(request.method())) {
            if (DryRun) {
                _logger.info("dry run");
                return null;
            } else {
                throw new UnsupportedOperationException("Cannot call this function when the client is configured in read-only mode");
            }
        }

        HttpResponse.BodyHandler handler = HttpResponse.BodyHandlers.discarding();
        if (c != HttpResponse.class) {
            handler = HttpResponse.BodyHandlers.ofString();
        }

        HttpResponse<Response> httpResponse = httpClient.send(request, handler);
        _logger.fine(httpResponse.toString());

        if (httpResponse.statusCode() != 200) {
            throw new HttpException(request.uri().toString(), httpResponse.statusCode());
        }

        if (c == HttpResponse.class) {
            return (Response) httpResponse;
        }

        String responseString = (String) httpResponse.body();

        if (c == String.class) {
            return (Response) responseString;
        }

        if (httpResponse.headers().firstValue("MediaType").get().equals("application/xml")) {
            try {
                var serializer = new XmlMapper();
                XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(responseString));
                return serializer.readValue(xmlReader, c);
            } catch (XMLStreamException e) {
                throw new IOException(e);
            }
        } else {
            return Client._json.fromJson(responseString, c);
        }
    }

    <Response> Response SendAsync(String httpMethod, String url, Object content, Class<Response> c) throws IOException, InterruptedException {
        var json = _json.toJson(content);
        var StringContent = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);

        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method(httpMethod, StringContent);


        return this.SendAsync(httpRequest, c);
    }

    <Response> Response SendAsync(String httpMethod, String url, String mime, HttpRequest.BodyPublisher content, Class<Response> c) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", mime)
                .method(httpMethod, content);

        return this.SendAsync(httpRequest, c);
    }

    private void log(HttpRequest request) {
        Level logLevel = Level.INFO;
        if (!_logger.isLoggable(logLevel)) return;

        _logger.log(logLevel, String.format("%s %s", request.method(), request.uri()));

        if (request.headers() != null) {
            _logger.log(logLevel, "Request headers:");
            for (var kvp : request.headers().map().entrySet()) {
                for (var value : kvp.getValue()) {
                    _logger.log(logLevel, String.format("%s: %s", kvp.getKey(), value));
                }
            }
        }
    }

    private HttpResponse<?> log(HttpResponse<?> response) {
        Level logLevel = Level.INFO;
        if (_logger.isLoggable(logLevel)) {
            if (response.headers() != null) {
                _logger.log(logLevel, "Response headers:");
                for (var kvp : response.headers().map().entrySet()) {
                    for (var value : kvp.getValue()) {
                        _logger.log(logLevel, String.format("%s: %s", kvp.getKey(), value));
                    }
                }
            }

            String body = response.body().toString();
            _logger.log(logLevel, "Response body: {body}", body);
            _logger.log(response.statusCode() / 200 == 1 ? logLevel : Level.SEVERE, String.format("Result: %d %s", (int) response.statusCode(), response.body()));
        }

        return response;
    }

    static class LoginResponse extends ServerResponse {

        public int Version;

        public Values_ Values;

        static class Values_ {

            public String Reason;

            static class Cookies_ {

                @JacksonXmlProperty(localName = "logged-in-sig")
                public String LoggedInSig;

                @JacksonXmlProperty(localName = "logged-in-user")
                public String LoggedInUser;
            }

            public Cookies_ Cookies;

            public String Email;
            public String ItemName;

            static class S3_ {

                @JacksonXmlProperty(localName = "access")
                public String AccessKey = null;

                @JacksonXmlProperty(localName = "secret")
                public String SecretKey = null;
            }

            public S3_ S3;

            public String ScreenName;
        }
    }

    private void LoginAsync(String emailAddress, String password, boolean readOnly) throws IOException, InterruptedException {
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

        _logger.info("Logging in...");
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log(httpResponse);

        if (httpResponse == null) throw new NullPointerException("httpResponse");

        LoginResponse loginResponse = _json.fromJson(httpResponse.body(), LoginResponse.class);

        if (httpResponse.statusCode() / 200 == 1) {
            if (loginResponse == null || loginResponse.Values == null || loginResponse.Values.S3 == null)
                throw new NullPointerException("loginResponse");
            loginResponse.EnsureSuccess();

            if (!readOnly) {
                AccessKey = loginResponse.Values.S3.AccessKey;
                SecretKey = loginResponse.Values.S3.SecretKey;
            }
        } else if (httpResponse.statusCode() == 401 /*Unauthorized*/) {
            throw new IOException("Login failed: " + loginResponse.Values.Reason);
        } else {
            throw new IOException(String.valueOf(httpResponse.statusCode()));
        }
    }

    private static String ReadPasswordFromConsole(String prompt) throws IOException {
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
