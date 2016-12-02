use_cassandra=True
use_kafka=True
use_print=True
#TODO: may have to replace those directives by ___debug___ or something more optimized

if use_cassandra:
    from cassandra.cluster import Cluster
import datetime
if use_kafka:
    from kafka import KafkaProducer
import math
import numpy
import os
import pandas
import time
import uuid

randomseed=34
batchsize=300
m1max=100
m2max=500
basedelay=2*60*1000 #2 minutes
aggwindowlength=datetime.timedelta(seconds=5)

deviceid=str(uuid.uuid4())

#connect to Kafka
if use_kafka:
    producer = KafkaProducer(bootstrap_servers=os.environ['KAFKA_ADVERTISED_SERVERS'])
    if use_print:
        print("Connected a producer to Kafka servers: {}".format(os.environ['KAFKA_ADVERTISED_SERVERS']))
else:
    producer = None

#connect to Cassandra
if use_cassandra:
    cluster=Cluster(['cassandra1', 'cassandra2', 'cassandra3'])
    session=cluster.connect('boontadata')
else:
    cluster = None
    session = None

def gettimewindow(secondssinceepoch):
    dt=datetime.datetime.fromtimestamp(int(secondssinceepoch))
    return dt+aggwindowlength-datetime.timedelta(seconds=dt.second%aggwindowlength.seconds)

def senddata(messageid, deviceid, devicetime, category, measure1, measure2, sendtime, patterncode):
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
        producer.send('sampletopic', str(data).encode('utf-8'))

    #write in Cassandra
    #TODO: may have to write in Async
    #TODO: may have to replace raw_events writing to Cassandra by aggregated events writes
    if use_cassandra:
        session.execute("INSERT INTO raw_events "
            + "(message_id, device_id, device_time, send_time, category, measure1, measure2) "
            + "VALUES ('{}', '{}', {}, {}, '{}', {}, {})"
            .format(messageid, deviceid, devicetime, sendtime, category, measure1, measure2))

def sendaggdata(aggtype, aggdf):
    if use_print:
        print('aggregates of type ' + aggtype + ':')
        print(aggdf)
    
    if use_cassandra:
        for i,r in aggdf.iterrows():
            # upsert by inserting (cf http://www.planetcassandra.org/blog/how-to-do-an-upsert-in-cassandra/)
            session.execute("INSERT INTO agg_events "
                + "(window_time, device_id, category, m1_sum_{0}_{1}, m2_sum_{0}_{1}) VALUES ('{2}', '{3}', '{4}', {5}, {6})"
                .format('ingest', aggtype, 
                    str(i[0]), deviceid, i[1],
                    int(r[0]), r[1]))

def main():
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
        senddata(r.messageid, deviceid, devicetime, r.category, r.measure1, r.measure2, sendtime, patterncode)

        if r.r2 < 0.05 :
            #resend a previous message
            patterncode='re' # resend previous message
            resendindex = int(i*r.r1)
            sendtime = int(round(time.time()*1000))
            rbis = df.iloc[resendindex].copy()
            senddata(rbis.messageid, deviceid, rbis.devicetime, rbis.category, rbis.measure1, rbis.measure2, sendtime, patterncode)
            rbis.sendtime=sendtime
            rbis.patterncode=patterncode
            df.loc[iappend] = rbis
            iappend+=1

        time.sleep(r.r3/10)

    # wait for all kafka messages to be sent
    if use_kafka:
        producer.flush()

    # calculate aggregations from the sender point of view and send them to Cassandra
    df = df.drop(['r1', 'r2', 'r3'], axis=1)
    df['devicetimewindow'] = df.apply(lambda row: gettimewindow(row.devicetime/1000), axis=1)
    df['sendtimewindow'] = df.apply(lambda row: gettimewindow(row.sendtime/1000), axis=1)

    sendaggdata('devicetime',
        df
            .query('patterncode != \'re\'')
            .groupby(['devicetimewindow', 'category'])['measure1', 'measure2']
            .sum())

    sendaggdata('sendtime',
        df
            .query('patterncode != \'re\'')
            .groupby(['sendtimewindow', 'category'])['measure1', 'measure2']
            .sum())

    #disconnect from Cassandra
    if use_cassandra:
        cluster.shutdown()

if __name__ == '__main__':
    main()
