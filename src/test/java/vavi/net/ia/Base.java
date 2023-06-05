package vavi.net.ia;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import vavi.net.ia.dotnet.KeyValuePair;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public abstract class Base {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static Client client = null;
    static Config config = null;

    static LocalDate startDate, endDate;
    static LocalDateTime startDateTime, endDateTime;

    @BeforeAll
    static void testInitialize() throws IOException, InterruptedException {

        config = new Config();

        if (!localPropertiesExists()) {
            throw new IllegalStateException("config is null");
        }

        try {
            PropsEntity.Util.bind(config);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (config.accessKey == null || config.accessKey.isEmpty()) {
            throw new IllegalStateException("To run tests, please create a private settings file or set environment variables. For details visit https://github.com/experimentaltvcenter/InternetArchive.NET/blob/main/docs/DEVELOPERS.md#unit-tests");
        }

        client = Client.createByKey(config.accessKey, config.secretKey, false, false);
        client.decorators.add(client::requestInteractivePriority);

        endDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);
        startDateTime = endDateTime.minus(7, ChronoUnit.DAYS);

        endDate = LocalDate.from(endDateTime);
        startDate = LocalDate.from(startDateTime);

        if (!Files.exists(Paths.get(config.localFilename)))
            Files.writeString(Path.of(config.localFilename), "test file for unit tests - ok to delete");

//        Metadata.ReadResponse response = client.metadata.read(config.testItem);
//        if (response.isDark) {
//            client.tasks.makeUndark(config.testItem, "used in automated tests", null);
//            waitForServer(config.testItem);
//        }
//
//        if (response.files.stream().anyMatch(x -> x.format.equals("Text") && x.name.equals(config.remoteFilename))) {
//            createTestItem(config.testItem, null);
//        }
//        response.close();
    }

    private static String sharedTestIdentifier = null;

    static String getSharedTestIdentifier() throws IOException, InterruptedException {
        if (sharedTestIdentifier != null) return sharedTestIdentifier;

        return sharedTestIdentifier = createTestItem(null, null);
    }

    static String generateIdentifier() {
        return "tmp-" + UUID.randomUUID();
    }

    public static String createTestItem(String identifier/*=null*/, List<KeyValuePair<String, Object>> extraMetadata/*=null*/) throws IOException, InterruptedException {
        try {
            Item.HeadRequest headRequest = new Item.HeadRequest(config.testBucket, config.remoteFilename);
            HttpResponse<?> r = client.item.head(headRequest);
Debug.println(config.testBucket + " exists, use this");
            return config.testBucket;
        } catch (Client.HttpException e) {
Debug.println(config.testBucket + " something wrong: " + e.statusCode);
        }

        if (identifier == null) {
            identifier = generateIdentifier();
        }

        List<KeyValuePair<String, Object>> metadata = new ArrayList<>();
        metadata.add(new KeyValuePair<>("collection", "test_collection"));
        metadata.add(new KeyValuePair<>("mediatype", "texts"));
        metadata.add(new KeyValuePair<>("noindex", "true"));

        if (extraMetadata != null) metadata.addAll(extraMetadata);

        Item.PutRequest request = new Item.PutRequest();
        request.bucket = identifier;
        request.localPath = Paths.get(config.localFilename);
        request.remoteFilename = config.remoteFilename;
        request.metadata = metadata;
        request.createBucket = true;
        request.noDerive = true;
        client.item.put(request);

        waitForServer(identifier);
        config.testBucket = identifier;
        return identifier;
    }

    static void verifyHashes(Item.PutRequest request) throws IOException, InterruptedException {
        assertNotNull(request.bucket);

        var sourceStream = request.sourceStream == null ? Files.newByteChannel(request.localPath) : request.sourceStream;

        try {
            ByteBuffer buffer = ByteBuffer.allocate((int) sourceStream.size());
            sourceStream.read(buffer);

            var md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer.array());
            var sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(buffer.array());

            var metadata = client.metadata.read(request.bucket);

            assertNotNull(metadata);
            assertTrue(metadata.files.stream().findFirst().isPresent());
            var fileOption = metadata.files.stream().filter(x -> x.name.equals(request.filename(false))).findFirst();
            assertTrue(fileOption.isPresent());

            var file = fileOption.get();
Debug.printf("%s, %s", ByteUtil.toHexString(md5.digest()), file.md5);
Debug.printf("%s, %s", ByteUtil.toHexString(sha1.digest()), file.sha1);
            assertEquals(ByteUtil.toHexString(md5.digest()), file.md5, "MD5 does not match");
            assertEquals(ByteUtil.toHexString(sha1.digest()), file.sha1, "SHA1 does not match");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } finally {
            if (request.sourceStream == null) sourceStream.close();
        }
    }

    public static void waitForServer(String identifier) throws IOException, InterruptedException {
        waitForServer(identifier, 20, 10);
    }

    public static void waitForServer(String identifier, int minutes, int secondsBetween) throws IOException, InterruptedException {
        int retries = minutes * 60 / secondsBetween;

        for (int i = 0; i < retries; i++) {
            Tasks.GetRequest taskRequest = new Tasks.GetRequest();
            taskRequest.identifier = identifier;

            Tasks.GetResponse response = client.tasks.get(taskRequest);
            assertTrue(response.success);

            var summary = response.value.summary;
            assertEquals(0, summary.error);

            if (summary.queued == 0 && summary.running == 0) return;
            Thread.sleep(secondsBetween * 1000L);
        }

        fail(String.format("timeout of %d minutes exceeded", minutes));
    }
}
