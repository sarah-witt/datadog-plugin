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

package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class ConfigChangedEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        DatadogEvent event = new ConfigChangedEventImpl(null, null, null);

        String hostname = DatadogUtilities.getHostname(null);
        Assert.assertTrue(event.getHost().equals(hostname));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("unknown"));
        Assert.assertTrue(event.getTags().size() == 1);
        Assert.assertTrue(event.getTags().get("event_type").contains("system"));
        Assert.assertTrue(event.getTitle().equals("User anonymous changed file unknown"));
        Assert.assertTrue(event.getText(), event.getText().contains("User anonymous changed file unknown"));
        Assert.assertTrue(event.getText(), event.getText().contains("Host: " + hostname + ", Jenkins URL: unknown"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.WARNING));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
        Assert.assertTrue(event.getJenkinsUrl().equals("unknown"));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        File file = new File("filename");
        XmlFile xmlfile = new XmlFile(file);

        DatadogEvent event = new ConfigChangedEventImpl(null, xmlfile, new HashMap<String, Set<String>>());

        String hostname = DatadogUtilities.getHostname(null);
        Assert.assertTrue(event.getHost().equals(hostname));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("filename"));
        Assert.assertTrue(event.getTags().size() == 1);
        Assert.assertTrue(event.getTags().get("event_type").contains("system"));
        Assert.assertTrue(event.getTitle().equals("User anonymous changed file filename"));
        Assert.assertTrue(event.getText(), event.getText().contains("User anonymous changed file filename"));
        Assert.assertTrue(event.getText(), event.getText().contains("Host: " + hostname + ", Jenkins URL: unknown"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.WARNING));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
        Assert.assertTrue(event.getJenkinsUrl().equals("unknown"));

        event = new ConfigChangedEventImpl(null, xmlfile, new HashMap<String, Set<String>>());
        ((ConfigChangedEventImpl)event).setEnums("SyStEm");

        Assert.assertTrue(event.getHost().equals(hostname));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("filename"));
        Assert.assertTrue(event.getTags().size() == 1);
        Assert.assertTrue(event.getTags().get("event_type").contains("system"));
        Assert.assertTrue(event.getTitle().equals("User anonymous changed file filename"));
        Assert.assertTrue(event.getText(), event.getText().contains("User anonymous changed file filename"));
        Assert.assertTrue(event.getText(), event.getText().contains("Host: " + hostname + ", Jenkins URL: unknown"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.LOW));
        Assert.assertTrue(event.getJenkinsUrl().equals("unknown"));
    }
}
