package vavi.net.ia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import vavi.net.ia.dotnet.KeyValuePair;
import vavi.util.ByteUtil;


public class Item {

    private final String Url = "https://s3.us.archive.org";

    private Client _client;

    public Item(Client client) {
        _client = client;
    }

    public static class PutRequest {

        public String Bucket;

        public Path LocalPath;

        public SeekableByteChannel SourceStream;

        public String RemoteFilename;

        public List<KeyValuePair<String, Object>> Metadata = new ArrayList<>();

        public boolean CreateBucket;

        public boolean NoDerive;

        public boolean KeepOldVersion;

        public boolean DeleteExistingMetadata;

        public long MultipartUploadMinimumSize = 1024 * 1024 * 300; // use multipart for files over 300 MB
        public int MultipartUploadChunkSize = 1024 * 1024 * 200; // upload in 200 MB chunks
        public int MultipartUploadThreadCount = 3; // three simultaneous uploads
        List<Integer> MultipartUploadSkipParts = new ArrayList<>(); // for testing

        public String SimulateError;

        boolean HasFilename() {
            return RemoteFilename != null || LocalPath != null;
        }

        String Filename() {
            return Filename(true);
        }

        String Filename(boolean encoded) {
            if (RemoteFilename != null) {
                return RemoteFilename;
            } else {
                Path filename = LocalPath.getFileName();
                if (filename != null)
                    return encoded ? Encode(filename.toString()) : filename.toString();
                else
                    throw new IllegalStateException("RemoteFilename or LocalPath required");
            }
        }
    }

    private static String Encode(String s) {
        // UrlEncode replaces spaces so we use this instead:
        return s.replace(";", "%3b").replace("#", "%23");
    }

    public HttpResponse<?> PutAsync(PutRequest request) throws IOException, InterruptedException {
        return PutAsync(request, 0);
    }

    public HttpResponse<?> PutAsync(PutRequest request, int timeout) throws IOException, InterruptedException {
        if (request.Bucket == null) throw new IllegalArgumentException("A Bucket identifier required");

        SeekableByteChannel sourceStream = null;

        try {
            HttpRequest.Builder uploadRequest = HttpRequest.newBuilder();
            boolean isMultipartUpload = false;

            Path fileInfo = null;
            HttpRequest.BodyPublisher content = null;

            if (request.HasFilename()) {
                if (request.SourceStream == null && request.LocalPath == null)
                    throw new IllegalArgumentException("A SourceStream or LocalPath is required");

                sourceStream = request.SourceStream == null ? Files.newByteChannel(request.LocalPath) : request.SourceStream;
                if (sourceStream.size() >= request.MultipartUploadMinimumSize) isMultipartUpload = true;

                uploadRequest.uri(URI.create(String.format("%s/%s/%s%s", Url, request.Bucket, request.Filename(), isMultipartUpload ? "?uploads" : "")));
                uploadRequest.header("x-archive-size-hint", String.valueOf(Files.size(fileInfo)));

                if (!isMultipartUpload) {
                    SeekableByteChannel sbc = sourceStream;
                    content = HttpRequest.BodyPublishers.ofInputStream(() -> Channels.newInputStream(sbc));

                    try {
                        var md5 = MessageDigest.getInstance("MD5");
                        md5.update(Files.readAllBytes(fileInfo));
                        uploadRequest.header("ContentMD5", ByteUtil.toHexString(md5.digest()));
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                uploadRequest.uri(URI.create(Url + "/" + request.Bucket));
            }

            AddMetadata(uploadRequest, request.Metadata);

            if (request.CreateBucket) uploadRequest.header("x-archive-auto-make-bucket", "1");
            if (request.KeepOldVersion) uploadRequest.header("x-archive-keep-old-version", "1");
            if (request.NoDerive) uploadRequest.header("x-archive-queue-derive", "0");
            if (request.DeleteExistingMetadata)
                uploadRequest.header("x-archive-ignore-preexisting-bucket", "1");
            if (request.SimulateError != null)
                uploadRequest.header("x-archive-simulate-error", request.SimulateError);

            if (!isMultipartUpload) {
                uploadRequest.method("PUT", content);
                return _client.SendAsync(uploadRequest, HttpResponse.class);
            } else {
                return MultipartUpload(request, sourceStream, uploadRequest);
            }
        } finally {
            if (sourceStream != null) sourceStream.close();
        }
    }

    static void AddMetadata(HttpRequest.Builder httpRequest, List<KeyValuePair<String, Object>> metadata) throws IOException {
        for (var group : metadata.stream().collect(Collectors.groupingBy(KeyValuePair::getKey)).entrySet()) {
            int count = 0;
            for (var kv : group.getValue()) {
                String val = kv.getValue().toString();
                if (val == null) throw new NullPointerException();
                if (val.chars().anyMatch(x -> x > 127)) {
                    val = String.format("uri(%s)", URLEncoder.encode(val, StandardCharsets.UTF_8));
                }

                httpRequest.header(String.format("x-archive-meta%d-%s", count++, kv.getKey().replace("_", "--")), val);
            }
        }
    }

    private List<XmlModels.Upload> GetUploadsInProgressAsync(String bucket) throws IOException, InterruptedException {
        PutRequest request1 = new PutRequest();
        request1.Bucket = bucket;
        return GetUploadsInProgressAsync(request1);
    }

    private List<XmlModels.Upload> GetUploadsInProgressAsync(PutRequest request) throws IOException, InterruptedException {
        var listMultipartUploadsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(String.format("%s/%s/?uploads", Url, request.Bucket)));

        var listMultipartUploadsResult = _client.SendAsync(listMultipartUploadsRequest, XmlModels.ListMultipartUploadsResult.class);
        if (request.HasFilename()) {
            return listMultipartUploadsResult.Uploads != null ? listMultipartUploadsResult.Uploads : Collections.emptyList();
        } else {
            return listMultipartUploadsResult.Uploads.stream().filter(x -> x.Key.equals(request.Filename())).collect(Collectors.toList());
        }
    }

    public void AbortUploadAsync(String bucket) throws ExecutionException, InterruptedException, IOException {
        var uploads = GetUploadsInProgressAsync(bucket);
        for (var upload : uploads) {
            var abortMultipartUploadRequest = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", Url, bucket, upload.Key, upload.UploadId)));

            _client.SendAsync(abortMultipartUploadRequest, HttpResponse.class);
        }
    }

    public void AbortUploadAsync(PutRequest request) throws ExecutionException, InterruptedException, IOException {
        var uploads = GetUploadsInProgressAsync(request);
        for (var upload : uploads) {
            var abortMultipartUploadRequest = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", Url, request.Bucket, request.Filename(), upload.UploadId)));

            _client.SendAsync(abortMultipartUploadRequest, HttpResponse.class);
        }
    }

    static class XmlModels {

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class ListMultipartUploadsResult {

            public String Bucket = null;
            public String KeyMarker = null;
            public String UploadIdMarker = null;
            public String NextKeyMarker = null;
            public String NextUploadIdMarker = null;
            public int MaxUploads;

            public boolean IsTruncated;

            @JacksonXmlProperty(localName = "Upload")
            public List<Upload> Uploads = null;
        }

        static class Upload {

            public String Key = null;
            public String UploadId = null;
        }

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class InitiateMultipartUploadResult {

            public String Bucket = null;
            public String Key = null;
            public String UploadId = null;
        }

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class ListPartsResult {

            public String Bucket = null;
            public String Key = null;
            public String UploadId = null;
            public String PartNumberMarker = null;
            public String NextPartNumberMarker = null;
            public int MaxParts;
            public boolean IsTruncated;

            @JacksonXmlProperty(localName = "Part")
            public List<Part> Parts = null;
        }

        @JacksonXmlRootElement
        static class Part {

            //@DataMember
            public int PartNumber;
            //@DataMember
            public String ETag = null;
        }

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class CompleteMultipartUpload {
            @JacksonXmlProperty(localName = "Part")
            public List<Part> Parts = null;
        }
    }

    private HttpResponse<?> MultipartUpload(PutRequest request, SeekableByteChannel
            sourceStream, HttpRequest.Builder uploadRequest) throws IOException, InterruptedException {
        CopyOnWriteArrayList<XmlModels.Part> parts = new CopyOnWriteArrayList<>();
        String uploadId = null;

        try {
            // see if there's already a multipart upload in progress

            var uploads = GetUploadsInProgressAsync(request);
            if (uploads.size() > 0) {
                uploadId = uploads.stream().findFirst().get().UploadId;

                var listPartsRequest = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", Url, request.Bucket, request.Filename(), uploadId)));

                var listPartsResult = _client.SendAsync(listPartsRequest, XmlModels.ListPartsResult.class);
                parts.addAll(listPartsResult.Parts);
            }
        } catch (Client.HttpException ex) {
            if (ex.statusCode != 404) throw ex;
        }

        if (uploadId == null) {
            uploadRequest.POST(HttpRequest.BodyPublishers.noBody()); // TODO

            var initiateMultipartUploadResult = _client.SendAsync(uploadRequest, XmlModels.InitiateMultipartUploadResult.class);
            if (initiateMultipartUploadResult == null) return null; // dry run

            uploadId = initiateMultipartUploadResult.UploadId;
        }

        var totalParts = sourceStream.size() / request.MultipartUploadChunkSize;
        if (sourceStream.size() % request.MultipartUploadChunkSize != 0) totalParts++;

        var tasks = new ArrayList<Future<?>>();

        ExecutorService es = Executors.newFixedThreadPool((int) totalParts);
        for (var i = new AtomicInteger(1); i.get() <= totalParts; i.addAndGet(2)) {
            if (request.MultipartUploadSkipParts.contains(i.get()) || parts.stream().anyMatch(x -> x.PartNumber == i.get()))
                continue;

            String uploadId_ = uploadId;
            tasks.add(es.submit(() -> {
                try {
                    var buffer = ByteBuffer.allocate(request.MultipartUploadChunkSize);
                    int length;

                    synchronized (sourceStream) {
                        sourceStream.position((i.get() - 1L) * request.MultipartUploadChunkSize);
                        length = sourceStream.read(buffer);
                    }

                    var partRequest = HttpRequest.newBuilder()
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(buffer.array()))
                            .uri(URI.create(String.format("%s/%s/%s?partNumber=%s&uploadId=%s", Url, request.Bucket, request.Filename(), i.get(), uploadId_)));

                    var md5 = MessageDigest.getInstance("MD5");
                    md5.update(buffer.array(), 0, length);
                    partRequest.header("ContentMD5", ByteUtil.toHexString(md5.digest()));

                    var partResponse = _client.SendAsync(partRequest, HttpResponse.class);
                    if (partResponse.headers().firstValue("ETag").isEmpty())
                        throw new Exception("Invalid multipart upload response for part {partNumber}");

                    var part = new XmlModels.Part();
                    part.PartNumber = i.get();
                    part.ETag = partResponse.headers().firstValue("ETag").get();
                    parts.add(part);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        es.shutdown();
        if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
            es.shutdownNow();
        }

        if (parts.size() != totalParts) return null;

        XmlMapper serializer = XmlMapper.xmlBuilder().build();
        var ms = new ByteArrayOutputStream();
        XmlModels.CompleteMultipartUpload cmu = new XmlModels.CompleteMultipartUpload();
        parts.sort(Comparator.comparingInt(x -> x.PartNumber));
        cmu.Parts = parts;
        serializer.writeValue(ms, cmu);
        String xml = ms.toString();
        var httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
                .header("Content-Type", "application/xml")
                .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", Url, request.Bucket, request.Filename(), uploadId)));
        return _client.SendAsync(httpRequest, HttpResponse.class);
    }

    static class DeleteRequest {

        public String Bucket;

        public String RemoteFilename;

        public boolean KeepOldVersion;

        public boolean CascadeDelete;
    }

    public HttpResponse<Void> DeleteAsync(DeleteRequest request) throws IOException, InterruptedException {
        if (request.Bucket == null) throw new IllegalArgumentException("identifier required");
        if (request.Bucket.contains("/"))
            throw new IllegalArgumentException("slash not allowed in bucket name; use .RemoteFilename to specify the file to delete in a bucket");

        String requestUri = "{Url}/{request.Bucket}";
        if (request.RemoteFilename != null) requestUri += "/{Encode(request.RemoteFilename)}";

        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .DELETE();
        if (request.KeepOldVersion) httpRequest.header("x-archive-keep-old-version", "1");
        if (request.CascadeDelete) httpRequest.header("x-archive-cascade-delete", "1");

        return _client.SendAsync(httpRequest, HttpResponse.class);
    }

    static class UseLimitResponse {

        public String Bucket;

        public String AccessKey;

        @JacksonXmlProperty(localName = "over_limit")
        public int OverLimit;

        static class Detail_ {

            @JacksonXmlProperty(localName = "accesskey_ration")
            public long AccessKeyRation;

            @JacksonXmlProperty(localName = "accesskey_tasks_queued")
            public long AccessKeyTasksQueued;

            @JacksonXmlProperty(localName = "bucket_ration")
            public long BucketRation;

            @JacksonXmlProperty(localName = "bucket_tasks_queued")
            public long BucketTasksQueued;

            @JacksonXmlProperty(localName = "limit_reason")
            public String LimitReason;

            @JacksonXmlProperty(localName = "rationing_engaged")
            public long RationingEngaged;

            @JacksonXmlProperty(localName = "rationing_level")
            public long RationingLevel;

            @JacksonXmlProperty(localName = "total_global_limit")
            public long TotalGlobalLimit;

            @JacksonXmlProperty(localName = "total_tasks_queued")
            public long TotalTasksQueued;
        }

        public Detail_ Detail;
    }

    public UseLimitResponse GetUseLimitAsync(String bucket/* = ""*/) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        query.put("check_limit", "1");
        query.put("accesskey", _client.AccessKey);
        query.put("bucket", bucket);

        return _client.GetAsync(Url, query, UseLimitResponse.class);
    }
}
