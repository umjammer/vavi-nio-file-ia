package vavi.net.ia;

import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vavi.net.ia.dotnet.KeyValuePair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


public class ItemTests extends Base {

    private static Path largeFilePath = null;
    private static final String largeFileRemoteName = "large.txt";

    @BeforeAll
    public static void classInit() throws IOException {
        byte[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz          ".getBytes(StandardCharsets.US_ASCII);
        byte[] s = new byte[1024 * 1024 * 11]; // 11MB

        Random random = new Random();
        for (int i = 0; i < s.length; i++) {
            s[i] = chars[random.nextInt(chars.length)];
        }

        largeFilePath = Files.createTempDirectory(ItemTests.class.getPackageName()).resolve(largeFileRemoteName);
        try (OutputStream stream = Files.newOutputStream(largeFilePath)) {
            stream.write(s, 0, s.length);
        }
    }

    @Test
    public void getUseLimit() throws IOException, InterruptedException {
        String identifier = getSharedTestIdentifier();

        var response = client.item.getUseLimit(config.testItem);

        assertNotNull(response);

        Assertions.assertEquals(identifier, response.bucket);
        Assertions.assertEquals(0, response.overLimit);

        assertNotNull(response.detail);
        Assertions.assertEquals("", response.detail.limitReason);

        assertNotNull(response.detail.accessKeyRation);
        assertNotNull(response.detail.accessKeyTasksQueued);
        assertNotNull(response.detail.bucketRation);
        assertNotNull(response.detail.bucketTasksQueued);
        assertNotNull(response.detail.rationingEngaged);
        assertNotNull(response.detail.rationingLevel);
        assertNotNull(response.detail.totalGlobalLimit);
        assertNotNull(response.detail.totalTasksQueued);
    }

    private static void assertHasMetadata(Metadata.ReadResponse response, String key, String expectedValue) {
        assertNotNull(response);
        assertNotNull(response.metadata);
        JsonElement element = response.metadata.get(key);
        assertNotNull(element);
        Assertions.assertEquals(expectedValue, element.getAsString());
    }

    private static void assertNoMetadata(Metadata.ReadResponse response, String key) {
        assertNotNull(response);
        assertNotNull(response.metadata);
        JsonElement element = response.metadata.get(key);
        Assertions.assertNull(element);
    }

    @Test
    public void createModifyDelete() throws Exception {
        final String _remoteFilename2 = "hello; again #2.txt";

        var extraMetadata = new ArrayList<KeyValuePair<String, Object>>() {
            {
                add(new KeyValuePair<>("title", "test_title"));
                add(new KeyValuePair<>("testfield", "hello"));
            }
        };

        var identifier = createTestItem("extraMetadata", extraMetadata);

        waitForServer(identifier);

        // verify metadata

        var response1 = client.metadata.read(identifier);
        assertHasMetadata(response1, "title", "test_title");
        assertHasMetadata(response1, "testfield", "hello");

        assertNotNull(response1.metadata);
        var metadataFiltered = response1.metadata.entrySet().stream()
                .filter(x -> !x.getKey().equals("title") && !x.getKey().equals("testfield") && !x.getKey().equals("collection"))
                .map(x -> new KeyValuePair<String, Object>(x.getKey(), x.getValue())).collect(Collectors.toList());
        metadataFiltered.add(new KeyValuePair<>("collection", "test_collection"));

        // delete existing metadata

        client.item.put(new Item.PutRequest() {{
            bucket = identifier;
            metadata = metadataFiltered;
            deleteExistingMetadata = true;
        }});

        waitForServer(identifier);

        var response2 = client.metadata.read(identifier);
        assertNotNull(response2);
        assertHasMetadata(response2, "title", identifier); // title reverts to identifier/bucket when removed
        assertNoMetadata(response2, "testfield");

        // add another file

        client.item.put(new Item.PutRequest() {{
            bucket = identifier;
            localPath = Paths.get(config.localFilename);
            remoteFilename = _remoteFilename2;
            noDerive = true;
        }});

        waitForServer(identifier);

        var response3 = client.metadata.read(identifier);
        assertNotNull(response3.files.stream().filter(x -> x.name.equals(config.remoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        assertNotNull(response3.files.stream().filter(x -> x.name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));

        // delete file

        client.item.delete(new Item.DeleteRequest() {{
            bucket = identifier;
            remoteFilename = _remoteFilename2;
            cascadeDelete = true;
            keepOldVersion = false;
        }});

        waitForServer(identifier);

        var response4 = client.metadata.read(identifier);
        assertNotNull(response4.files.stream().filter(x -> x.name.equals(config.remoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        Assertions.assertNull(response4.files.stream().filter(x -> x.name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));

        // delete other file

        client.item.delete(new Item.DeleteRequest() {{
            bucket = identifier;
            remoteFilename = config.remoteFilename;
            cascadeDelete = true;
            keepOldVersion = false;
        }});

        waitForServer(identifier);

        Metadata.ReadResponse response5 = client.metadata.read(identifier);
        Assertions.assertNull(response5.files.stream().filter(x -> x.name.equals(config.remoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        Assertions.assertNull(response5.files.stream().filter(x -> x.name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        if (config.canDelete) {
            client.tasks.submit(identifier, Tasks.Command.Delete, null, null);
            waitForServer(identifier);
        }
    }

    @Test
    void createAddStream() throws Exception {
        String _remoteFilename2 = "hello; again #2.txt";

        var identifier = createTestItem(null, null);

        // add another file via stream

        var putRequest = new Item.PutRequest();
        putRequest.bucket = identifier;
        putRequest.sourceStream = Files.newByteChannel(Path.of(config.localFilename));
        putRequest.remoteFilename = _remoteFilename2;
        putRequest.noDerive = true;

        client.item.put(putRequest);
        waitForServer(identifier);

        putRequest.sourceStream = Files.newByteChannel(Path.of(config.localFilename));
        verifyHashes(putRequest);

        var response = client.metadata.read(identifier);
        assertNotNull(response.files.stream().filter(x -> x.name.equals(config.remoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
        assertNotNull(response.files.stream().filter(x -> x.name.equals(_remoteFilename2)).findFirst().orElseGet(Metadata.ReadResponse.File::new));
    }

    private static Item.PutRequest createMultipartRequest(String identifier, boolean createBucket/* =true*/) {
        List<KeyValuePair<String, Object>> metadata = new ArrayList<>();
        metadata.add(new KeyValuePair<>("collection", "test_collection"));
        metadata.add(new KeyValuePair<>("mediatype", "texts"));
        metadata.add(new KeyValuePair<>("noindex", "true"));

        Item.PutRequest request = new Item.PutRequest();
        request.bucket = identifier;
        request.localPath = largeFilePath;
        request.metadata = metadata;
        request.createBucket = createBucket;
        request.noDerive = true;
        request.multipartUploadMinimumSize = 0; // force multipart upload
        request.multipartUploadChunkSize = 1024 * 1024 * 5; // 5 MB chunks
        return request;
    }

    @Test
    public void uploadMultipartCancel() throws Exception {
        String identifier = generateIdentifier();
        var putRequest = createMultipartRequest(identifier, true);

        try {
            client.item.put(putRequest, 3000);
            fail("CancellationToken ignored");
        } catch (Exception ex) {
            assertInstanceOf(InterruptedException.class, ex);
        }
    }

    @Test
    public void uploadMultipartAbortImmediately() throws Exception {
        String identifier = getSharedTestIdentifier();
        client.item.abortUpload(identifier);
    }

    @Test
    public void uploadMultipartAbort() throws Exception {
        String identifier = generateIdentifier();
        var putRequest = createMultipartRequest(identifier, true);
        client.item.put(putRequest);

        Thread.sleep(20 * 1000);
        client.item.abortUpload(putRequest);
    }

    @Test
    public void uploadMultipart() throws Exception {
        String identifier = generateIdentifier();

        var putRequest = createMultipartRequest(identifier, true);
        client.item.put(putRequest);

        waitForServer(identifier);
        verifyHashes(putRequest);
    }

    @Test
    public void uploadMultipartWithContinue() throws Exception {
        String identifier = generateIdentifier();

        var putRequest = createMultipartRequest(identifier, true);
        putRequest.multipartUploadSkipParts = List.of(1, 2);

        client.item.put(putRequest);

        putRequest = createMultipartRequest(identifier, false);
        client.item.put(putRequest);

        waitForServer(identifier);
        verifyHashes(putRequest);
    }

    @Test
    public void head() throws Exception {
        Item.HeadRequest headRequest = new Item.HeadRequest(config.testBucket, config.remoteFilename);

        HttpResponse<?> r = client.item.head(headRequest);
        assertEquals(200, r.statusCode());
    }
}
