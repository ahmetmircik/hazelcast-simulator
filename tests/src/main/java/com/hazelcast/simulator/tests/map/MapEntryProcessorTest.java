/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.tests.map;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.query.SqlPredicate;
import com.hazelcast.simulator.probes.Probe;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.worker.tasks.AbstractMonotonicWorkerWithProbeControl;

import java.util.Map;

public class MapEntryProcessorTest {

    // properties
    public String basename = "index-ep";
    public int keyCount = 1000;
    public int rangeStart = 0;
    public int rangeEnd = 100;

    private IMap<Integer, Integer> map;

    @Setup
    public void setUp(TestContext testContext) {
        HazelcastInstance targetInstance = testContext.getTargetInstance();
        map = targetInstance.getMap(basename);
    }

    @Teardown
    public void tearDown() {
        map.destroy();
    }

    @Warmup(global = true)
    public void warmup() {
        if (basename.indexOf("index") != -1) {
            map.addIndex("__key", true);
        }

        for (int i = 0; i < keyCount; i++) {
            map.put(i, i);
        }
    }

    @Verify
    public void verify() {
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    private class Worker extends AbstractMonotonicWorkerWithProbeControl {

        @Override
        public void timeStep(Probe probe) {
            long started = System.nanoTime();
            map.executeOnEntries(new NOOP(),
                    new SqlPredicate("__key > " + rangeStart + " AND __key < " + rangeEnd));
            probe.recordValue(System.nanoTime() - started);
        }

        @Override
        protected void afterRun() {
        }

    }

    private static final class NOOP extends AbstractEntryProcessor<Integer, Long> {


        private NOOP() {
        }

        @Override
        public Object process(Map.Entry<Integer, Long> entry) {
            return Boolean.TRUE;
        }
    }

    public static void main(String[] args) throws Exception {


        String t = "index-ep";

        int index = t.indexOf("index");
        System.out.println("index = " + index);
    }
}
