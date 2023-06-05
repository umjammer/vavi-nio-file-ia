package vavi.net.ia;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.StringUtil;

import static org.junit.jupiter.api.Assertions.fail;


public class RelationshipTests extends Base {

    @Test
    public void addRemoveList() throws Exception {
        if (config.testCollection == null || config.testCollection.isEmpty()) {
            fail("Skipping test because config.testCollection is null");
        }

        String identifier = getSharedTestIdentifier();

        var response = client.relationships.add(identifier, config.testCollection, config.testList, null);
        Assertions.assertTrue(response.success);

        var parents = client.relationships.getParents(identifier);
        Assertions.assertTrue(parents.lists.containsKey(config.testCollection), "vavi.net.ia.item not added to collection");

        var children = client.relationships.getChildren(config.testCollection, config.testList, null, null);
        Assertions.assertNotNull(children);
        // can't test this because query API is not in immediate sync with metadata API and there is no identifier to wait on
        // WaitForServer(config.testCollection)

        response = client.relationships.remove(identifier, config.testCollection, config.testList);
        Assertions.assertTrue(response.success);

        parents = client.relationships.getParents(identifier);
        Assertions.assertNotNull(parents.error); // no parent so returns error String
        Assertions.assertFalse(parents.lists.containsKey(config.testCollection), "vavi.net.ia.item not removed from collection");
    }

    @Test
    public void parentChild() throws Exception {
        var children = client.relationships.getChildren(config.testParent, null, /*rows:*/ 1, null);
Debug.println(StringUtil.paramString(children));

        var testChild = children.identifiers().stream().findFirst();
        Assertions.assertTrue(testChild.isPresent());

        var parents = client.relationships.getParents(testChild.get());
Debug.println(parents.lists);
Debug.println(config.testParent);
        Assertions.assertTrue(parents.lists.containsKey(config.testParent));
    }

    @Test
    public void getChildren() throws Exception {
        Relationships.GetChildrenResponse children = client.relationships.getChildren(config.testParent, null, /*rows:*/ 7, null);

        Assertions.assertNotNull(children);
        Assertions.assertNotNull(children.response);
        Assertions.assertNotNull(children.response.docs);
        Assertions.assertNotNull(children.response.numFound);
        Assertions.assertNotNull(children.response.start);

        Assertions.assertEquals(7, children.response.docs.size());
    }

    @Test
    public void getParents() throws Exception {
        var parents = client.relationships.getParents(config.testChild);

        Assertions.assertNotNull(parents);
        Assertions.assertNotNull(parents.lists);

        var list = parents.lists.entrySet().stream().findFirst().get();
        Assertions.assertEquals(config.testParent, list.getKey());

        Assertions.assertNotNull(list.getValue());
        Assertions.assertNotNull(list.getValue().lastChangedBy);
        Assertions.assertNotNull(list.getValue().lastChangedDate);

        Assertions.assertNotNull(list.getValue().notes);
    }
}