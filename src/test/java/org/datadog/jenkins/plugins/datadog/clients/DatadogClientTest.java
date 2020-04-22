/*
The MIT License

Copyright (c) 2015-Present Datadog, Inc <opensource@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package org.datadog.jenkins.plugins.datadog.clients;

import org.datadog.jenkins.plugins.datadog.DatadogClient;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;


public class DatadogClientTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testHttpClientGetInstanceApiKey() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Datadog API Key is not set properly");
        DatadogHttpClient.enableValidations = false;
        DatadogClient client = DatadogHttpClient.getInstance("http", "test", null);
        DatadogHttpClient.validateCongiguration();
    }

    @Test
    public void testHttpClientGetInstanceApiUrl() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Datadog Target URL is not set properly");
        DatadogHttpClient.enableValidations = false;
        DatadogClient client = DatadogHttpClient.getInstance("", "test", null);
        DatadogHttpClient.validateCongiguration();
    }

    @Test
    public void testHttpClientGetInstanceEnableValidations() throws IOException {
        try {
            DatadogHttpClient.enableValidations = true;
            DatadogClient client = DatadogHttpClient.getInstance("https", null, null);
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),"Datadog Target URL is not set properly");
        }
        DatadogHttpClient.enableValidations = true;
        DatadogClient newClient = DatadogHttpClient.getInstance("https", null, null);
    }

    @Test
    public void testHttpClientGetInstanceApiUrlNull() throws IOException {
        /*
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Datadog Target URL is not set properly");
        DatadogHttpClient.enableValidations = false;
        Secret secret = mock(Secret.class);
        when(Secret.fromString("test")).thenReturn(Secret("test"));

        DatadogClient client = DatadogHttpClient.getInstance(null, "test", null);
        DatadogHttpClient.validateCongiguration();
        */
    }

    @Test
    public void testHttpClientGetInstanceIntakeUrl() throws IOException {
        //TODO: Mock secret through client factory
        /*
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Datadog API Key is not set properly");
        DatadogHttpClient.enableValidations = false;
        Secret apiKey = Secret.fromString(null);
        DatadogClient client = DatadogHttpClient.getInstance("http", null, apiKey);
        DatadogHttpClient.validateCongiguration();
        */
    }

    @Test
    public void testDogstatsDClientGetInstanceTargetUrl() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Datadog Target Port is not set properly");
        DogStatsDClient.enableValidations = false;
        DatadogClient client = DogStatsDClient.getInstance("test", null, null);
        DogStatsDClient.validateCongiguration();
    }

    @Test
    public void testDogstatsDClientGetInstanceEnableValidations() throws IOException {
        try {
            DogStatsDClient.enableValidations = true;
            DatadogClient client = DogStatsDClient.getInstance("https", null, null);
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),"Datadog Target URL is not set properly");
        }
        DogStatsDClient.enableValidations = true;
        DatadogClient newClient = DogStatsDClient.getInstance("https", null, null);
    }

    @Test
    public void testDogstatsDClientGetInstanceTargetLogPort() throws IOException {
        DogStatsDClient.enableValidations = false;
        DatadogClient client = DogStatsDClient.getInstance("https", 8000, null);
        DogStatsDClient.validateCongiguration();
        Assert.assertTrue(true);
    }

    @Test
    public void testIncrementCountAndFlush() throws IOException, InterruptedException {
        DatadogHttpClient.enableValidations = false;
        DatadogClient client = DatadogHttpClient.getInstance("test", null, null);
        Map<String, Set<String>> tags1 = new HashMap<>();
        tags1 = DatadogClientStub.addTagToMap(tags1, "tag1", "value");
        tags1 = DatadogClientStub.addTagToMap(tags1, "tag2", "value");
        client.incrementCounter("metric1", "host1", tags1);
        client.incrementCounter("metric1", "host1", tags1);

        Map<String, Set<String>> tags2 = new HashMap<>();
        tags2 = DatadogClientStub.addTagToMap(tags2, "tag1", "value");
        tags2 = DatadogClientStub.addTagToMap(tags2, "tag2", "value");
        tags2 = DatadogClientStub.addTagToMap(tags2, "tag3", "value");
        client.incrementCounter("metric1", "host1", tags2);

        client.incrementCounter("metric1", "host2", tags2);
        client.incrementCounter("metric1", "host2", tags2);

        client.incrementCounter("metric2", "host2", tags2);

        // The following code should be the same as in the flushCounters method
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> countersEmpty = ConcurrentMetricCounters.getInstance().getAndReset();
        Assert.assertTrue("size = " + countersEmpty.size(), countersEmpty.size() == 0);

        // Check that metrics to submit are correct
        boolean check1  = false, check2 = false, check3 = false, check4 = false;
        Assert.assertTrue("counters = " + counters.size(), counters.size() == 4);
        for (CounterMetric counterMetric: counters.keySet()) {
            int count = counters.get(counterMetric);
            if(counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host1")
                    && counterMetric.getTags().size() == 2){
                Assert.assertTrue("count = " + count, count == 2);
                check1 = true;
            } else if (counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host1")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue("count = " + count,count == 1);
                check2 = true;
            } else if (counterMetric.getMetricName().equals("metric1") && counterMetric.getHostname().equals("host2")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue("count = " + count,count == 2);
                check3 = true;
            } else if (counterMetric.getMetricName().equals("metric2") && counterMetric.getHostname().equals("host2")
                    && counterMetric.getTags().size() == 3){
                Assert.assertTrue("count = " + count,count == 1);
                check4 = true;
            }
        }
        Assert.assertTrue(check1 + " " + check2 + " " + check3 + " " + check4,
                check1 && check2 && check3 && check4);
    }

    @Test
    public void testIncrementCountAndFlushThreadedEnv() throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable increment = new Runnable() {
            @Override
            public void run() {
                // We use a new instance of a client on every run.
                DatadogHttpClient.enableValidations = false;
                DatadogClient client = DatadogHttpClient.getInstance("test", null, null);
                Map<String, Set<String>> tags = new HashMap<>();
                tags = DatadogClientStub.addTagToMap(tags, "tag1", "value");
                tags = DatadogClientStub.addTagToMap(tags, "tag2", "value");
                client.incrementCounter("metric1", "host1", tags);
            }
        };

        for(int i = 0; i < 10000; i++){
            executor.submit(increment);
        }

        stop(executor);

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();
        Assert.assertTrue("size = " + counters.size(), counters.size() == 1);
        Assert.assertTrue("counters.values() = " + counters.values(), counters.values().contains(10000));

    }

    @Test
    public void testIncrementCountAndFlushThreadedEnvThreadCheck() throws IOException, InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable increment = new Runnable() {
            @Override
            public void run() {
                // We use a new instance of a client on every run.
                DatadogHttpClient.enableValidations = false;
                DatadogClient client = DatadogHttpClient.getInstance("test", null, null);
                Map<String, Set<String>> tags = new HashMap<>();
                tags = DatadogClientStub.addTagToMap(tags, "tag1", "value");
                tags = DatadogClientStub.addTagToMap(tags, "tag2", "value");
                client.incrementCounter("metric1", "host1", tags);
            }
        };

        for(int i = 0; i < 10000; i++){
            executor.submit(increment);
        }

        stop(executor);

        // We also check the result in a distinct thread
        ExecutorService single = Executors.newSingleThreadExecutor();
        Callable<Boolean> check = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Check counter is reset as expected
                ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();
                Assert.assertTrue("size = " + counters.size(), counters.size() == 1);
                Assert.assertTrue("counters.values() = " + counters.values(), counters.values().contains(10000));
                return true;
            }
        };

        Future<Boolean> value = single.submit(check);

        stop(single);

        Assert.assertTrue(value.get());

    }

    @Test
    public void testIncrementCountAndFlushThreadedEnvOneClient() throws IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // We only have one instance of the client used by all threads
        DatadogHttpClient.enableValidations = false;
        final DatadogClient client = DatadogHttpClient.getInstance("test", null, null);
        Runnable increment = new Runnable() {
            @Override
            public void run() {
                Map<String, Set<String>> tags = new HashMap<>();
                tags = DatadogClientStub.addTagToMap(tags, "tag1", "value");
                tags = DatadogClientStub.addTagToMap(tags, "tag2", "value");
                client.incrementCounter("metric1", "host1", tags);
            }
        };

        for(int i = 0; i < 10000; i++){
            executor.submit(increment);
        }

        stop(executor);

        // Check counter is reset as expected
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();
        Assert.assertTrue("size = " + counters.size(), counters.size() == 1);
        Assert.assertTrue("counters.values() = " + counters.values(), counters.values().contains(10000));

    }

    private static void stop(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("termination interrupted");
        }
        finally {
            if (!executor.isTerminated()) {
                System.err.println("killing non-finished tasks");
            }
            executor.shutdownNow();
        }
    }

}
