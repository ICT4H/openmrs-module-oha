package org.openmrs.module.bahmnioha.service;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bahmnioha.model.OHARequest;

import java.util.Collection;

/**
 * Created by dreddy on 14/03/18.
 */
public class ObsToOHARequestMapperImpl implements ObsToOHARequestMapper {
    @Override
    public OHARequest mapObsToOHARequest(BahmniEncounterTransaction encounterTransaction) {
        Collection<BahmniObservation> observations = encounterTransaction.getObservations();
        OHARequest ohaRequest = new OHARequest();
        setDemoGraphics(ohaRequest, encounterTransaction);
        setMeasurements(ohaRequest,observations);
        setSmoking(ohaRequest,observations);
        setDietHistory(ohaRequest,observations);
        setPathology(ohaRequest,observations);
        setOthers(ohaRequest,observations);

        return ohaRequest;
    }

    private void setDemoGraphics(OHARequest ohaRequest, BahmniEncounterTransaction encounterTransaction) {
        OHARequest.Demographics demographics = ohaRequest.new Demographics();
        Patient patient = Context.getPatientService().getPatientByUuid(encounterTransaction.getPatientUuid());
        demographics.setAge(patient.getAge());
        demographics.setGender(patient.getGender());
        ohaRequest.getData().getBody().setDemographics(demographics);
    }

    private void setOthers(OHARequest ohaRequest, Collection<BahmniObservation> observations) {
        BahmniObservation physicalActivity = find("Physical Activity\n",observations,null);
        if(hasValue(physicalActivity)){
            ohaRequest.getData().getBody().setPhysical_activity(((Double)physicalActivity.getValue()).intValue());
        }

    }

    private void setMeasurements(OHARequest ohaRequest, Collection<BahmniObservation> observations) {
        BahmniObservation height = find("Height",observations,null);
        BahmniObservation weight = find("Weight",observations,null);
        BahmniObservation waist = find("Waist",observations,null);
        BahmniObservation hip = find("Hip",observations,null);
        BahmniObservation sbp = find("Systolic BP",observations,null);
        BahmniObservation dbp = find("Diastolic BP",observations,null);
        OHARequest.Measurements measurements = ohaRequest.new Measurements();

        //TODO: add validations before using obs values
        if(hasValue(height)) {
            measurements.setHeight(new Object[]{((Number)height.getValue()).doubleValue(),"m"});
        }
        if(hasValue(weight)){
            measurements.setWeight(new Object[]{((Number)weight.getValue()).doubleValue(),"kg"});
        }
        if(hasValue(hip)) {
            measurements.setHip(new Object[]{((Number)hip.getValue()).doubleValue(), "cm"});
        }
        if(hasValue(waist)) {
            measurements.setWaist(new Object[]{((Number)waist.getValue()).doubleValue(), "cm"});
        }
        if(hasValue(sbp)) {
            measurements.setSbp(new Object[]{((Number)sbp.getValue()).doubleValue(), "sitting"});
        }
        if(hasValue(dbp)) {
            measurements.setDbp(new Object[]{((Number)dbp.getValue()).doubleValue(), "sitting"});
        }

        ohaRequest.getData().getBody().setMeasurements(measurements);
    }

    private void setSmoking(OHARequest ohaRequest, Collection<BahmniObservation> observations) {
        BahmniObservation current = find("Current",observations,null);
        BahmniObservation exSmoker = find("Ex-Smoker",observations,null);
        BahmniObservation quitWithinYear = find("Quit Within Year",observations,null);
        int currentValue=0,exvalue=0,quitValue=0;
        if(hasValue(current) && (Boolean)current.getValue()){
            currentValue=1;
        }
        if(hasValue(exSmoker) && (Boolean)exSmoker.getValue()){
            exvalue=1;
        }
        if(hasValue(quitWithinYear) && (Boolean)quitWithinYear.getValue()){
            quitValue=1;
        }

        OHARequest.Smoking smoking = ohaRequest.new Smoking(currentValue,exvalue,quitValue);
        ohaRequest.getData().getBody().setSmoking(smoking);

    }

    private void setDietHistory(OHARequest ohaRequest, Collection<BahmniObservation> observations) {
        BahmniObservation fruits = find("Fruit Servings",observations,null);
        BahmniObservation vegies = find("Vegetable Servings",observations,null);
        int fruitsValue=0,vegisValue=0;
        if(hasValue(fruits)){
            fruitsValue=((Number)fruits.getValue()).intValue();
        }
        if(hasValue(vegies)){
            vegisValue=((Number)vegies.getValue()).intValue();
        }


        OHARequest.DietHistory dietHistory = ohaRequest.new DietHistory(fruitsValue,vegisValue);
        ohaRequest.getData().getBody().setDiet_history(dietHistory);
    }

    private void setPathology(OHARequest ohaRequest, Collection<BahmniObservation> observations) {
        OHARequest.Pathology pathology = ohaRequest.new Pathology();
       //Setting BSL VALUES
        BahmniObservation bsl = find("BSL Value",observations,null);
        BahmniObservation bslType = find("BSL Type",observations,null);

        if(hasValue(bsl) ) {
            String bslTypeValue = "random";
            if(hasValue(bslType)){
                bslTypeValue = bslType.getValueAsString();
            }

            OHARequest.BSL bsl1 = ohaRequest.new BSL(bslTypeValue,((Number)bsl.getValue()).intValue());
          pathology.setBsl(bsl1);
        }

        //Setting Cholestrol Values

        BahmniObservation totalCholesterol = find("Total Cholesterol",observations,null);
        BahmniObservation cholesterolType = find("Cholesterol type",observations,null);
        BahmniObservation hdl = find("HDL",observations,null);
        BahmniObservation ldl = find("LDL",observations,null);

        if(hasValue(totalCholesterol)  &&  hasValue(cholesterolType) && hasValue(hdl) && hasValue(ldl)){
            OHARequest.Cholesterol cholesterol = ohaRequest.new Cholesterol();
            cholesterol.setTotal_chol(((Number)totalCholesterol.getValue()).doubleValue());
            cholesterol.setType(cholesterolType.getValueAsString());
            cholesterol.setHdl(((Number)hdl.getValue()).intValue());
            cholesterol.setLdl(((Number)ldl.getValue()).intValue());
            pathology.setCholesterol(cholesterol);
        }
    }

    public BahmniObservation find(String conceptName, Collection<BahmniObservation> observations, BahmniObservation parent) {
        for (BahmniObservation observation : observations) {
            if (conceptName.equalsIgnoreCase(observation.getConcept().getName())) {
                // obsParentMap.put(observation, parent);
                return observation;
            }
            BahmniObservation matchingObservation = find(conceptName, observation.getGroupMembers(), observation);
            if (matchingObservation!=null)
                return matchingObservation;
        }
        return null;
    }

    private static boolean hasValue(BahmniObservation observation) {
        return observation != null && observation.getValue() != null && !StringUtils.isEmpty(observation.getValue().toString());
    }

}
