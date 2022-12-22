package vavi.net.ia;

class ServerResponseException extends RuntimeException {
    public ServerResponseException(String message) {
        super(message);
    }

    public ServerResponseException(String message, Exception ex/* = null*/) {
        super(message, ex);
    }
}

public class ServerResponse {
    public boolean Success;

    public void EnsureSuccess() throws ServerResponseException {
        if (!Success) throw new ServerResponseException("server returned success == false");
    }
}
