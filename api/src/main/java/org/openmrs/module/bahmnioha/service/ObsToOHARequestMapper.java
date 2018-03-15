package org.openmrs.module.bahmnioha.service;

import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bahmnioha.model.OHARequest;

import java.util.Collection;

/**
 * Created by dreddy on 14/03/18.
 */
public interface ObsToOHARequestMapper {
    OHARequest mapObsToOHARequest(BahmniEncounterTransaction encounterTransaction);
    public BahmniObservation find(String conceptName, Collection<BahmniObservation> observations, BahmniObservation parent);
}
