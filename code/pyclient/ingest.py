use_cassandra=True
use_kafka=True
use_print=True
#TODO: may have to replace those directives by ___debug___ or something more optimized

if use_cassandra:
    from cassandra.cluster import Cluster
import datetime
import getopt
if use_kafka:
    from kafka import KafkaProducer
import math
import numpy
import os
import pandas
import time
import uuid
import sys

def gettimewindow(secondssinceepoch, aggwindowlength):
    dt=datetime.datetime.fromtimestamp(int(secondssinceepoch))
    return dt+aggwindowlength-datetime.timedelta(seconds=dt.second%aggwindowlength.seconds)

def senddata(kproducer, csession, messageid, deviceid, devicetime, category, measure1, measure2, sendtime, patterncode):
    data="|".join([
        messageid,
        deviceid, 
        str(devicetime), 
        category,
        str(measure1),
        str(measure2)])

    if use_print:
        print(str(data), sendtime, str((sendtime-devicetime)/1000), patterncode)

    #write in Kafka
    if use_kafka:
        kproducer.send('sampletopic', str(data).encode('utf-8'))

    #write in Cassandra
    #TODO: may have to write in Async
    #TODO: may have to replace raw_events writing to Cassandra by aggregated events writes
    if use_cassandra:
        csession.execute("INSERT INTO raw_events "
            + "(message_id, device_id, device_time, send_time, category, measure1, measure2) "
            + "VALUES ('{}', '{}', {}, {}, '{}', {}, {})"
            .format(messageid, deviceid, devicetime, sendtime, category, measure1, measure2))

def sendaggdata(csession, deviceid, aggtype, aggdf):
    if use_print:
        print('aggregates of type ' + aggtype + ':')
        print(aggdf)
    
    if use_cassandra:
        for i,r in aggdf.iterrows():
            # upsert by inserting (cf http://www.planetcassandra.org/blog/how-to-do-an-upsert-in-cassandra/)
            csession.execute("INSERT INTO agg_events "
                + "(window_time, device_id, category, m1_sum_{0}_{1}, m2_sum_{0}_{1}) VALUES ('{2}', '{3}', '{4}', {5}, {6})"
                .format('ingest', aggtype, 
                    str(i[0]), deviceid, i[1],
                    int(r[0]), r[1]))

def main():
    scriptusage='ingest.py -r <random-seed> -b <batch-size>'
    randomseed=34
    batchsize=300
    m1max=100
    m2max=500
    basedelay=2*60*1000 #2 minutes
    aggwindowlength=datetime.timedelta(seconds=5)

    deviceid=str(uuid.uuid4())
    
    try:
        opts, args = getopt.getopt(sys.argv[1:],"hr:b:",["random-seed=","batch-size="])
    except getopt.GetoptError:
        print(scriptusage)
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print(scriptusage)
            sys.exit()
        elif opt in ("-r", "--random-seed"):
            randomseed = int(arg)
        elif opt in ("-b", "--batch-size"):
            batchsize = int(arg)
    
    print("randomseed={}, batchsize={}", randomseed, batchsize)

    #connect to Kafka
    if use_kafka:
        kproducer = KafkaProducer(bootstrap_servers=os.environ['KAFKA_ADVERTISED_SERVERS'])
        if use_print:
            print("Connected a producer to Kafka servers: {}".format(os.environ['KAFKA_ADVERTISED_SERVERS']))
    else:
        kproducer = None

    #connect to Cassandra
    if use_cassandra:
        ccluster=Cluster(['cassandra1', 'cassandra2', 'cassandra3'])
        csession=ccluster.connect('boontadata')
    else:
        ccluster = None
        csession = None

    numpy.random.seed(randomseed)
    df = pandas.DataFrame({
        'measure1'   : numpy.random.randint(0, m1max, batchsize),
        'm2r'  : numpy.random.rand(batchsize),
        'catr' : numpy.random.randint(1,5,batchsize),
        'r1'   : numpy.random.rand(batchsize),
        'r2'   : numpy.random.rand(batchsize),
        'r3'   : numpy.random.rand(batchsize),
        'msgid': numpy.arange(0, batchsize, 1, dtype=int),
        'devicetime'  : numpy.array([0]*batchsize, dtype=int),
        'sendtime'    : numpy.array([0]*batchsize, dtype=int),
        'patterncode' : numpy.array(['']*batchsize)
    })
    df['category'] = df.apply(lambda row: "cat-{}".format(int(row['catr'])), axis=1)
    df['measure2'] = df.apply(lambda row: row['m2r'] * m2max, axis=1)
    df['messageid'] = df.apply(lambda row: "{}-{}".format(deviceid, int(row.msgid)), axis=1)
    df = df.drop(['catr', 'm2r', 'msgid'], axis=1)

    iappend=batchsize

    for i in range(0, batchsize):
        r = df.iloc[i]
        sendtime=int(round(time.time()*1000))
        patterncode=''
        if r.r1 < 0.01 :
            # late arrival, out of order
            devicetime=int(sendtime-basedelay-int(r.r2*1000*300)) #may add up to 300 additional seconds to the base delay
            patterncode='d<s' # devicetime < sendtime
        else:
            devicetime=sendtime
        df.loc[i, 'devicetime'] = devicetime
        df.loc[i, 'sendtime'] = sendtime
        df.loc[i, 'patterncode'] = patterncode
        senddata(kproducer, csession, r.messageid, deviceid, devicetime, r.category, r.measure1, r.measure2, sendtime, patterncode)

        if r.r2 < 0.05 :
            #resend a previous message
            patterncode='re' # resend previous message
            resendindex = int(i*r.r1)
            sendtime = int(round(time.time()*1000))
            rbis = df.iloc[resendindex].copy()
            senddata(kproducer, csession, rbis.messageid, deviceid, rbis.devicetime, rbis.category, rbis.measure1, rbis.measure2, sendtime, patterncode)
            rbis.sendtime=sendtime
            rbis.patterncode=patterncode
            df.loc[iappend] = rbis
            iappend+=1

        time.sleep(r.r3/10)

    # wait for all kafka messages to be sent
    if use_kafka:
        kproducer.flush()

    # calculate aggregations from the sender point of view and send them to Cassandra
    df = df.drop(['r1', 'r2', 'r3'], axis=1)
    df['devicetimewindow'] = df.apply(lambda row: gettimewindow(row.devicetime/1000, aggwindowlength), axis=1)
    df['sendtimewindow'] = df.apply(lambda row: gettimewindow(row.sendtime/1000, aggwindowlength), axis=1)

    sendaggdata(csession, deviceid, 'devicetime',
        df
            .query('patterncode != \'re\'')
            .groupby(['devicetimewindow', 'category'])['measure1', 'measure2']
            .sum())

    sendaggdata(csession, deviceid, 'sendtime',
        df
            .query('patterncode != \'re\'')
            .groupby(['sendtimewindow', 'category'])['measure1', 'measure2']
            .sum())

    #disconnect from Cassandra
    if use_cassandra:
        ccluster.shutdown()

if __name__ == '__main__':
    main()
