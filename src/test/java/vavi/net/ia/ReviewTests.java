package vavi.net.ia;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class ReviewTests extends Base {

    @Test
    public void ReadUpdateDeleteAsync() throws Exception {
        String identifier = GetSharedTestIdentifierAsync();

        try {
            var ignore = _client.Reviews.GetAsync(identifier);

            // review already exists... clean up from previous test run

            _client.Reviews.DeleteAsync(identifier);
            WaitForServerAsync(identifier, 200, 3);
        } catch (IOException ex) {
            assertEquals(404 /*NotFound*/, Integer.parseInt(ex.getMessage())); // TODO
        }

        // add a review

        Reviews.AddOrUpdateRequest addRequest = new Reviews.AddOrUpdateRequest();
        addRequest.Title = "Title Text";
        addRequest.Body = "Body text";
        addRequest.Identifier = identifier;
        addRequest.Stars = 3;

        var addResponse = _client.Reviews.AddOrUpdateAsync(addRequest);
        assertNotNull(addResponse);
        assertTrue(addResponse.Success);
        assertNotNull(addResponse.Value);
        assertNotNull(addResponse.Value.TaskId);
        Assertions.assertFalse(addResponse.Value.ReviewUpdated);

        WaitForServerAsync(identifier, 200, 3);

        var getResponse = _client.Reviews.GetAsync(identifier);
        assertTrue(getResponse.Success);
        assertNotNull(getResponse.Value);
        assertEquals(addRequest.Title, getResponse.Value.Title);
        assertEquals(addRequest.Body, getResponse.Value.Body);
        assertEquals(addRequest.Stars, getResponse.Value.Stars);
        assertNotNull(getResponse.Value.DateCreated);
        assertNotNull(getResponse.Value.DateModified);

        // resend same review
        assertThrows(ServerResponseException.class, () -> {
            var addResponse1 = _client.Reviews.AddOrUpdateAsync(addRequest);
            fail("Sending same review should not succeed");
        });

        // update review with new title

        addRequest.Title = "New Title Text";
        var addResponse2 = _client.Reviews.AddOrUpdateAsync(addRequest);
        assertTrue(addResponse2.Value.ReviewUpdated);

        WaitForServerAsync(identifier, 200, 3);

        // verify new title

        var getResponse3 = _client.Reviews.GetAsync(identifier);
        assertTrue(getResponse3.Success);
        assertNotNull(getResponse3.Value);
        assertEquals(addRequest.Title, getResponse3.Value.Title);

        // delete review

        var deleteResponse = _client.Reviews.DeleteAsync(identifier);
        assertNotNull(deleteResponse);
        assertTrue(deleteResponse.Success);
        assertNotNull(deleteResponse.Value);
        assertNotNull(deleteResponse.Value.TaskId);

        WaitForServerAsync(identifier, 200, 3);

        // verify delete

        try {
            var ignore = _client.Reviews.GetAsync(identifier);
            fail("Failed to delete");
        } catch (IOException ex) {
            assertEquals(404, Integer.parseInt(ex.getMessage())); // TODO
        }
    }
}
