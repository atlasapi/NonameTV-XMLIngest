package com.metabroadcast.nonametv.ingest;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.webapp.health.HealthController;
import com.metabroadcast.common.webapp.health.probes.CpuProbe;
import com.metabroadcast.nonametv.ingest.process.XmlTvFileProcessor;

public class HealthModule {

    public HealthController healthController(XmlTvFileProcessor xmlTvFileProcessor) {
        return new HealthController(ImmutableList.of(new CpuProbe(), new StatusProbe(xmlTvFileProcessor)));
    }

    private static class StatusProbe implements HealthProbe {

        private XmlTvFileProcessor xmlTvFileProcessor;

        public StatusProbe(XmlTvFileProcessor xmlTvFileProcessor) {
            this.xmlTvFileProcessor = xmlTvFileProcessor;
        }

        @Override
        public ProbeResult probe() throws Exception {
            ProbeResult probeResult = new ProbeResult(title());
            if (xmlTvFileProcessor.wasLastRunSuccessful()) {
                probeResult.addInfo("last file processing", "success");
            } else {
                probeResult.addInfo("last file processing", "failure");
            }

            return probeResult;
        }

        @Override
        public String title() {
            return "Last file processing status";
        }

        @Override
        public String slug() {
            return "status";
        }

    }

}
