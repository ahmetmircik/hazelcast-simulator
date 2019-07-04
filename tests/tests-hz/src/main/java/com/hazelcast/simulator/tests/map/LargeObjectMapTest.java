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

import com.hazelcast.map.IMap;
import com.hazelcast.simulator.hz.HazelcastTest;
import com.hazelcast.simulator.test.BaseThreadState;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.TimeStep;
import com.hazelcast.simulator.tests.helpers.KeyLocality;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.waitClusterSize;
import static com.hazelcast.simulator.tests.helpers.KeyUtils.generateIntKeys;

public class LargeObjectMapTest extends HazelcastTest {

    // properties
    public int keyCount = 10000;
    public int getAllKeyCount = 1000;
    public int putAllKeyCount = 1000;
    public int hotKeyCount = 100;
    public int objLength = 1024;
    public KeyLocality keyLocality = KeyLocality.SHARED;
    public int minNumberOfMembers = 0;

    private IMap<Integer, LargeObject> map;
    private int[] keys;

    private static final Set<Integer> KEY_SET = new HashSet<Integer>();
    private static final Map<Integer, LargeObject> ENTRY_SET = new HashMap<Integer, LargeObject>();

    @Setup
    public void setUp() {
        map = targetInstance.getMap(name);
    }

    @Prepare(global = false)
    public void prepare() {
        for (int i = 0; i < getAllKeyCount; i++) {
            KEY_SET.add(i);
        }

        for (int i = 0; i < putAllKeyCount; i++) {
            ENTRY_SET.put(i, new LargeObject(objLength));
        }

        waitClusterSize(logger, targetInstance, minNumberOfMembers);
        keys = generateIntKeys(keyCount, keyLocality, targetInstance);
        Streamer<Integer, LargeObject> streamer = StreamerFactory.getInstance(map);
        for (int key : keys) {
            streamer.pushEntry(key, new LargeObject(objLength));
        }
        streamer.await();
    }

    @TimeStep(prob = -1)
    public void getAll(ThreadState state) {
        map.getAll(KEY_SET);
    }

    @TimeStep(prob = -1)
    public void putAll(ThreadState state) {
        map.putAll(ENTRY_SET);
    }

    public class ThreadState extends BaseThreadState {

        private int randomKey() {
            return keys[randomInt(keys.length)];
        }

        private int randomHotKey() {
            return keys[randomInt(hotKeyCount)];
        }

        private int randomValue() {
            return randomInt(Integer.MAX_VALUE);
        }
    }

}
