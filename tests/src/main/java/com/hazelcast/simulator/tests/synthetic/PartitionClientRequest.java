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

package com.hazelcast.simulator.tests.synthetic;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.spi.Operation;

import java.io.IOException;
import java.security.Permission;

public abstract class PartitionClientRequest implements Portable {

    public abstract Operation prepareOperation();

    public abstract int getPartition();

    public String getServiceName() {
        return null;
    }

    public int getFactoryId() {
        return SyntheticRequestPortableFactory.FACTORY_ID;
    }

    public int getClassId() {
        return 1;
    }

    public Permission getRequiredPermission() {
        return null;
    }

    public void write(PortableWriter writer) throws IOException {

    }
    public void read(PortableReader reader) throws IOException {

    }
}
