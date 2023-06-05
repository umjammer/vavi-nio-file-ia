package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;


/**
 * @see "https://archive.org/developers/tasks.html"
 */
public class Tasks {

    private final String url = "https://archive.org/services/tasks.php";
    private final String logUrl = "https://catalogd.archive.org/services/tasks.php";

    private final Client client;

    public Tasks(Client client) {
        this.client = client;
    }

    public static class GetResponse extends ServerResponse {

        public Value_ value;

        public static class Value_ {

            public static class Summary_ {

                public Integer queued;
                public Integer running;
                public Integer error;
                public Integer paused;
            }

            public Summary_ summary;

            public static class HistoryEntry {

                public String identifier;

                @SerializedName("task_id")
                public Long taskId;

                public String server;

                @SerializedName("cmd")
                public String command;

                public Map<String, String> args = new HashMap<>();

                @SerializedName("submittime")
                public LocalDateTime dateSubmitted;

                public String submitter;
                public Integer priority;

                public Long finished; // not a Unix timestamp
            }

            public List<HistoryEntry> history;
        }

        public String cursor;
    }

    public enum SubmitTimeType {
        GreaterThan,
        GreaterThanOrEqual,
        LessThan,
        LessThanOrEqual
    }

    public static class GetRequest {

        public String identifier;
        public Long taskId;
        public String server;
        public String command;
        public String args;
        public String submitter;
        public Integer priority;
        public Integer waitAdmin;
        public SubmitTimeType submitTimeType;
        public LocalDateTime submitTime;
        public boolean summary = true;
        public boolean catalog;
        public boolean history;
        public int limit;
    }

    public GetResponse get(GetRequest request) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();

        if (request.identifier != null) query.put("identifier", request.identifier);
        if (request.taskId != null) query.put("task_id", String.valueOf(request.taskId));
        if (request.server != null) query.put("server", request.server);
        if (request.command != null) query.put("cmd", request.command);
        if (request.args != null) query.put("args", request.args);
        if (request.submitter != null) query.put("submitter", request.submitter);
        if (request.priority != null) query.put("priority", String.valueOf(request.priority));
        if (request.waitAdmin != null) query.put("wait_admin", String.valueOf(request.waitAdmin));

        String submitTimeType;
        if (request.submitTimeType != null) {
            submitTimeType = switch (request.submitTimeType) {
                case GreaterThan -> ">";
                case GreaterThanOrEqual -> ">=";
                case LessThan -> "<";
                case LessThanOrEqual -> "<=";
                default -> throw new IllegalArgumentException("Unexpected submitTimeType: " + request.submitTimeType);
            };
        } else {
            submitTimeType = null;
        }

        if (request.submitTime != null && submitTimeType == null)
            throw new IllegalArgumentException("Must specify a submitTimeType");
        if (submitTimeType != null && request.submitTime == null)
            throw new IllegalArgumentException("Specified a submitTimeType but no submitTime");
        if (request.submitTime != null)
            query.put("submittime" + submitTimeType, request.submitTime.toString());

        if (!request.summary) query.put("summary", "0");
        if (request.catalog) query.put("catalog", "1");
        if (request.history) query.put("history", "1");

        GetResponse response = client.get(url, query, GetResponse.class);
        response.ensureSuccess();
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

    /** @see "https://archive.org/developers/tasks.html#request-entity" */
    private static class SubmitRequest {

        public String identifier;

        @SerializedName("cmd")
        public String command;

        public Map<String, String> args;
        public Integer priority;
    }

    public static class SubmitResponse extends ServerResponse {

        public Value_ value;

        public static class Value_ {

            @SerializedName("task_id")
            public Long taskId;

            public String log;
        }
    }

    private static final Map<Command, String> submitCommands = new HashMap<>() {{
        put(Command.BookOp, "book_op.php");
        put(Command.Backup, "bup.php");
        put(Command.Delete, "delete.php");
        put(Command.Derive, "derive.php");
        put(Command.Fixer, "fixer.php");
        put(Command.MakeDark, "make_dark.php");
        put(Command.MakeUndark, "make_undark.php");
        put(Command.Rename, "rename.php");
    }};

    public SubmitResponse submit(String identifier, Command command, Map<String, String> args/*=null*/, Integer priority/*=null*/) throws IOException, InterruptedException {
        if (args == null) {
            switch (command) {
            case MakeDark, MakeUndark, Rename ->
                    throw new IllegalArgumentException(command + " requires additional arguments. See https://archive.org/services/docs/api/tasks.html and/or use a helper method");
            default -> {}
            }
        }

        SubmitRequest request = new SubmitRequest();
        request.identifier = identifier;
        request.command = submitCommands.get(command);
        request.args = args;
        request.priority = priority;

        SubmitResponse response = client.send("POST", url, request, SubmitResponse.class);
        response.ensureSuccess();
        return response;
    }

    // These tasks require additional parameters so we add helper methods to make them more discoverable
    // https://archive.org/services/docs/api/tasks.html

    public SubmitResponse rename(String identifier, String newIdentifier, Integer priority/* = null*/) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("new_identifier", newIdentifier);
        return submit(identifier, Command.Rename, map, priority);
    }

    public SubmitResponse makeDark(String identifier, String comment, Integer priority/* = null*/) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("comment", comment);
        return submit(identifier, Command.MakeDark, map, priority);
    }

    public SubmitResponse makeUndark(String identifier, String comment, Integer priority/* = null*/) throws IOException, InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put("comment", comment);
        return submit(identifier, Command.MakeUndark, map, priority);
    }

    public static class RateLimitResponse extends ServerResponse {

        public Value_ value;

        public static class Value_ {

            @SerializedName("cmd")
            public String command = "";

            @SerializedName("task_limits")
            public int taskLimits;

            @SerializedName("tasks_inflight")
            public int taskInFlight;

            @SerializedName("tasks_blocked_by_offline")
            public int tasksBlockedByOffline;
        }
    }

    public RateLimitResponse getRateLimit(Command command) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        query.put("rate_limits", "1");
        query.put("cmd", command.toString());

        RateLimitResponse response = client.get(url, query, RateLimitResponse.class);
        response.ensureSuccess();
        return response;
    }

    public static class RerunRequest {

        public String op = "rerun";
        public long taskId;
    }

    public static class RerunResponse extends ServerResponse {

        public Map<String, Object> value = new HashMap<>();
    }

    public RerunResponse rerun_(long taskId) throws IOException, InterruptedException {
        RerunRequest request = new RerunRequest();
        request.taskId = taskId;
        RerunResponse response = client.send("PUT", url, request, RerunResponse.class);
        response.ensureSuccess();
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

        @SerializedName("identifier")
        public String identifier;

        @SerializedName("cmd")
        public int command;

        @SerializedName("args")
        public Map<String, String> args = new HashMap<>();

        @SerializedName("priority")
        public int priority = 0;
    }

    public GetLogRequest getLog(long taskId) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        query.put("task_log", String.valueOf(taskId));
        return client.get(logUrl, query, GetLogRequest.class);
    }
}