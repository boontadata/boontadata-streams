package io.boontadata.storm1;

import java.util.Map;
import org.apache.storm.Config;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.cassandra.CassandraContext;
import org.apache.storm.cassandra.query.CQLStatementTupleMapper;
import org.apache.storm.cassandra.trident.state.CassandraState;
import org.apache.storm.cassandra.trident.state.CassandraStateFactory;
import org.apache.storm.kafka.BrokerHosts;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.kafka.trident.OpaqueTridentKafkaSpout;
import org.apache.storm.kafka.trident.TridentKafkaConfig;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.state.KeyValueState;
import org.apache.storm.state.State;
import org.apache.storm.StormSubmitter;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.trident.operation.Aggregator;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.apache.storm.windowing.TupleWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.storm.topology.base.BaseWindowedBolt.Count;
import static org.apache.storm.cassandra.DynamicStatementBuilder.*;

public class Storm1Topology {
    /*
    private static class SumAggregator implements Aggregator<Map<Tuple2<Long, String>, Tuple4<String, Long, String, Long, Float>>> {
        
        public Map<Tuple2<Long, String>, Tuple4<String, Long, String, Long, Float>> init(object batchId, TridentCollector collector) {
            return new HashMap<Tuple2<Long, String>, Tuple4<String, Long, String, Long, Float>>();
        }

        public void aggregate(Map<Tuple2<Long, String>, Tuple4<String, Long, String, Long, Float>> val, TridentTuple tuple, TridentCollector collector) {
            //
        }

        public void complete(Map<Tuple2<Long, String>, Tuple4<String, Long, String, Long, Float>> val, TridentCollector collector) {
            //
            
            //How can I find the window time in order to emit tw field? 
            // asked the question on Stack Overflow: http://stackoverflow.com/questions/42488607/how-to-retrieve-current-window-time-in-apache-storm-trident
            // in the mean time, the txid can be used instead of time window.
        }

        public void cleanup() {
        }
    }

    private static class DeduplicateAggregator implements Aggregator<> {
        // TODO
    }
    */

    public class SplitKafkaInput { 
        @Override 
        public void execute(TridentTuple tridentTuple, TridentCollector tridentCollector) { 
            String kafkaInput = tridentTuple.getString(0); 
    
            String url; 
            Integer depth; 
    
            String[] tokens = kafkaInput.split(" "); 
            if(tokens.length == 2){ 
                url = tokens[0]; 
                depth = Integer.parseInt(tokens[1]); 
            } else if (tokens.length == 1){ 
                url = tokens[0]; 
                depth = new Integer(0); 
            } else { 
                return; 
            } 
            tridentCollector.emit(new Values(url, depth) ); 
        } 
    }

    public static StormTopology buildTopology() {
        TridentTopology tridentTopology = new TridentTopology();
        BrokerHosts zk = new ZkHosts("zk1");
        TridentKafkaConfig spoutConf = new TridentKafkaConfig(zk, "sampletopic");
        spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
        OpaqueTridentKafkaSpout kafkaSpout = new OpaqueTridentKafkaSpout(spoutConf); 

        //inspired by http://storm.apache.org/releases/1.0.1/storm-cassandra.html
        CassandraState.Options options = new CassandraState.Options(new CassandraContext());
        CQLStatementTupleMapper insertValues = boundQuery(
            "INSERT INTO boontadata.agg_events"
            + " (window_time, device_id, category, m1_sum_downstream, m2_sum_downstream)"
            + " VALUES (?, ?, ?, ?, ?);")
            .bind(
                with(
                    field("tw").as("window_time"), 
                    field("devid").as("device_id"), 
                    field("cat").as("category"),
                    field("sum_m1").as("m1_sum_downstream"), 
                    field("sum_m2").as("m2_sum_downstream")
                )
            );
        options.withCQLStatementTupleMapper(insertValues);
        CassandraStateFactory insertValuesStateFactory =  new CassandraStateFactory(options);

        tridentTopology.newStream("kafkaSpout", kafkaSpout)
            .parallelismHint(3)
            .each(new Fields("str"), new SplitKafkaInput(), new Fields("msgid", "devid", "devts", "cat", "m1", "m2"))
            .partitionBy(new Fields("msgid"))
            .tumblingWindow(new BaseWindowedBolt.Duration(5, TimeUnit.SECONDS), 
                new InMemoryWindowsStoreFactory(),
                new Fields("msgid", "devid", "devts", "cat", "m1", "m2"), 
                new DeduplicateAggregator(), 
                new Fields("tw", "msgid", "devid", "devts", "cat", "m1", "m2"))
            .withWatermarkInterval(new BaseWindowedBolt.Duration(1, TimeUnit.SECONDS))
            .tumblingWindow(new BaseWindowedBolt.Duration(5, TimeUnit.SECONDS), 
                new InMemoryWindowsStoreFactory(),
                new Fields("tw", "msgid", "devid", "devts", "cat", "m1", "m2"), 
                new SumAggregator(), 
                new Fields("tw", "devid", "cat", "m1", "m2"))
            .withWatermarkInterval(new BaseWindowedBolt.Duration(1, TimeUnit.SECONDS))
            .partitionPersist(insertValuesStateFactory, 
                new Fields("tw", "devid", "cat", "sum_m1", "sum_m2"), 
                new CassandraStateUpdater(), new Fields());
        return tridentTopology.build();
    }

    public static void main(String[] args) throws Exception {
        Config conf = new Config();
        conf.setDebug(true);
        String topologyName = "boontadata-storm1";
        if (args.length > 0) {
            topologyName = args[0];
        }
        conf.setNumWorkers(3);
        StormSubmitter.submitTopologyWithProgressBar(topologyName, conf, buildTopology());
    }
}