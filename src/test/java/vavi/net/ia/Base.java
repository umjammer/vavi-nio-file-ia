package vavi.net.ia;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import vavi.net.ia.dotnet.KeyValuePair;
import vavi.util.ByteUtil;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public abstract class Base {

    static Client _client = null;
    static Config _config = null;

    static LocalDate _startLocalDate, _endLocalDate;
    static LocalDateTime _startDateTime, _endDateTime;

    @BeforeAll
    static void TestInitialize() throws IOException, InterruptedException {

        _config = new Config();

        try {
            PropsEntity.Util.bind(_config);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (_config.AccessKey == null || _config.AccessKey.isEmpty()) {
            throw new IllegalStateException("To run tests, please create a private settings file or set environment variables. For details visit https://github.com/experimentaltvcenter/InternetArchive.NET/blob/main/docs/DEVELOPERS.md#unit-tests");
        }

        _client = Client.Create(_config.AccessKey, _config.SecretKey, false, false);
        Client.decorators.add(_client::RequestInteractivePriority);

        _endDateTime = LocalDateTime.from(Instant.now()).minus(1, ChronoUnit.DAYS);
        _startDateTime = _endDateTime.minus(-7, ChronoUnit.DAYS);

        _endLocalDate = LocalDate.of(_endDateTime.getYear(), _endDateTime.getMonth(), _endDateTime.getDayOfMonth());
        _startLocalDate = LocalDate.of(_startDateTime.getYear(), _startDateTime.getMonth(), _startDateTime.getDayOfMonth());

        if (!Files.exists(Paths.get(_config.LocalFilename)))
            Files.writeString(Path.of(_config.LocalFilename), "test file for unit tests - ok to delete");

        Metadata.ReadResponse response = _client.Metadata.ReadAsync(_config.TestItem);
        if (response.IsDark) {
            _client.Tasks.MakeUndarkAsync(_config.TestItem, "used in automated tests", null);
            WaitForServerAsync(_config.TestItem);
        }

        if (response.Files.stream().anyMatch(x -> x.Format.equals("Text") && x.Name.equals(_config.RemoteFilename))) {
            CreateTestItemAsync(_config.TestItem, null);
        }
        response.close();
    }

    private static String _sharedTestIdentifier = null;

    static String GetSharedTestIdentifierAsync() {
        if (_sharedTestIdentifier != null) return _sharedTestIdentifier;

        try {
            return _sharedTestIdentifier = CreateTestItemAsync(null, null);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static String GenerateIdentifier() {
        return "tmp-" + UUID.randomUUID();
    }

    public static String CreateTestItemAsync(String identifier/*=null*/, List<KeyValuePair<String, Object>> extraMetadata/*=null*/) throws IOException, InterruptedException {
        if (identifier == null) {
            identifier = GenerateIdentifier();
        }

        List<KeyValuePair<String, Object>> metadata = new ArrayList<>();
        metadata.add(new KeyValuePair<>("collection", "test_collection"));
        metadata.add(new KeyValuePair<>("mediatype", "texts"));
        metadata.add(new KeyValuePair<>("noindex", "true"));

        if (extraMetadata != null) metadata.addAll(extraMetadata);

        Item.PutRequest request = new Item.PutRequest();
        request.Bucket = identifier;
        request.LocalPath = Paths.get(_config.LocalFilename);
        request.RemoteFilename = _config.RemoteFilename;
        request.Metadata = metadata;
        request.CreateBucket = true;
        request.NoDerive = true;
        _client.Item.PutAsync(request);

        WaitForServerAsync(identifier);
        return identifier;
    }

    static void VerifyHashesAsync(Item.PutRequest request) throws IOException, InterruptedException {
        assertNotNull(request.Bucket);

        var sourceStream = request.SourceStream == null ? Files.newByteChannel(request.LocalPath) : request.SourceStream;

        try {
            ByteBuffer buffer = ByteBuffer.allocate((int) sourceStream.size());
            sourceStream.read(buffer);

            var md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer.array());
            var sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(buffer.array());

            try (var metadata = _client.Metadata.ReadAsync(request.Bucket)) {

                assertNotNull(metadata);
                assertTrue(metadata.Files.stream().findFirst().isPresent());
                var file = metadata.Files.stream().filter(x -> x.Name.equals(request.Filename(false))).findFirst().orElseGet(Metadata.ReadResponse.File::new);
                assertNotNull(file);

                assertEquals(ByteUtil.toHexString(md5.digest()), file.Md5, "MD5 does not match");
                assertEquals(ByteUtil.toHexString(sha1.digest()), file.Sha1, "SHA1 does not match");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } finally {
            if (request.SourceStream == null) sourceStream.close();
        }
    }

    public static void WaitForServerAsync(String identifier) throws IOException, InterruptedException {
        WaitForServerAsync(identifier, 20, 10);
    }

    public static void WaitForServerAsync(String identifier, int minutes/* = 20*/, int secondsBetween/* = 10*/) throws IOException, InterruptedException {
        int retries = minutes * 60 / secondsBetween;

        for (int i = 0; i < retries; i++) {
            Tasks.GetRequest taskRequest = new Tasks.GetRequest();
            taskRequest.Identifier = identifier;

            Tasks.GetResponse response = _client.Tasks.GetAsync(taskRequest);
            assertTrue(response.Success);

            var summary = response.Value.Summary;
            assertEquals(0, summary.Error);

            if (summary.Queued == 0 && summary.Running == 0) return;
            Thread.sleep(secondsBetween * 1000L);
        }

        fail(String.format("timeout of %d minutes exceeded", minutes));
    }
}
