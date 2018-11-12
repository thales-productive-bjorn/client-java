/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.demo.consumer;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.OrchestrationClient;
import eu.arrowhead.common.api.clients.RestClient;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.*;
import eu.arrowhead.demo.model.TemperatureReadout;

import javax.ws.rs.core.Response;

class ConsumerMain extends ArrowheadApplication {

    public static void main(String[] args) {
        new ConsumerMain(args).start();
    }

    public ConsumerMain(String[] args) {
        super(args);
    }

    @Override
    protected void onStart() {
        final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
        final ArrowheadSystem me = ArrowheadSystem.createFromProperties();
        final OrchestrationClient orchestration = OrchestrationClient.createFromProperties(securityContext);

        final ServiceRequestForm srf = new ServiceRequestForm.Builder(me)
                .requestedService("temperature", "json", getProps().isSecure())
                .metadata(ServiceMetadata.Keys.UNIT, "celsius")
                .flag(OrchestrationFlags.Flags.OVERRIDE_STORE, true)
                .flag(OrchestrationFlags.Flags.PING_PROVIDERS, false)
                .flag(OrchestrationFlags.Flags.METADATA_SEARCH, true)
                .flag(OrchestrationFlags.Flags.ENABLE_INTER_CLOUD, true)
                .build();
        log.info("Service Request payload: " + Utility.toPrettyJson(null, srf));

        final RestClient restClient = orchestration.buildClient(srf);
        final Response getResponse = restClient.get().send();

        TemperatureReadout readout = new TemperatureReadout();
        try {
            readout = getResponse.readEntity(TemperatureReadout.class);
            log.info("Provider Response payload: " + Utility.toPrettyJson(null, readout));
        } catch (RuntimeException e) {
            log.error("Provider did not send the temperature readout in SenML format.", e);
        }
        if (readout.getE().get(0) == null) {
            log.error("Provider did not send any MeasurementEntry.");
        } else {
            log.info("The indoor temperature is " + readout.getE().get(0).getV() + " degrees celsius.");
        }
    }

    @Override
    protected void onStop() {

    }

}