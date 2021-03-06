package sample.hello;

import java.util.TimerTask;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.model.WorkerTimeLog;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.stream.cluster.ClusterDataManager;
import rfx.core.stream.node.worker.BaseWorker;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

import com.google.gson.Gson;

public class HelloWorker extends BaseWorker {

    public HelloWorker(String name) {
        super(name);
    }

    @Override
    public void start(String host, int port) {
        Handler<HttpServerRequest> handler = new Handler<HttpServerRequest>() {

            public void handle(HttpServerRequest request) {
                if (request.absoluteURI().getPath().equals("/cmd/kill")) {
                    request.response().end("Exiting...");
                    killWorker();
                    return;
                } else if (request.absoluteURI().getPath().equals("/cmd/ping")) {
                    request.response().end("PONG");
                    return;
                }
                request.response().end("Hello");
            }
        };
        registerWorkerHttpHandler(host, port, handler);
    }

    @Override
    protected void onStartDone() {
        System.out.println("Ready to do my work!");
        ShardedJedisPool jedisPool = ClusterDataManager.getRedisClusterInfoPool();
        new RedisCommand<Boolean>(jedisPool) {

            @Override
            protected Boolean build() throws JedisException {
                jedis = shardedJedis.getShard(StringPool.BLANK);
                String workerName = StringUtil.toString(host.replaceAll("\\.", ""), "_", port);
                WorkerTimeLog timeLog = new Gson().fromJson(
                        jedis.hget(ClusterDataManager.CLUSTER_WORKER_PREFIX, workerName
                                + ClusterDataManager.WORKER_TIMELOG_POSTFIX), WorkerTimeLog.class);
                if (timeLog == null) {
                    timeLog = new WorkerTimeLog();
                }
                timeLog.addUpTime(System.currentTimeMillis());
                jedis.hset(ClusterDataManager.CLUSTER_WORKER_PREFIX, workerName
                        + ClusterDataManager.WORKER_TIMELOG_POSTFIX, new Gson().toJson(timeLog));
                return true;
            }
        }.execute();
        
        for (int i = 0; i < 30000; i++) {
            String test = "test";
            test = test + i;
        }
        
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                ClusterDataManager.updateWorkerData(host, port);
            }
        }, 2000, 2000);
    }

    public static void main(String[] args) {
        String host = args[0];
        int port = StringUtil.safeParseInt(args[1]);
        String name = host + "_" + port;

        BaseWorker worker = new HelloWorker(name);
        worker.start(host, port);
    }
}
