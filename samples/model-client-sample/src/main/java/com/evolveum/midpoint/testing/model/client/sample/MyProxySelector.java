/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.model.client.sample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Useful when redirecting HTTP communication via proxy.
 *
 * @author mederly
 */
class MyProxySelector extends ProxySelector {

    private List<Proxy> proxyList = new ArrayList<>();

    MyProxySelector(String proxyHost, int port) {
        proxyList.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port)));
    }

    @Override
    public List<Proxy> select(URI uri) {
        return proxyList;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // nothing to do here
    }
}
