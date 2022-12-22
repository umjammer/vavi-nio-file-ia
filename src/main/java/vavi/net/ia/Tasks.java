package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.annotations.JsonAdapter;


public class Tasks {

    private final String Url = "https://archive.org/services/tasks.php";
    private final String LogUrl = "https://catalogd.archive.org/services/tasks.php";

    private final Client _client;

    public Tasks(Client client) {
        _client = client;
    }

    public static class GetResponse extends ServerResponse {

        public Value_ Value;

        public static class Value_ {

            public static class Summary_ {

                public int Queued;
                public int Running;
                public int Error;
                public int Paused;
            }

            public Summary_ Summary;

            public static class HistoryEntry {

                public String Identifier;

                @JacksonXmlProperty(localName = "task_id")
                public long TaskId;

                public String Server;

                @JacksonXmlProperty(localName = "cmd")
                public String Command;

                public Map<String, String> Args = new HashMap<>();

                @JsonAdapter(LocalDateTimeNullableDeserializer.class)
                @JacksonXmlProperty(localName = "submittime")
                public LocalDateTime DateSubmitted;

                public String Submitter;
                public int Priority;

                public long Finished; // not a Unix timestamp
            }

            public List<HistoryEntry> History;
        }

        public String Cursor;
    }

    public enum SubmitTimeType {
        GreaterThan,
        GreaterThanOrEqual,
        LessThan,
        LessThanOrEqual
    }

    public static class GetRequest {

        public String Identifier;
        public Long TaskId;
        public String Server;
        public String Command;
        public String Args;
        public String Submitter;
        public Integer Priority;
        public Integer WaitAdmin;
        public SubmitTimeType SubmitTimeType;
        public OffsetDateTime SubmitTime;
        public boolean Summary = true;
        public boolean Catalog;
        public boolean History;
        public int Limit;
    }

    public GetResponse GetAsync(GetRequest request) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();

        if (request.Identifier != null) query.put("identifier", request.Identifier);
        if (request.TaskId != null) query.put("task_id", String.valueOf(request.TaskId));
        if (request.Server != null) query.put("server", request.Server);
        if (request.Command != null) query.put("cmd", request.Command);
        if (request.Args != null) query.put("args", request.Args);
        if (request.Submitter != null) query.put("submitter", request.Submitter);
        if (request.Priority != null) query.put("priority", String.valueOf(request.Priority));
        if (request.WaitAdmin != null) query.put("wait_admin", String.valueOf(request.WaitAdmin));

        String submitTimeType = null;
        switch (request.SubmitTimeType) {
        case GreaterThan:
            submitTimeType = ">";
            break;
        case GreaterThanOrEqual:
            submitTimeType = ">=";
            break;
        case LessThan:
            submitTimeType = "<";
            break;
        case LessThanOrEqual:
            submitTimeType = "<=";
            break;
        default:
            throw new IllegalArgumentException("Unexpected SubmitTimeType: " + request.SubmitTimeType);
        }

        if (request.SubmitTime != null && submitTimeType == null)
            throw new IllegalArgumentException("Must specify a SubmitTimeType");
        if (submitTimeType != null && request.SubmitTime == null)
            throw new IllegalArgumentException("Specified a SubmitTimeType but no SubmitTime");
        if (request.SubmitTime != null)
            query.put("submittime" + submitTimeType, request.SubmitTime.toString());

        if (!request.Summary) query.put("summary", "0");
        if (request.Catalog) query.put("catalog", "1");
        if (request.History) query.put("history", "1");

        GetResponse response = _client.GetAsync(Url, query, GetResponse.class);
        response.EnsureSuccess();
        return response;
    }

    public enum Command {
        BookOp,
        Backup,
        Delete,
        Derive,
        Fixer,
        MakeDark,
        MakeUndark,
        Rename
    }

    private static class SubmitRequest {

        public String Identifier;

        @JacksonXmlProperty(localName = "cmd")
        public String Command;

        public Map<String, String> Args;
        public Integer Priority;
    }

    public static class SubmitResponse extends ServerResponse {

        public Value_ Value;

        public static class Value_ {

            @JacksonXmlProperty(localName = "task_id")
            public long TaskId;

            public String Log;
        }
    }

    private static final Map<Command, String> _submitCommands = new HashMap<>() {{
        put(Command.BookOp, "book_op.php");
        put(Command.Backup, "bup.php");
        put(Command.Delete, "delete.php");
        put(Command.Derive, "derive.php");
        put(Command.Fixer, "fixer.php");
        put(Command.MakeDark, "make_dark.php");
        put(Command.MakeUndark, "make_undark.php");
        put(Command.Rename, "rename.php");
    }};

    public SubmitResponse SubmitAsync(String identifier, Command command, Map<String, String> args/*=null*/, Integer priority/*=null*/) throws IOException, InterruptedException {
        if (args == null) {
            switch (command) {
            case MakeDark, MakeUndark, Rename ->
                    throw new IllegalArgumentException(command + " requires additional arguments. See https://archive.org/services/docs/api/tasks.html and/or use a helper method");
            default -> {}
            }
        }

        SubmitRequest request = new SubmitRequest();
        request.Identifier = identifier;
        request.Command = _submitCommands.get(command);
        request.Args = args;
        request.Priority = priority;

        SubmitResponse response = _client.SendAsync("POST", Url, request, SubmitResponse.class);
        response.EnsureSuccess();
        return response;
    }

    // These tasks require additional parameters so we add helper methods to make them more discoverable
    // https://archive.org/services/docs/api/tasks.html

    public SubmitResponse RenameAsync(String identifier, String newIdentifier, Integer priority/* = null*/) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("new_identifier", newIdentifier);
        return SubmitAsync(identifier, Command.Rename, map, priority);
    }

    public SubmitResponse MakeDarkAsync(String identifier, String comment, Integer priority/* = null*/) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("new_identifier", comment);
        return SubmitAsync(identifier, Command.MakeDark, map, priority);
    }

    public SubmitResponse MakeUndarkAsync(String identifier, String comment, Integer priority/* = null*/) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("new_identifier", comment);
        return SubmitAsync(identifier, Command.MakeUndark, map, priority);
    }

    public static class RateLimitResponse extends ServerResponse {

        public Value_ Value;

        public static class Value_ {

            @JacksonXmlProperty(localName = "cmd")
            public String Command = "";

            @JacksonXmlProperty(localName = "task_limits")
            public int TaskLimits;

            @JacksonXmlProperty(localName = "tasks_inflight")
            public int TaskInFlight;

            @JacksonXmlProperty(localName = "tasks_blocked_by_offline")
            public int TasksBlockedByOffline;
        }
    }

    public RateLimitResponse GetRateLimitAsync(Command command) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        query.put("rate_limits", "1");
        query.put("cmd", command.toString());

        RateLimitResponse response = _client.<RateLimitResponse>GetAsync(Url, query, RateLimitResponse.class);
        response.EnsureSuccess();
        return response;
    }

    public static class RerunRequest {

        public String Op = "rerun";
        public long TaskId;
    }

    public static class RerunResponse extends ServerResponse {

        public Map<String, Object> Value = new HashMap<>();
    }

    public RerunResponse RerunAsync(long taskId) throws IOException, InterruptedException {
        RerunRequest request = new RerunRequest();
        request.TaskId = taskId;
        RerunResponse response = _client.<RerunResponse>SendAsync("PUT", Url, request, RerunResponse.class);
        response.EnsureSuccess();
        return response;
    }

    public enum RunState {
        Queued(0),
        Running(1),
        Error(2),
        Paused(9);
        final int v;

        RunState(int v) {
            this.v = v;
        }
    }

    public static class GetLogRequest {

        @JacksonXmlProperty(localName = "identifier")
        public String Identifier;

        @JacksonXmlProperty(localName = "cmd")
        public int Command;

        @JacksonXmlProperty(localName = "args")
        public Map<String, String> Args = new HashMap<>();

        @JacksonXmlProperty(localName = "priority")
        public int Priority = 0;
    }

    public GetLogRequest GetLogAsync(long taskId) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        query.put("task_log", String.valueOf(taskId));
        return _client.GetAsync(LogUrl, query, GetLogRequest.class);
    }
}