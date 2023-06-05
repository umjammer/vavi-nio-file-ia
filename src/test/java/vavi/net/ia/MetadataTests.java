package vavi.net.ia;

import java.io.IOException;
import java.io.StringWriter;

import com.google.gson.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MetadataTests extends Base {

    protected static String testItem = null;

    @BeforeAll
    static void classInit() throws IOException, InterruptedException {
        testItem = createTestItem(null, null);
    }

    @Test
    public void readMetadata() throws Exception {
        Metadata.ReadResponse response = client.metadata.read(testItem);

        assertNotNull(response);
        assertNotNull(response.dataNodePrimary);
        assertNotNull(response.dataNodeSecondary);
        assertNull(response.dataNodeSolo);
        assertNotNull(response.dateCreated);
        assertNotNull(response.dateLastUpdated);
        assertNotNull(response.dir);
        assertNotNull(response.files);
        assertNotNull(response.metadata);
        assertNotNull(response.size);
        assertNotNull(response.uniq);

        assertNotNull(response.workableServers);
        assertTrue(response.workableServers.size() > 0);
        // Assertions.assertNull(response.serversUnavailable); may be null or not

        var collection = response.metadata.entrySet().stream().filter(x -> x.getKey().equals("collection")).findFirst().get();
        assertNotNull(collection);

        var file = response.files.stream().filter(x -> x.format.equals("Text") && x.name.equals(config.remoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new);

        assertNotNull(file);
        assertNotNull(file.crc32);
        assertNotNull(file.format);
        assertNotNull(file.md5);
        assertNotNull(file.modificationDate);
        assertNotNull(file.name);
        assertNotNull(file.sha1);
        assertNotNull(file.size);
        assertNotNull(file.source);
        assertNotNull(file.virusCheckDate);
    }

    @Test
    public void writeMetadata() throws IOException, InterruptedException {
        var readResponse1 = client.metadata.read(testItem);

        JsonPatchBuilder patch = Json.createPatchBuilder();

        JsonElement element;
        String value;
        if ((element = readResponse1.metadata.get("testkey")) != null) {
            value = element.getAsString().equals("flop") ? "flip" : "flop";
            patch.replace("/testkey", value);
        } else {
            value = "flip";
            patch.add("/testkey", value);
        }

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);

        jsonWriter.write(patch.build().toJsonArray()); // need not be an array

        var writeResponse = client.metadata.write(testItem, stringWriter.toString());

        assertNotNull(writeResponse);
        assertTrue(writeResponse.success);
        assertNull(writeResponse.error);
        assertNotNull(writeResponse.log);
        assertNotNull(writeResponse.taskId);

        var readResponse2 = client.metadata.read(testItem);
        assertEquals(value, readResponse2.metadata.get("testkey").getAsString());
    }
}