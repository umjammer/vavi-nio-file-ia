package vavi.net.ia;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TaskTests extends Base {

    @Test
    public void GetTasksAsync() throws IOException, InterruptedException {
        var request = new Tasks.GetRequest();
        request.Submitter = _config.EmailAddress;
        var response = _client.Tasks.GetAsync(request);

        Assertions.assertTrue(response.Success);
    }

    @Test
    public void GetItemTaskAsync() throws Exception {
        String identifier = GetSharedTestIdentifierAsync();

        var request = new Tasks.GetRequest();
        request.Identifier = identifier;
        request.Catalog = true;
        request.History = true;
        var response = _client.Tasks.GetAsync(request);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.Success);
        Assertions.assertNull(response.Cursor);

        Assertions.assertNotNull(response.Value);
        Assertions.assertNotNull(response.Value.Summary);
        Assertions.assertNotNull(response.Value.Summary.Error);
        Assertions.assertNotNull(response.Value.Summary.Paused);
        Assertions.assertNotNull(response.Value.Summary.Queued);
        Assertions.assertNotNull(response.Value.Summary.Running);

        Assertions.assertNotNull(response.Value.History);
        var history = response.Value.History.stream().findFirst().get();

        Assertions.assertNotNull(history.Args);
        Assertions.assertNotNull(history.Command);
        Assertions.assertNotNull(history.DateSubmitted);
        Assertions.assertNotNull(history.Finished);
        Assertions.assertEquals(_config.TestItem, history.Identifier);
        Assertions.assertNotNull(history.Priority);
        Assertions.assertNotNull(history.Server);
        Assertions.assertNotNull(history.Submitter);
        Assertions.assertNotNull(history.TaskId);
    }

    private static void ValidateSubmitResponse(Tasks.SubmitResponse response) {
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.Success);
        Assertions.assertNotNull(response.Value);
        Assertions.assertNotNull(response.Value.TaskId);
        Assertions.assertNotNull(response.Value.Log);
    }

    @Test
    public void DarkUndarkItemAsync() throws Exception {
        String identifier = CreateTestItemAsync(null, null);

        var response = _client.Tasks.MakeDarkAsync(identifier, "test item - please delete", null);
        ValidateSubmitResponse(response);

        WaitForServerAsync(identifier, 200, 3);

        response = _client.Tasks.MakeUndarkAsync(identifier, "test item - please delete", null);
        ValidateSubmitResponse(response);

        WaitForServerAsync(identifier, 200, 3);
    }
}