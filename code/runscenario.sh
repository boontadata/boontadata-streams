#!/bin/bash

if test $# -eq 0
then
    command=$0
    echo "usage: $command <scenario>"
    echo "<scenario> can be flink, truncate_cassandra_data, ..."
    return 0
fi

scenario=$1
echo "will start scenario $scenario"

tellandwaitnsecs()
{
    nbofseconds=$1
    echo "will wait for $nbofseconds seconds"
    sleep $nbofseconds
}

scenario_flink()
{
    timecharacterictic=$1
    echo "starting Flink scenario with a time characteristic of $timecharacterictic"

    echo "Initial content in the Cassandra database"
    docker exec -ti cassandra3 cqlsh --execute "use boontadata; select count(*) from debug; select count(*) from raw_events; select count(*) from agg_events;"

    echo "start Flink job"
    docker exec -ti flink-master flink run -c io.boontadata.flink1.StreamingJob --time.characteristic=$timecharacterictic /workdir/flink1-0.1.jar -d &
    tellandwaitnsecs 15
    docker exec -ti flink-master flink list

    echo "inject data"
    docker exec -ti client1 python /workdir/ingest.py

    echo "wait for Flink to finish ingesting"
    tellandwaitnsecs 10

    echo "get the result"
    docker exec -ti client1 python /workdir/compare.py

    tellandwaitnsecs 5

    echo "add 1 event later, then get the result again"
    docker exec -ti client1 python /workdir/ingest.py --batch-size 1
    tellandwaitnsecs 10
    docker exec -ti client1 python /workdir/compare.py

    echo "kill the Flink job"
    flinkjobid=`docker exec -ti flink-master flink list | grep io.boontadata.flink1.StreamingJob | awk '{print $4}'`
    echo "Flink job id is $flinkjobid"
    docker exec -ti flink-master flink cancel $flinkjobid
    docker exec -ti flink-master flink list
}

scenario_truncate_cassandra_data()
{
    echo "Initial content in the Cassandra database"
    docker exec -ti cassandra3 cqlsh --execute "use boontadata; select count(*) from debug; select count(*) from raw_events; select count(*) from agg_events;"

    echo "truncate"
    docker exec -ti cassandra3 cqlsh --execute "use boontadata; truncate table debug; truncate table raw_events; truncate table agg_events;"

    echo "new content in the Cassandra database"
    docker exec -ti cassandra3 cqlsh --execute "use boontadata; select count(*) from debug; select count(*) from raw_events; select count(*) from agg_events;"
    
}

case $scenario in
    flink1)
        scenario_flink EventTime
        ;;
    flink2)
        scenario_flink ProcessingTime
        ;;
    truncate_cassandra_data)
        scenario_truncate_cassandra_data
        ;;
    *)
        echo "scenario $scenario is not implemented (yet?)."
esac

return 0
