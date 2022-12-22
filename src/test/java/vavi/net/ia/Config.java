package vavi.net.ia;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@PropsEntity(url = "file:local.properties")
class Config {
    @Property
    public String LocalFilename = "test.txt";
    @Property
    public String RemoteFilename = "hello.txt";

    @Property
    public String TestItem = "";

    public String TestList = "";

    public String TestCollection = "";

    @Property
    public String TestParent = "";
    @Property
    public String TestChild = "";

    @Property
    public String EmailAddress = "";
    @Property
    public String AccessKey = "";
    @Property
    public String SecretKey = "";

    @Property
    public boolean CanDelete;
}
