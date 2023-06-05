package vavi.net.ia;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class ReviewTests extends Base {

    @Test
    public void readUpdateDelete() throws Exception {
        String identifier = getSharedTestIdentifier();

        // PRACTICE: this is not good flow, it's better to separate test file creation.
        try {
            var ignore = client.reviews.get(identifier);

            // review already exists... clean up from previous test run

            client.reviews.delete(identifier);
            waitForServer(identifier, 200, 3);
        } catch (Client.HttpException ex) {
            assertEquals(404 /*NotFound*/, ex.statusCode);
        }

        // add a review

        Reviews.AddOrUpdateRequest addRequest = new Reviews.AddOrUpdateRequest();
        addRequest.title = "title Text";
        addRequest.body = "body text";
        addRequest.identifier = identifier;
        addRequest.stars = 3;

        var addResponse = client.reviews.addOrUpdate(addRequest);
        assertNotNull(addResponse);
        assertTrue(addResponse.success);
        assertNotNull(addResponse.value);
        assertNotNull(addResponse.value.taskId);
        Assertions.assertFalse(addResponse.value.reviewUpdated);

        waitForServer(identifier, 200, 3);

        var getResponse = client.reviews.get(identifier);
        assertTrue(getResponse.success);
        assertNotNull(getResponse.value);
        assertEquals(addRequest.title, getResponse.value.title);
        assertEquals(addRequest.body, getResponse.value.body);
        assertEquals(addRequest.stars, getResponse.value.stars);
        assertNotNull(getResponse.value.dateCreated);
        assertNotNull(getResponse.value.dateModified);

        // resend same review
        assertThrows(ServerResponseException.class, () -> {
            var addResponse1 = client.reviews.addOrUpdate(addRequest);
            fail("Sending same review should not succeed");
        });

        // update review with new title

        addRequest.title = "New title Text";
        var addResponse2 = client.reviews.addOrUpdate(addRequest);
        assertTrue(addResponse2.value.reviewUpdated);

        waitForServer(identifier, 200, 3);

        // verify new title

        var getResponse3 = client.reviews.get(identifier);
        assertTrue(getResponse3.success);
        assertNotNull(getResponse3.value);
        assertEquals(addRequest.title, getResponse3.value.title);

        // delete review

        var deleteResponse = client.reviews.delete(identifier);
        assertNotNull(deleteResponse);
        assertTrue(deleteResponse.success);
        assertNotNull(deleteResponse.value);
        assertNotNull(deleteResponse.value.taskId);

        waitForServer(identifier, 200, 3);

        // verify delete

        Client.HttpException ex = assertThrows(Client.HttpException.class, () -> {
            var ignore = client.reviews.get(identifier);
        }, "Failed to delete");
        assertEquals(404, ex.statusCode);
    }
}
