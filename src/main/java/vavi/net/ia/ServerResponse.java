package vavi.net.ia;

import java.io.IOException;


class ServerResponseException extends IOException {

    public ServerResponseException(String message) {
        super(message);
    }

    public ServerResponseException(String message, Exception ex/* = null*/) {
        super(message, ex);
    }
}

public class ServerResponse {
    public boolean success;

    public void ensureSuccess() throws IOException {
        if (!success) throw new ServerResponseException("server returned success == false");
    }
}
