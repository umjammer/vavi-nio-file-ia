package vavi.net.ia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.gson.annotations.SerializedName;
import vavi.net.ia.dotnet.KeyValuePair;
import vavi.util.ByteUtil;
import vavi.util.Debug;


public class Item {

    private static final String url = "https://s3.us.archive.org";

    private Client client;

    public Item(Client client) {
        this.client = client;
    }

    public static class PutRequest {

        public String bucket;

        public Path localPath;

        public SeekableByteChannel sourceStream;

        public String remoteFilename;

        public List<KeyValuePair<String, Object>> metadata = new ArrayList<>();

        public boolean createBucket;

        public boolean noDerive;

        public boolean keepOldVersion;

        public boolean deleteExistingMetadata;

        public long multipartUploadMinimumSize = 1024 * 1024 * 300; // use multipart for files over 300 MB
        public int multipartUploadChunkSize = 1024 * 1024 * 200; // upload in 200 MB chunks
        public int multipartUploadThreadCount = 3; // three simultaneous uploads
        List<Integer> multipartUploadSkipParts = new ArrayList<>(); // for testing

        public String simulateError;

        boolean hasFilename() {
            return remoteFilename != null || localPath != null;
        }

        String filename() {
            return filename(true);
        }

        String filename(boolean encoded) {
            if (remoteFilename != null) {
                return remoteFilename;
            } else {
                Path filename = localPath.getFileName();
                if (filename != null)
                    return encoded ? encode(filename.toString()) : filename.toString();
                else
                    throw new IllegalStateException("remoteFilename or localPath required");
            }
        }

        private static String encode(String s) {
            // UrlEncode replaces spaces so we use this instead:
            return s.replace(";", "%3b").replace("#", "%23");
        }
    }

    public HttpResponse<?> put(PutRequest request) throws IOException, InterruptedException {
        return put(request, 0);
    }

    public HttpResponse<?> put(PutRequest request, int timeout) throws IOException, InterruptedException {
        if (request.bucket == null) throw new IllegalArgumentException("A bucket identifier required");

        SeekableByteChannel sourceStream = null;

        try {
            HttpRequest.Builder uploadRequest = HttpRequest.newBuilder();
            boolean isMultipartUpload = false;

            HttpRequest.BodyPublisher content = null;

            if (request.hasFilename()) {
                if (request.sourceStream == null && request.localPath == null)
                    throw new IllegalArgumentException("A sourceStream or localPath is required");

                sourceStream = request.sourceStream == null ? Files.newByteChannel(request.localPath) : request.sourceStream;
                if (sourceStream.size() >= request.multipartUploadMinimumSize) isMultipartUpload = true;

                uploadRequest.uri(URI.create(String.format("%s/%s/%s%s", url, request.bucket, request.filename().replace(" ", "+"), isMultipartUpload ? "?uploads" : "")));
                uploadRequest.header("x-archive-size-hint", String.valueOf(sourceStream.size()));

                if (!isMultipartUpload) {
                    ByteBuffer bb = ByteBuffer.allocate((int) sourceStream.size());
                    sourceStream.read(bb);
Debug.println(Level.FINER, "sourceStream: " + sourceStream.size());
                    content = HttpRequest.BodyPublishers.ofByteArray(bb.array());

                    try {
                        var md5 = MessageDigest.getInstance("MD5");
                        md5.update(bb.array());
                        uploadRequest.header("ContentMD5", ByteUtil.toHexString(md5.digest()));
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                uploadRequest.uri(URI.create(url + "/" + request.bucket));
            }

            addMetadata(uploadRequest, request.metadata);

            if (request.createBucket) uploadRequest.header("x-archive-auto-make-bucket", "1");
            if (request.keepOldVersion) uploadRequest.header("x-archive-keep-old-version", "1");
            if (request.noDerive) uploadRequest.header("x-archive-queue-derive", "0");
            if (request.deleteExistingMetadata)
                uploadRequest.header("x-archive-ignore-preexisting-bucket", "1");
            if (request.simulateError != null)
                uploadRequest.header("x-archive-simulate-error", request.simulateError);

            if (!isMultipartUpload) {
                uploadRequest.PUT(content);
                return client.send(uploadRequest, HttpResponse.class);
            } else {
                return multipartUpload(request, sourceStream, uploadRequest);
            }
        } finally {
            if (sourceStream != null) sourceStream.close();
        }
    }

    static void addMetadata(HttpRequest.Builder httpRequest, List<KeyValuePair<String, Object>> metadata) throws IOException {
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

    private List<XmlModels.Upload> getUploadsInProgress(String bucket) throws IOException, InterruptedException {
        PutRequest request1 = new PutRequest();
        request1.bucket = bucket;
        return getUploadsInProgress(request1);
    }

    private List<XmlModels.Upload> getUploadsInProgress(PutRequest request) throws IOException, InterruptedException {
        var listMultipartUploadsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(String.format("%s/%s/?uploads", url, request.bucket)));

        var listMultipartUploadsResult = client.send(listMultipartUploadsRequest, XmlModels.ListMultipartUploadsResult.class);
        if (listMultipartUploadsResult.uploads != null) {
            if (request.hasFilename()) {
                return listMultipartUploadsResult.uploads;
            } else {
                return listMultipartUploadsResult.uploads.stream().filter(x -> x.key.equals(request.filename())).collect(Collectors.toList());
            }
        } else {
            return Collections.emptyList();
        }
    }

    public void abortUpload(String bucket) throws InterruptedException, IOException {
        var uploads = getUploadsInProgress(bucket);
        for (var upload : uploads) {
            var abortMultipartUploadRequest = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", url, bucket, upload.key, upload.uploadId)));

            client.send(abortMultipartUploadRequest, HttpResponse.class);
        }
    }

    public void abortUpload(PutRequest request) throws InterruptedException, IOException {
        var uploads = getUploadsInProgress(request);
        for (var upload : uploads) {
            var abortMultipartUploadRequest = HttpRequest.newBuilder()
                    .DELETE()
                    .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", url, request.bucket, request.filename(), upload.uploadId)));

            client.send(abortMultipartUploadRequest, HttpResponse.class);
        }
    }

    static class XmlModels {

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class ListMultipartUploadsResult {

            @JacksonXmlProperty(localName = "Bucket")
            public String bucket = null;
            @JacksonXmlProperty(localName = "KeyMarker")
            public String keyMarker = null;
            @JacksonXmlProperty(localName = "UploadIdMarker")
            public String uploadIdMarker = null;
            @JacksonXmlProperty(localName = "NextKeyMarker")
            public String nextKeyMarker = null;
            @JacksonXmlProperty(localName = "NextUploadIdMarker")
            public String nextUploadIdMarker = null;
            @JacksonXmlProperty(localName = "MaxUploads")
            public int maxUploads;

            @JacksonXmlProperty(localName = "IsTruncated")
            public boolean isTruncated;

            @JacksonXmlProperty(localName = "Upload")
            public List<Upload> uploads = null;
        }

        static class Upload {

            @JacksonXmlProperty(localName = "Key")
            public String key = null;
            @JacksonXmlProperty(localName = "UploadId")
            public String uploadId = null;
        }

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class InitiateMultipartUploadResult {

            @JacksonXmlProperty(localName = "Bucket")
            public String bucket = null;
            @JacksonXmlProperty(localName = "Key")
            public String key = null;
            @JacksonXmlProperty(localName = "UploadId")
            public String uploadId = null;
        }

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class ListPartsResult {

            @JacksonXmlProperty(localName = "Bucket")
            public String bucket = null;
            @JacksonXmlProperty(localName = "Key")
            public String key = null;
            @JacksonXmlProperty(localName = "UploadId")
            public String uploadId = null;
            @JacksonXmlProperty(localName = "PartNumberMarker")
            public String partNumberMarker = null;
            @JacksonXmlProperty(localName = "NextPartNumberMarker")
            public String nextPartNumberMarker = null;
            @JacksonXmlProperty(localName = "MaxParts")
            public int maxParts;
            @JacksonXmlProperty(localName = "IsTruncated")
            public boolean isTruncated;

            @JacksonXmlProperty(localName = "Part")
            public List<Part> parts = null;
        }

        @JacksonXmlRootElement
        static class Part {

            //@DataMember
            public int partNumber;
            //@DataMember
            public String eTag = null;
        }

        @JacksonXmlRootElement(namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        static class CompleteMultipartUpload {
            @JacksonXmlProperty(localName = "Part")
            public List<Part> parts = null;
        }
    }

    private HttpResponse<?> multipartUpload(PutRequest request, SeekableByteChannel
            sourceStream, HttpRequest.Builder uploadRequest) throws IOException, InterruptedException {
        CopyOnWriteArrayList<XmlModels.Part> parts = new CopyOnWriteArrayList<>();
        String uploadId = null;

        try {
            // see if there's already a multipart upload in progress

            var uploads = getUploadsInProgress(request);
            if (uploads.size() > 0) {
                uploadId = uploads.stream().findFirst().get().uploadId;

                var listPartsRequest = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", url, request.bucket, request.filename(), uploadId)));

                var listPartsResult = client.send(listPartsRequest, XmlModels.ListPartsResult.class);
                parts.addAll(listPartsResult.parts);
            }
        } catch (Client.HttpException ex) {
            if (ex.statusCode != 404) throw ex;
        }

        if (uploadId == null) {
            uploadRequest.POST(HttpRequest.BodyPublishers.noBody()); // TODO

            var initiateMultipartUploadResult = client.send(uploadRequest, XmlModels.InitiateMultipartUploadResult.class);
            if (initiateMultipartUploadResult == null) return null; // dry run

            uploadId = initiateMultipartUploadResult.uploadId;
        }

        var totalParts = sourceStream.size() / request.multipartUploadChunkSize;
        if (sourceStream.size() % request.multipartUploadChunkSize != 0) totalParts++;

        var tasks = new ArrayList<Future<?>>();

        ExecutorService es = Executors.newFixedThreadPool((int) totalParts);
        for (var i = new AtomicInteger(1); i.get() <= totalParts; i.addAndGet(2)) {
            if (request.multipartUploadSkipParts.contains(i.get()) || parts.stream().anyMatch(x -> x.partNumber == i.get()))
                continue;

            String uploadId_ = uploadId;
            tasks.add(es.submit(() -> {
                try {
                    var buffer = ByteBuffer.allocate(request.multipartUploadChunkSize);
                    int length;

                    synchronized (sourceStream) {
Debug.printf("[%d] sourceStream: %d/%d", i.get(), sourceStream.position(), sourceStream.size());
                        sourceStream.position((i.get() - 1L) * request.multipartUploadChunkSize);
                        length = sourceStream.read(buffer);
Debug.printf("[%d] length: %d/%d", i.get(), length, buffer.capacity());
                    }

                    var partRequest = HttpRequest.newBuilder()
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(buffer.array()))
                            .uri(URI.create(String.format("%s/%s/%s?partNumber=%s&uploadId=%s", url, request.bucket, request.filename(), i.get(), uploadId_)));

                    var md5 = MessageDigest.getInstance("MD5");
                    md5.update(buffer.array(), 0, length);
                    partRequest.header("ContentMD5", ByteUtil.toHexString(md5.digest()));

                    var partResponse = client.send(partRequest, HttpResponse.class);
                    if (partResponse.headers().firstValue("ETag").isEmpty())
                        throw new Exception("Invalid multipart upload response for part " + i.get());

                    var part = new XmlModels.Part();
                    part.partNumber = i.get();
                    part.eTag = partResponse.headers().firstValue("ETag").get();
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

        var ms = new ByteArrayOutputStream();
        XmlModels.CompleteMultipartUpload cmu = new XmlModels.CompleteMultipartUpload();
        parts.sort(Comparator.comparingInt(x -> x.partNumber));
        cmu.parts = parts;
        Client.jaxson.writeValue(ms, cmu);
        String xml = ms.toString();
        var httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
                .header("Content-Type", "application/xml")
                .uri(URI.create(String.format("%s/%s/%s?uploadId=%s", url, request.bucket, request.filename(), uploadId)));
        return client.send(httpRequest, HttpResponse.class);
    }

    static class DeleteRequest {

        public String bucket;

        public String remoteFilename;

        public boolean keepOldVersion;

        public boolean cascadeDelete;
    }

    public HttpResponse<?> delete(DeleteRequest request) throws IOException, InterruptedException {
        if (request.bucket == null) throw new IllegalArgumentException("identifier required");
        if (request.bucket.contains("/"))
            throw new IllegalArgumentException("slash not allowed in bucket name; use .remoteFilename to specify the file to delete in a bucket");

        String requestUri = url + "/" + request.bucket;
        if (request.remoteFilename != null) requestUri += "/" + URLEncoder.encode(request.remoteFilename, StandardCharsets.UTF_8);

        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .DELETE();
        if (request.keepOldVersion) httpRequest.header("x-archive-keep-old-version", "1");
        if (request.cascadeDelete) httpRequest.header("x-archive-cascade-delete", "1");

        return client.send(httpRequest, HttpResponse.class);
    }

    static class HeadRequest {

        public String bucket;

        public String remoteFilename;

        public HeadRequest(String bucket, String remoteFilename) {
            if (bucket == null) throw new IllegalArgumentException("identifier required");
            if (bucket.contains("/"))
                throw new IllegalArgumentException("slash not allowed in bucket name; use .remoteFilename to specify the file to delete in a bucket");

            this.bucket = bucket;
            this.remoteFilename = remoteFilename;
        }

        URI toUri() {
            String requestUri = url + "/" + this.bucket;
            if (this.remoteFilename != null)
                requestUri += "/" + URLEncoder.encode(this.remoteFilename, StandardCharsets.UTF_8);
            return URI.create(requestUri);
        }
    }

    public HttpResponse<Void> head(HeadRequest request) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder()
                .uri(request.toUri())
                .method("HEAD", HttpRequest.BodyPublishers.noBody()); // WTF #HEAD() is from java20

        return client.send(httpRequest, HttpResponse.class);
    }

    static class UseLimitResponse {

        public String bucket;

        public String accessKey;

        @SerializedName("over_limit")
        public int overLimit;

        static class Detail_ {

            @SerializedName("accesskey_ration")
            public Long accessKeyRation;

            @SerializedName("accesskey_tasks_queued")
            public Long accessKeyTasksQueued;

            @SerializedName("bucket_ration")
            public Long bucketRation;

            @SerializedName("bucket_tasks_queued")
            public Long bucketTasksQueued;

            @SerializedName("limit_reason")
            public String limitReason;

            @SerializedName("rationing_engaged")
            public Long rationingEngaged;

            @SerializedName("rationing_level")
            public Long rationingLevel;

            @SerializedName("total_global_limit")
            public Long totalGlobalLimit;

            @SerializedName("total_tasks_queued")
            public Long totalTasksQueued;
        }

        public Detail_ detail;
    }

    public UseLimitResponse getUseLimit(String bucket/* = ""*/) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        query.put("check_limit", "1");
        query.put("accesskey", client.accessKey);
        query.put("bucket", bucket);

        return client.get(url, query, UseLimitResponse.class);
    }
}
