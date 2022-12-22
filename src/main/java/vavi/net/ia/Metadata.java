package vavi.net.ia;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import vavi.net.ia.dotnet.KeyValuePair;


public class Metadata {

    static String Url(String identifier) {
        return "https://archive.org/metadata/" + identifier;
    }

    private final Client _client;

    public Metadata(Client client) {
        _client = client;
    }

    public static class ReadResponse implements Closeable {

        @JacksonXmlProperty(localName = "created")
        @JsonAdapter(UnixEpochDateTimeNullableDeserializer.class)
        public LocalDateTime DateCreated;

        @JacksonXmlProperty(localName = "d1")
        public String DataNodePrimary = null;

        @JacksonXmlProperty(localName = "d2")
        public String DataNodeSecondary;

        @JacksonXmlProperty(localName = "solo")
        public Boolean DataNodeSolo;

        @JacksonXmlProperty(localName = "is_dark")
        public boolean IsDark;

        public String Dir = null;

        public List<File> Files;

        public static class File {

            public String Name;
            public String Source;
            public String Original;

            @JacksonXmlProperty(localName = "mtime")
            @JsonAdapter(UnixEpochDateTimeNullableDeserializer.class)
            public OffsetDateTime ModificationDate;

            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public Long Size;

            public String Md5;
            public String Crc32;
            public String Sha1;

            public String Btih;
            public String Summation;
            public String Format;

            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public float Length;

            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public int Width;

            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public int Height;

            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public int Rotation;

            @JacksonXmlProperty(localName = "viruscheck")
            @JsonAdapter(UnixEpochDateTimeNullableDeserializer.class)
            public LocalDateTime VirusCheckDate;
        }

        public JsonObject Metadata;

        @JacksonXmlProperty(localName = "item_last_updated")
        @JsonAdapter(UnixEpochDateTimeNullableDeserializer.class)
        public LocalDateTime DateLastUpdated;

        @JacksonXmlProperty(localName = "item_size")
        public Long Size;

        public Long Uniq;

        @JacksonXmlProperty(localName = "servers_unavailable")
        public boolean ServersUnavailable;

        @JacksonXmlProperty(localName = "workable_servers")
        @JsonAdapter(EnumerableStringDeserializer.class)
        public String[] WorkableServers;

        public void close() {
//            Metadata.close();
            Metadata = null;
        }
    }

    public ReadResponse ReadAsync(String identifier) throws IOException, InterruptedException {
        return _client.GetAsync(identifier, null, ReadResponse.class);
    }

    public static class WriteResponse extends ServerResponse {

        @JacksonXmlProperty(localName = "task_id")
        public Long TaskId;

        public String Log;
        public String Error;
    }

    WriteResponse WriteAsync(String url, String target, String json) throws IOException, InterruptedException {
        List<KeyValuePair<String, String>> formData = new ArrayList<>();
        formData.add(new KeyValuePair<>("-target", target));
        formData.add(new KeyValuePair<>("-patch", json));

        String form = formData.stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest.BodyPublisher httpContent = HttpRequest.BodyPublishers.ofString(form);
        WriteResponse writeMetadataResponse = _client.SendAsync("POST", url, httpContent, WriteResponse.class);

        writeMetadataResponse.EnsureSuccess();
        return writeMetadataResponse;
    }

    public WriteResponse WriteAsync(String identifier, String patch) throws IOException, InterruptedException {
        return WriteAsync(Url(identifier), "metadata", patch);
    }
}