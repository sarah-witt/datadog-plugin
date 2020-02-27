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

package org.datadog.jenkins.plugins.datadog.logs;

import hudson.model.Run;
import io.opentracing.Span;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;
import org.datadog.jenkins.plugins.datadog.model.BuildData;
import org.datadog.jenkins.plugins.datadog.trace.DatadogTraceCache;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class DatadogWriter {

    private static final Logger logger = Logger.getLogger(DatadogWriter.class.getName());

    private Charset charset;
    private Run<?, ?> run;

    public DatadogWriter(Run<?, ?> run, Charset charset) {
        this.charset = charset;
        this.run = run;
    }

    public Charset getCharset() {
        return charset;
    }

    public void write(String line) {
        try {
            if (!StringUtils.isNotEmpty(line)) {
                return;
            }

            JSONObject payload = new JSONObject();
            BuildData buildData = new BuildData(this.run, null);
            payload.put("ddtags", String.join(",", TagsUtil.convertTagsToArray(buildData.getTags())));
            payload = buildData.addLogAttributes(payload);
            payload.put("message", line);
            payload.put("ddsource", "jenkins");
            payload.put("service", "jenkins");
            DatadogTraceCache.AugmentedSpan augmentedSpan = DatadogTraceCache.cache.get(buildData.getBuildId(null));
            // TODO: Is this a valid way to correlate traces?
            if(augmentedSpan != null) {
                payload.put("trace_id", augmentedSpan.traceId);
                payload.put("span_id", augmentedSpan.spanId);
            }
            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();
            if(client == null){
                return;
            }
            boolean status = client.sendLogs(payload.toString());
            if(!status){
                // we try again in case a connection has to be re-established.
                client.sendLogs(payload.toString());
            }
        } catch (Exception e){
            DatadogUtilities.severe(logger, e, null);
        }
    }

}
