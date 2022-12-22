package vavi.net.ia;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


public class RelationshipTests extends Base {

    @Test
    public void AddRemoveListAsync() throws Exception {
        if (_config.TestCollection == null || _config.TestCollection.isEmpty()) {
            fail("Skipping test because _config.TestCollection is null");
        }

        String identifier = GetSharedTestIdentifierAsync();

        var response = _client.Relationships.AddAsync(identifier, _config.TestCollection, _config.TestList, null);
        Assertions.assertTrue(response.Success);

        var parents = _client.Relationships.GetParentsAsync(identifier);
        Assertions.assertTrue(parents.Lists.containsKey(_config.TestCollection), "vavi.net.ia.Item not added to collection");

        var children = _client.Relationships.GetChildrenAsync(_config.TestCollection, _config.TestList, null, null);
        Assertions.assertNotNull(children);
        // can't test this because query API is not in immediate sync with metadata API and there is no identifier to wait on
        // WaitForServerAsync(_config.TestCollection)

        response = _client.Relationships.RemoveAsync(identifier, _config.TestCollection, _config.TestList);
        Assertions.assertTrue(response.Success);

        parents = _client.Relationships.GetParentsAsync(identifier);
        Assertions.assertNotNull(parents.Error); // no parent so returns error String
        Assertions.assertFalse(parents.Lists.containsKey(_config.TestCollection), "vavi.net.ia.Item not removed from collection");
    }

    @Test
    public void ParentChildAsync() throws Exception {
        var children = _client.Relationships.GetChildrenAsync(_config.TestParent, null, /*rows:*/ 1, null);

        String testChild = children.Identifiers().stream().findFirst().orElse("");
        Assertions.assertNotNull(testChild);

        var parents = _client.Relationships.GetParentsAsync(testChild);
        Assertions.assertTrue(parents.Lists.containsKey(_config.TestParent));
    }

    @Test
    public void GetChildrenAsync() throws Exception {
        Relationships.GetChildrenResponse children = _client.Relationships.GetChildrenAsync(_config.TestParent, null, /*rows:*/ 7, null);

        Assertions.assertNotNull(children);
        Assertions.assertNotNull(children.Response);
        Assertions.assertNotNull(children.Response.Docs);
        Assertions.assertNotNull(children.Response.NumFound);
        Assertions.assertNotNull(children.Response.Start);

        Assertions.assertEquals(7, children.Response.Docs.size());
    }

    @Test
    public void GetParentsAsync() throws Exception {
        var parents = _client.Relationships.GetParentsAsync(_config.TestChild);

        Assertions.assertNotNull(parents);
        Assertions.assertNotNull(parents.Lists);

        var list = parents.Lists.entrySet().stream().findFirst().get();
        Assertions.assertEquals(_config.TestParent, list.getKey());

        Assertions.assertNotNull(list.getValue());
        Assertions.assertNotNull(list.getValue().LastChangedBy);
        Assertions.assertNotNull(list.getValue().LastChangedDate);

        Assertions.assertNotNull(list.getValue().Notes);
    }
}