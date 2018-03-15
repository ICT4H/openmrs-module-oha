package org.openmrs.module.bahmnioha.service;


import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;

public interface BahmniOHAService {

    public void calculateOHAAssesment(BahmniEncounterTransaction en);

}
