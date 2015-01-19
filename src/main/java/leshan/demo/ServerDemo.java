/*
 * Copyright (c) 2013, Sierra Wireless
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of {{ project }} nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package leshan.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import leshan.core.node.LwM2mResource;
import leshan.core.node.Value;
import leshan.core.request.ContentFormat;
import leshan.core.response.ClientResponse;
import leshan.server.LwM2mServer;
import leshan.server.californium.LeshanServerBuilder;
import leshan.server.client.Client;
import leshan.server.client.ClientRegistryListener;
import leshan.server.request.WriteRequest;

public class ServerDemo {

    // the leshan server
    private LwM2mServer lwServer;

    private final ScheduledExecutorService schedExecutor = Executors.newScheduledThreadPool(1);

    // the list of new registered clients
    private List<Client> freshRegistration = new CopyOnWriteArrayList<Client>();

    public void start() {
        // Build LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        lwServer = builder.build();

        lwServer.getClientRegistry().addListener(new ClientRegistryListener() {

            @Override
            public void registered(Client client) {
                System.out.println("New registered client with endpoint: " + client.getEndpoint());
                freshRegistration.add(client);
            }

            @Override
            public void updated(Client clientUpdated) {
                //
            }

            @Override
            public void unregistered(Client client) {
                //
            }

        });

        // start
        lwServer.start();
        schedExecutor.scheduleAtFixedRate(new TimeSynchronizer(), 5, 5, TimeUnit.SECONDS);

        System.out.println("Demo server started");
    }

    private class TimeSynchronizer implements Runnable {

        @Override
        public void run() {
            List<Client> clients = new ArrayList<Client>(freshRegistration);

            // prepare the new value
            LwM2mResource currentTimeResource = new LwM2mResource(13, Value.newDateValue(new Date()));

            // send a write request for each client
            for (Client client : clients) {
                WriteRequest writeCurrentTime = new WriteRequest(client, 3, 0, 13, currentTimeResource,
                        ContentFormat.TEXT, true);
                ClientResponse response = ServerDemo.this.lwServer.send(writeCurrentTime);
                System.out.println("Response to write request from client " + client.getEndpoint() + ": "
                        + response.getCode());
            }

            freshRegistration.removeAll(clients);
        }
    }

    public static void main(String[] args) {
        new ServerDemo().start();
    }
}