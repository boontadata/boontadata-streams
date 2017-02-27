package io.boontadata.storm1;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.starter.bolt.PrinterBolt;
import org.apache.storm.starter.spout.RandomIntegerSpout;
import org.apache.storm.state.KeyValueState;
import org.apache.storm.state.State;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.apache.storm.windowing.TupleWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.apache.storm.topology.base.BaseWindowedBolt.Count;

/**
 * A simple example that demonstrates the usage of {@link org.apache.storm.topology.IStatefulWindowedBolt} to
 * save the state of the windowing operation to avoid re-computation in case of failures.
 * <p>
 * The framework internally manages the window boundaries and does not invoke
 * {@link org.apache.storm.topology.IWindowedBolt#execute(TupleWindow)} for the already evaluated windows in case of restarts
 * during failures. The {@link org.apache.storm.topology.IStatefulBolt#initState(State)}
 * is invoked with the previously saved state of the bolt after prepare, before the execute() method is invoked.
 * </p>
 */
public class StatefulWindowingTopology {
    private static final Logger LOG = LoggerFactory.getLogger(StatefulWindowingTopology.class);

    private static class WindowDeduplicateBolt extends 
        BaseStatefulWindowedBolt<KeyValueState<String, Tuple6<String, String, Long, String, Long, Double>>> {
        private KeyValueState<String, Tuple6<String, String, Long, String, Long, Double>> state;
        private long sum;

        private OutputCollector collector;

        @Override
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
            this.collector = collector;
        }

        @Override
        public void initState(KeyValueState<String, Long> state) {
            this.state = state;
            sum = state.get("sum", 0L);
            LOG.debug("initState with state [" + state + "] current sum [" + sum + "]");
        }

        @Override
        public void execute(TupleWindow inputWindow) {
            for (Tuple tuple : inputWindow.get()) {
                sum += tuple.getIntegerByField("value");
            }
            state.put("sum", sum);
            collector.emit(new Values(sum));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("sum"));
        }
    }

    public static void main(String[] args) throws Exception {

/*
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("kafkaSpout", new XYZ());
        builder.setBolt("deduplicateBold", new WindowDeduplicateBolt().XYZ);
        builder.setBolt("sumBolt", XYZ);
        builder.setBolt("cassandraBolt", XYZ);
        Config conf = new Config();*/

        // http://www.programcreek.com/java-api-examples/index.php?source_dir=web-crawler-master/src/jvm/storm/crawler/WebCrawlerTopology.java
        TridentTopology tridentTopology = new TridentTopology();
        BrokerHosts zk = new ZkHosts("zk1");
        TridentKafkaConfig spoutConf = new TridentKafkaConfig(zk, "sampletopic");
        spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
        OpaqueTridentKafkaSpout kafkaSpout = new OpaqueTridentKafkaSpout(spoutConf); 

        tridentTopology.newStream("kafkaSpout", kafkaSpout).parallelismHint(3)
            .each(new Fields("str"), new SplitKafkaInput(), new Fields("msgid", "devid", "devts", "cat", "m1", "m2"))
            .tumblingWindow(BaseWindowedBolt.Duration windowDuration, 
                WindowsStoreFactory windowStoreFactory,
                new Fields("msgid", "devid", "devts", "cat", "m1", "m2"), 
                deduplicateAggregator, 
                new Fields("tw", "msgid", "devid", "devts", "cat", "m1", "m2"))
            .tumblingWindow(BaseWindowedBolt.Duration windowDuration, 
                WindowsStoreFactory windowStoreFactory,
                new Fields("tw", "msgid", "devid", "devts", "cat", "m1", "m2"), 
                Aggregator sumAggregator, 
                new Fields("tw", "devid", "cat", "m1", "m2"))
            .partitionPersist(insertValuesStateFactory, 
                new Fields("tw", "devid", "cat", "sum_m1", "sum_m2"), 
                new CassandraStateUpdater(), new Fields());

        //http://storm.apache.org/releases/1.0.1/storm-cassandra.html

        CassandraState.Options options = new CassandraState.Options(new CassandraContext());
        CQLStatementTupleMapper insertTemperatureValues = boundQuery(
                "INSERT INTO weather.temperature(weather_station_id, weather_station_name, event_time, temperature) VALUES(?, ?, ?, ?)")
                .bind(with(field("weather_station_id"), field("name").as("weather_station_name"), field("event_time").now(), field("temperature")));
        options.withCQLStatementTupleMapper(insertTemperatureValues);
        CassandraStateFactory insertValuesStateFactory =  new CassandraStateFactory(options);
        TridentState selectState = topology.newStaticState(selectWeatherStationStateFactory);
        stream = stream.stateQuery(selectState, new Fields("weather_station_id"), new CassandraQuery(), new Fields("name"));
        stream = stream.each(new Fields("name"), new PrintFunction(), new Fields("name_x"));
        stream.partitionPersist(insertValuesStateFactory, new Fields("weather_station_id", "name", "event_time", "temperature"), new CassandraStateUpdater(), new Fields());


        Config conf = new Config();
        conf.setDebug(false);
        //conf.put(Config.TOPOLOGY_STATE_PROVIDER, "org.apache.storm.redis.state.RedisKeyValueStateProvider");
        if (args != null && args.length > 0) {
            conf.setNumWorkers(1);
            StormSubmitter.submitTopologyWithProgressBar(args[0], conf, tridentTopology.build());
        } else {
            LocalCluster cluster = new LocalCluster();
            StormTopology stormTopology = tridentTopology.build();
            cluster.submitTopology("boontadata_local", conf, topology);
            Utils.sleep(40000);
            cluster.killTopology("boontadata_local");
            cluster.shutdown();
        }
    }

}