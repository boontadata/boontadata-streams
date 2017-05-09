package io.boontadata.storm1;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.storm.Config;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.cassandra.CassandraContext;
import org.apache.storm.cassandra.query.builder.BoundCQLStatementMapperBuilder;
import org.apache.storm.cassandra.trident.state.CassandraState;
import org.apache.storm.cassandra.trident.state.CassandraStateFactory;
import org.apache.storm.cassandra.trident.state.CassandraStateUpdater;
import org.apache.storm.kafka.BrokerHosts;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.kafka.trident.OpaqueTridentKafkaSpout;
import org.apache.storm.kafka.trident.TridentKafkaConfig;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.spout.SchemeAsMultiScheme;
//import org.apache.storm.state.KeyValueState;
//import org.apache.storm.state.State;
import org.apache.storm.StormSubmitter;
//import org.apache.storm.task.OutputCollector;
//import org.apache.storm.task.TopologyContext;
//import org.apache.storm.topology.OutputFieldsDeclarer;
//import org.apache.storm.topology.TopologyBuilder;
//import org.apache.storm.topology.base.BaseStatefulWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.trident.operation.*;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.trident.windowing.InMemoryWindowsStoreFactory;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.apache.storm.windowing.TupleWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.storm.topology.base.BaseWindowedBolt.Count;
import static org.apache.storm.cassandra.DynamicStatementBuilder.*;

public class Storm1Topology implements Serializable {
    public class SplitKafkaInput extends BaseFunction { 
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

    public class DeduplicateState {
        // each Map row contains the following fields (#, type)
        // key:  msgid (String)
        // value: Tuple made of msgid (0, String), devid (1, String), devts (2, Long), cat (3, String), m1 (4, Long), m2 (5, Float)
        public HashMap<String, TridentTuple> deduplicated = new HashMap<String, TridentTuple>();
    }

    public class DeduplicateAggregator implements Aggregator<DeduplicateState> {
        public DeduplicateState init(Object batchId, TridentCollector collector) {
            return new DeduplicateState();
        }

        public void aggregate(DeduplicateState state, TridentTuple tuple, TridentCollector collector) {
            String msgid = tuple.getString(0);
            if (! state.deduplicated.containsKey(msgid)) {
                state.deduplicated.put(msgid, tuple);
            }
        }
 
        public void complete(DeduplicateState state, TridentCollector collector) {
            // emit the time window and the Tuples
            Long tw = System.currentTimeMillis();
            for(Map.Entry<String, TridentTuple> entry : state.deduplicated.entrySet()) {
                TridentTuple t = entry.getValue();
                collector.emit(new Values(tw, t.getString(0), t.getString(1), t.getLong(2), t.getString(3), t.getLong(4), t.getFloat(5)));
            }
        }

        @Override
        public void prepare(Map conf, TridentOperationContext context) {
        }

        @Override
        public void cleanup() {
        }
    }

    public class SumState {
        // each Map row contains the following fields (#, type)
        // key:  twmsgid (String)
        // value: Tuple made of tw (0, Long), devid (1, String), cat (2, String), m1 (3, Long), m2 (4, Float)
        public HashMap<String, Map> summed = new HashMap<String, Map>();
    }

    public class SumAggregator implements Aggregator<SumState> {
        public SumState init(Object batchId, TridentCollector collector) {
            return new SumState();
        }

        public void aggregate(SumState state, TridentTuple tuple, TridentCollector collector) {
            // the TridentTuple tuple has the following fields and types
            // tw (0, Long), msgid (1, String), devid (2, String), devts (3, Long), cat (4, String), m1 (5, Long), m2 (6, Float)
            String twmsgid = tuple.getLong(0).toString() + "-" + tuple.getString(1);
            if (state.summed.containsKey(twmsgid)) {
                Map map = state.summed.get(twmsgid);
                map.put(3, (Long) map.get(3) + tuple.getLong(5));
                map.put(4, (Float) map.get(4) + tuple.getFloat(6));
            } else {
                Map map = new HashMap();
                map.put(0, tuple.getLong(0));
                map.put(1, tuple.getString(2));
                map.put(2, tuple.getString(4));
                map.put(3, tuple.getLong(5));
                map.put(4, tuple.getFloat(6));
                state.summed.put(twmsgid, map);
            }
        }
 
        public void complete(SumState state, TridentCollector collector) {
            for(Map.Entry<String, Map> entry : state.summed.entrySet()) {
                Map m = entry.getValue();
                // emit tw, devid, cat, sum_m1, sum_m2
                collector.emit(new Values((String) m.get(0), (String) m.get(1), (String) m.get(2), (Long) m.get(3), (Float) m.get(4)));
            }
        }

        @Override
        public void prepare(Map conf, TridentOperationContext context) {
        }

        @Override
        public void cleanup() {
        }
    }

    public static StormTopology buildTopology() {
        Storm1Topology defaultInstance = new Storm1Topology();

        TridentTopology tridentTopology = new TridentTopology();
        BrokerHosts zk = new ZkHosts("zk1");
        TridentKafkaConfig spoutConf = new TridentKafkaConfig(zk, "sampletopic");
        spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
        OpaqueTridentKafkaSpout kafkaSpout = new OpaqueTridentKafkaSpout(spoutConf); 

        //inspired by http://storm.apache.org/releases/1.0.1/storm-cassandra.html
        CassandraState.Options options = new CassandraState.Options(new CassandraContext());
        BoundCQLStatementMapperBuilder insertValues = boundQuery(
            "INSERT INTO agg_events"
            + " (window_time, device_id, category, m1_sum_downstream, m2_sum_downstream)"
            + " VALUES (:tw, :devid, :cat, :sum_m1, :sum_m2);")
            .bind(
                field("tw").as("window_time"), 
                field("devid").as("device_id"), 
                field("cat").as("category"),
                field("sum_m1").as("m1_sum_downstream"), 
                field("sum_m2").as("m2_sum_downstream")
            ).byNamedSetters();
        options.withCQLStatementTupleMapper(insertValues.build());
        CassandraStateFactory insertValuesStateFactory =  new CassandraStateFactory(options);

        tridentTopology.newStream("kafkaSpout", kafkaSpout)
            .parallelismHint(3)
            .each(new Fields("str"), defaultInstance.new SplitKafkaInput(), new Fields("msgid", "devid", "devts", "cat", "m1", "m2"))
            .partitionBy(new Fields("msgid"))
            .tumblingWindow(new BaseWindowedBolt.Duration(5, TimeUnit.SECONDS), 
                new InMemoryWindowsStoreFactory(),
                new Fields("msgid", "devid", "devts", "cat", "m1", "m2"), 
                defaultInstance.new DeduplicateAggregator(), 
                new Fields("tw", "msgid", "devid", "devts", "cat", "m1", "m2"))
            .tumblingWindow(new BaseWindowedBolt.Duration(5, TimeUnit.SECONDS), 
                new InMemoryWindowsStoreFactory(),
                new Fields("tw", "msgid", "devid", "devts", "cat", "m1", "m2"), 
                defaultInstance.new SumAggregator(), 
                new Fields("tw", "devid", "cat", "sum_m1", "sum_m2"))
            .partitionPersist(insertValuesStateFactory, 
                new Fields("tw", "devid", "cat", "sum_m1", "sum_m2"), 
                new CassandraStateUpdater(), new Fields());
        return tridentTopology.build();
    }

    public static void main(String[] args) throws Exception {
        Config conf = new Config();
        conf.setDebug(true);
        conf.put("cassandra.keyspace", "boontadata");
        conf.put("cassandra.nodes", "cassandra1:9042,cassandra2:9042,cassandra3:9042");
        //conf.put("cassandra.port", 9042);

        String topologyName = "boontadata-storm1";
        if (args.length > 0) {
            topologyName = args[0];
        }
        conf.setNumWorkers(3);
        StormSubmitter.submitTopologyWithProgressBar(topologyName, conf, buildTopology());
    }
}