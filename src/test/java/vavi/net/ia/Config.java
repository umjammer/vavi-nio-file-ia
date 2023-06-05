package vavi.net.ia;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@PropsEntity(url = "file:local.properties")
class Config {
    @Property
    public String localFilename = "src/test/resources/test.txt";
    @Property
    public String remoteFilename = "hello.txt";

    @Property
    public String testBucket = "";

    @Property
    public String testItem = "file:test_item.txt";

    public String testList = "";

    public String testCollection = "";

    @Property
    public String testParent = "";
    @Property
    public String testChild = "";

    @Property
    public String emailAddress = "";
    @Property
    public String accessKey = "";
    @Property
    public String secretKey = "";

    @Property
    public boolean canDelete;
}
