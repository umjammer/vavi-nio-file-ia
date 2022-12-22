package vavi.net.ia;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vavi.net.ia.dotnet.KeyValuePair;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


public class ItemTests extends Base {

    private static Path _largeFilePath = null;
    private static final String _largeFileRemoteName = "large.txt";

    @BeforeAll
    public static void ClassInit() throws IOException {
        byte[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz          ".getBytes(StandardCharsets.US_ASCII);
        byte[] s = new byte[1024 * 1024 * 11]; // 11MB

        Random random = new Random();
        for (int i = 0; i < s.length; i++) {
            s[i] = chars[random.nextInt(chars.length)];
        }

        _largeFilePath = Files.createTempDirectory(ItemTests.class.getPackageName()).resolve(_largeFileRemoteName);
        try (OutputStream stream = Files.newOutputStream(_largeFilePath)) {
            stream.write(s, 0, s.length);
        }
    }

    @Test
    public void GetUseLimitAsync() throws IOException, InterruptedException {
        String identifer = GetSharedTestIdentifierAsync();

        var response = _client.Item.GetUseLimitAsync(_config.TestItem);

        assertNotNull(response);

        Assertions.assertEquals(identifer, response.Bucket);
        Assertions.assertEquals(0, response.OverLimit);

        assertNotNull(response.Detail);
        Assertions.assertEquals("", response.Detail.LimitReason);

        assertNotNull(response.Detail.AccessKeyRation);
        assertNotNull(response.Detail.AccessKeyTasksQueued);
        assertNotNull(response.Detail.BucketRation);
        assertNotNull(response.Detail.BucketTasksQueued);
        assertNotNull(response.Detail.RationingEngaged);
        assertNotNull(response.Detail.RationingLevel);
        assertNotNull(response.Detail.TotalGlobalLimit);
        assertNotNull(response.Detail.TotalTasksQueued);
    }

    private static void AssertionsHasMetadata(Metadata.ReadResponse response, String key, String expectedValue) {
        assertNotNull(response);
        assertNotNull(response.Metadata);
        JsonElement element = response.Metadata.get(key);
        assertNotNull(element);
        Assertions.assertEquals(expectedValue, element.getAsString());
    }

    private static void AssertionsNoMetadata(Metadata.ReadResponse response, String key) {
        assertNotNull(response);
        assertNotNull(response.Metadata);
        JsonElement element = response.Metadata.get(key);
        Assertions.assertNull(element);
    }

    @Test
    public void CreateModifyDeleteAsync() throws Exception {
        final String _remoteFilename2 = "hello; again #2.txt";

        var extraMetadata = new ArrayList<KeyValuePair<String, Object>>() {
            {
                add(new KeyValuePair<>("title", "test_title"));
                add(new KeyValuePair<>("testfield", "hello"));
            }
        };

        var identifier = CreateTestItemAsync("extraMetadata", extraMetadata);

        WaitForServerAsync(identifier);

        // verify metadata

        var response1 = _client.Metadata.ReadAsync(identifier);
        AssertionsHasMetadata(response1, "title", "test_title");
        AssertionsHasMetadata(response1, "testfield", "hello");

        assertNotNull(response1.Metadata);
        var metadataFiltered = response1.Metadata.entrySet().stream()
                .filter(x -> !x.getKey().equals("title") && !x.getKey().equals("testfield") && !x.getKey().equals("collection"))
                .map(x -> new KeyValuePair<String, Object>(x.getKey(), x.getValue())).collect(Collectors.toList());
        metadataFiltered.add(new KeyValuePair<>("collection", "test_collection"));

        // delete existing metadata

        _client.Item.PutAsync(new Item.PutRequest() {{
            Bucket = identifier;
            Metadata = metadataFiltered;
            DeleteExistingMetadata = true;
        }});

        WaitForServerAsync(identifier);

        try (var response2 = _client.Metadata.ReadAsync(identifier)) {
            assertNotNull(response2);
            AssertionsHasMetadata(response2, "title", identifier); // title reverts to identifier/bucket when removed
            AssertionsNoMetadata(response2, "testfield");
        }

        // add another file

        _client.Item.PutAsync(new Item.PutRequest() {{
            Bucket = identifier;
            LocalPath = Paths.get(_config.LocalFilename);
            RemoteFilename = _remoteFilename2;
            NoDerive = true;
        }});

        WaitForServerAsync(identifier);

        try (var response3 = _client.Metadata.ReadAsync(identifier)) {
            assertNotNull(response3.Files.stream().filter(x -> x.Name.equals(_config.RemoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
            assertNotNull(response3.Files.stream().filter(x -> x.Name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        }

        // delete file

        _client.Item.DeleteAsync(new Item.DeleteRequest() {{
            Bucket = identifier;
            RemoteFilename = _remoteFilename2;
            CascadeDelete = true;
            KeepOldVersion = false;
        }});

        WaitForServerAsync(identifier);

        try (var response4 = _client.Metadata.ReadAsync(identifier)) {
            assertNotNull(response4.Files.stream().filter(x -> x.Name.equals(_config.RemoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
            Assertions.assertNull(response4.Files.stream().filter(x -> x.Name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        }

        // delete other file

        _client.Item.DeleteAsync(new Item.DeleteRequest() {{
            Bucket = identifier;
            RemoteFilename = _config.RemoteFilename;
            CascadeDelete = true;
            KeepOldVersion = false;
        }});

        WaitForServerAsync(identifier);

        try (Metadata.ReadResponse response5 = _client.Metadata.ReadAsync(identifier)) {
            Assertions.assertNull(response5.Files.stream().filter(x -> x.Name.equals(_config.RemoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
            Assertions.assertNull(response5.Files.stream().filter(x -> x.Name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        }
        if (_config.CanDelete) {
            _client.Tasks.SubmitAsync(identifier, Tasks.Command.Delete, null, null);
            WaitForServerAsync(identifier);
        }
    }

    @Test
    void CreateAddStreamAsync() throws Exception {
        String _remoteFilename2 = "hello; again #2.txt";

        var identifier = CreateTestItemAsync(null, null);

        // add another file via stream

        var putRequest = new Item.PutRequest();
        putRequest.Bucket = identifier;
        putRequest.SourceStream = Files.newByteChannel(Path.of(_config.LocalFilename));
        putRequest.RemoteFilename = _remoteFilename2;
        putRequest.NoDerive = true;

        _client.Item.PutAsync(putRequest);
        WaitForServerAsync(identifier);

        putRequest.SourceStream = Files.newByteChannel(Path.of(_config.LocalFilename));
        VerifyHashesAsync(putRequest);

        try (var response = _client.Metadata.ReadAsync(identifier)) {
            assertNotNull(response.Files.stream().filter(x -> x.Name.equals(_config.RemoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
            assertNotNull(response.Files.stream().filter(x -> x.Name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        }
    }

    private static Item.PutRequest CreateMultipartRequest(String identifier, boolean createBucket/* =true*/) {
        List<KeyValuePair<String, Object>> metadata = new ArrayList<>();
        metadata.add(new KeyValuePair<>("collection", "test_collection"));
        metadata.add(new KeyValuePair<>("mediatype", "texts"));
        metadata.add(new KeyValuePair<>("noindex", "true"));

        Item.PutRequest request = new Item.PutRequest();
        request.Bucket = identifier;
        request.LocalPath = _largeFilePath;
        request.Metadata = metadata;
        request.CreateBucket = createBucket;
        request.NoDerive = true;
        request.MultipartUploadMinimumSize = 0; // force multipart upload
        request.MultipartUploadChunkSize = 1024 * 1024 * 5; // 5 MB chunks
        return request;
    }

    @Test
    public void UploadMultipartCancelAsync() throws Exception {
        String identifier = GenerateIdentifier();
        var putRequest = CreateMultipartRequest(identifier, true);

        try {
            _client.Item.PutAsync(putRequest, 3000);
            fail("CancellationToken ignored");
        } catch (Exception ex) {
            assertInstanceOf(InterruptedException.class, ex);
        }
    }

    @Test
    public void UploadMultipartAbortImmediatelyAsync() throws Exception {
        String identifier = GetSharedTestIdentifierAsync();
        _client.Item.AbortUploadAsync(identifier);
    }

    @Test
    public void UploadMultipartAbortAsync() throws Exception {
        String identifier = GenerateIdentifier();
        var putRequest = CreateMultipartRequest(identifier, true);
        _client.Item.PutAsync(putRequest);

        Thread.sleep(20 * 1000);
        _client.Item.AbortUploadAsync(putRequest);
    }

    @Test
    public void UploadMultipartAsync() throws Exception {
        String identifier = GenerateIdentifier();

        var putRequest = CreateMultipartRequest(identifier, true);
        _client.Item.PutAsync(putRequest);

        WaitForServerAsync(identifier);
        VerifyHashesAsync(putRequest);
    }

    @Test
    public void UploadMultipartWithContinueAsync() throws Exception {
        String identifier = GenerateIdentifier();

        var putRequest = CreateMultipartRequest(identifier, true);
        putRequest.MultipartUploadSkipParts = List.of(1, 2);

        _client.Item.PutAsync(putRequest);

        putRequest = CreateMultipartRequest(identifier, false);
        _client.Item.PutAsync(putRequest);

        WaitForServerAsync(identifier);
        VerifyHashesAsync(putRequest);
    }

    @Test
    @Disabled("original deleted")
    public void AbortUpload() throws Exception {
        String identifier = _config.TestItem;
        var putRequest = CreateMultipartRequest(identifier, true);

        _client.Item.AbortUploadAsync(putRequest);
        WaitForServerAsync(identifier);
    }

    @Test
    @Disabled("original deleted")
    public void AbortAllBucketUploads() throws Exception {
        String identifier = _config.TestItem;

        _client.Item.AbortUploadAsync(identifier);
        WaitForServerAsync(identifier);
    }

    @Test
    @Disabled("original deleted")
    public void StartThenAbortUpload() throws Exception {
        String identifier = "etc-tmp-{Guid.NewGuid():N}";
        Item.PutRequest putRequest = CreateMultipartRequest(identifier, true);

        putRequest.MultipartUploadSkipParts = List.of(1, 2);

        _client.Item.PutAsync(putRequest);
        WaitForServerAsync(identifier);

        _client.Item.AbortUploadAsync(putRequest);
    }

    @Test
    @Disabled("original deleted")
    public void UploadMultipart() throws IOException, InterruptedException {
        String identifier = "etc-tmp-{Guid.NewGuid():N}";
        _client.Item.PutAsync(CreateMultipartRequest(identifier, true));
    }

    @Test
    @Disabled("original deleted")
    public void UploadMultipartExistingWithContinue() throws Exception {
        String identifier = _config.TestItem;
        Item.PutRequest putRequest = CreateMultipartRequest(identifier, /*createBucket:*/false);
        putRequest.MultipartUploadSkipParts = List.of(1, 2);

        _client.Item.PutAsync(putRequest);

        putRequest.MultipartUploadSkipParts = new ArrayList<>();
        _client.Item.PutAsync(putRequest);

        WaitForServerAsync(identifier);
    }
}
