package com.hazelcast.simulator.hz;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.IMap;
import com.hazelcast.instance.Node;
import com.hazelcast.internal.partition.MigrationInfo;
import com.hazelcast.internal.partition.impl.InternalMigrationListener;
import com.hazelcast.internal.partition.impl.InternalPartitionServiceImpl;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Run;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.tests.helpers.HazelcastTestUtils;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Random;

import static com.hazelcast.util.UuidUtil.newUnsecureUuidString;

public class MigrationPerfTest extends HazelcastTest {

    public int clusterSize = 5;
    public int mapCount = 10;
    public int entryCount = 10000;
    public int valueSize = 1024;

    private IMap[] maps;
    private Streamer[] streamers;

    private final MasterMigrationListener migrationListener = new MasterMigrationListener();
    private InternalPartitionServiceImpl partitionService;

    @Setup
    public void setup() {
        maps = new IMap[mapCount];
        streamers = new Streamer[mapCount];
        for (int i = 0; i < streamers.length; i++) {
            IMap<Object, Object> map = targetInstance.getMap(newUnsecureUuidString());
            maps[i] = map;
            streamers[i] = StreamerFactory.getInstance(map);
        }
    }

    @Prepare(global = true)
    public void prepare() {
        Random rand = new Random();
        for (int i = 0; i < entryCount; i++) {
            for (Streamer streamer : streamers) {
                byte[] value = new byte[valueSize];
                rand.nextBytes(value);
                streamer.pushEntry(i, value);
            }
        }
        for (Streamer streamer : streamers) {
            streamer.await();
        }
        testContext.echoCoordinator("Inserted %d entries into %d different maps.", entryCount, mapCount);

        Node node = HazelcastTestUtils.getNode(targetInstance);
        if (node.getClusterService().isMaster()) {
            partitionService = node.partitionService;
            partitionService.setInternalMigrationListener(migrationListener);
        }

        waitForCluster();
    }

    private void waitForCluster() {
        testContext.echoCoordinator("Waiting for %d members.", clusterSize);
        Cluster cluster = targetInstance.getCluster();
        while (cluster.getMembers().size() < clusterSize) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Run
    public void migrations() {
        while (!partitionService.isMemberStateSafe()) {
            testContext.echoCoordinator("Remaining migrations: %d", partitionService.getMigrationQueueSize());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        testContext.echoCoordinator("All migrations are completed.");
        try {
            PrintStream stream = new PrintStream(new File(testContext.getTestId() + "-histogram.hdr"));
            HistogramLogWriter writer = new HistogramLogWriter(stream);
            writer.outputIntervalHistogram(migrationListener.histogram);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        testContext.stop();
    }

    @Teardown
    public void tearDown() {
        for (IMap map : maps) {
            map.destroy();
        }
    }

    private class MasterMigrationListener extends InternalMigrationListener {
        // InternalMigrationListener is used by single thread on master member.
        private final Histogram histogram = new Histogram(3);
        private long startTime = -1;

        @Override
        public void onMigrationStart(MigrationParticipant participant, MigrationInfo migrationInfo) {
            if (participant != MigrationParticipant.MASTER) {
                return;
            }

            if (startTime == -1) {
                startTime = System.nanoTime();
                return;
            }

            histogram.recordValue(System.nanoTime() - startTime);
            startTime = System.nanoTime();
        }
    }
}
