package com.metabroadcast.nonametv.ingest;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.webapp.health.HealthController;
import com.metabroadcast.common.webapp.health.probes.CpuProbe;

public class HealthModule {

    public HealthController healthController() {
        return new HealthController(healthProbes());
    }
    
    public List<HealthProbe> healthProbes() {
        return ImmutableList.<HealthProbe>of(new CpuProbe());
    }
}
