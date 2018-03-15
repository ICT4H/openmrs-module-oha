package org.openmrs.module.bahmnioha.rule

import org.openmrs.module.bahmnioha.domain.DosageRequest
import org.openmrs.module.bahmnioha.domain.Dose
import org.openmrs.module.bahmnioha.domain.RuleName

@RuleName(name = "testrule")
public class TestDosageRule implements DosageRule {

    public Dose calculateDose(DosageRequest request) throws Exception {
       return new Dose(request.drugName,100,Dose.DoseUnit.mg);
    }

}