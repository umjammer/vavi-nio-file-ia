package vavi.net.ia;

import java.io.IOException;
import java.io.StringWriter;
import javax.json.Json;
import javax.json.JsonPatchBuilder;
import javax.json.JsonWriter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MetadataTests extends Base {

    protected static String _testItem = null;

    @BeforeAll
    static void ClassInit() throws IOException, InterruptedException {
        _testItem = CreateTestItemAsync(null, null);
    }

    @Test
    public void ReadMetadataAsync() throws Exception {
        Metadata.ReadResponse response = _client.Metadata.ReadAsync(_testItem);

        assertNotNull(response);
        assertNotNull(response.DataNodePrimary);
        assertNotNull(response.DataNodeSecondary);
        assertNull(response.DataNodeSolo);
        assertNotNull(response.DateCreated);
        assertNotNull(response.DateLastUpdated);
        assertNotNull(response.Dir);
        assertNotNull(response.Files);
        assertNotNull(response.Metadata);
        assertNotNull(response.Size);
        assertNotNull(response.Uniq);

        assertNotNull(response.WorkableServers);
        assertTrue(response.WorkableServers.length > 0);
        // Assertions.assertNull(response.ServersUnavailable); may be null or not

        var collection = response.Metadata.entrySet().stream().filter(x -> x.getKey().equals("collection")).findFirst().get();
        assertNotNull(collection);

        var file = response.Files.stream().filter(x -> x.Format.equals("Text") && x.Name.equals(_config.RemoteFilename)).findFirst().orElseGet(Metadata.ReadResponse.File::new);

        assertNotNull(file);
        assertNotNull(file.Crc32);
        assertNotNull(file.Format);
        assertNotNull(file.Md5);
        assertNotNull(file.ModificationDate);
        assertNotNull(file.Name);
        assertNotNull(file.Sha1);
        assertNotNull(file.Size);
        assertNotNull(file.Source);
        assertNotNull(file.VirusCheckDate);
    }

    @Test
    public void WriteMetadataAsync() throws IOException, InterruptedException {
        var readResponse1 = _client.Metadata.ReadAsync(_testItem);

        JsonPatchBuilder patch = Json.createPatchBuilder();

        JsonObject json = Client._json.fromJson(Client._json.toJson(readResponse1.Metadata), JsonObject.class);

        JsonElement element;
        String value;
        if ((element = json.get("testkey")) != null) {
            value = element.getAsString().equals("flop") ? "flip" : "flop";
            patch.replace("/testkey", value);
        } else {
            value = "flip";
            patch.add("/testkey", value);
        }

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);
        jsonWriter.write(patch.build().toJsonArray());

        var writeResponse = _client.Metadata.WriteAsync(_testItem, stringWriter.toString());

        assertNotNull(writeResponse);
        assertTrue(writeResponse.Success);
        assertNull(writeResponse.Error);
        assertNotNull(writeResponse.Log);
        assertNotNull(writeResponse.TaskId);

        var readResponse2 = _client.Metadata.ReadAsync(_testItem);
        assertEquals(value, readResponse2.Metadata.get("testkey").getAsString());
        readResponse2.close();
    }
}