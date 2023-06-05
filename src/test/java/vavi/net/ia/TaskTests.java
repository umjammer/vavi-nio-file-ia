package vavi.net.ia;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TaskTests extends Base {

    @Test
    public void getTasks() throws IOException, InterruptedException {
        var request = new Tasks.GetRequest();
        request.submitter = config.emailAddress;
        var response = client.tasks.get(request);

        assertTrue(response.success);
    }

    @Test
    public void getItemTask() throws Exception {
        String identifier = getSharedTestIdentifier();

        var request = new Tasks.GetRequest();
        request.identifier = identifier;
        request.catalog = true;
        request.history = true;
        var response = client.tasks.get(request);

        assertNotNull(response);
        assertTrue(response.success);
        assertNull(response.cursor);

        assertNotNull(response.value);
        assertNotNull(response.value.summary);
        assertNotNull(response.value.summary.error);
        assertNotNull(response.value.summary.paused);
        assertNotNull(response.value.summary.queued);
        assertNotNull(response.value.summary.running);

        assertNotNull(response.value.history);
        var historyOption = response.value.history.stream().findFirst();
        assertTrue(historyOption.isPresent());

        var history = historyOption.get();

        assertNotNull(history.args);
        assertNotNull(history.command);
        assertNotNull(history.dateSubmitted);
        assertNotNull(history.finished);
        assertEquals(identifier, history.identifier);
        assertNotNull(history.priority);
        assertNotNull(history.server);
        assertNotNull(history.submitter);
        assertNotNull(history.taskId);
    }

    private static void validateSubmitResponse(Tasks.SubmitResponse response) {
        assertNotNull(response);
        assertTrue(response.success);
        assertNotNull(response.value);
        assertNotNull(response.value.taskId);
        assertNotNull(response.value.log);
    }

    @Test
    public void darkUndarkItem() throws Exception {
        String identifier = createTestItem(null, null);

        var response = client.tasks.makeDark(identifier, "test item - please delete", null);
        validateSubmitResponse(response);

        waitForServer(identifier, 200, 3);

        response = client.tasks.makeUndark(identifier, "test item - please delete", null);
        validateSubmitResponse(response);

        waitForServer(identifier, 200, 3);
    }
}