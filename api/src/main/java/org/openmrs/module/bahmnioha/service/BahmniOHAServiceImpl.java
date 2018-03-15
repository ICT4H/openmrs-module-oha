package org.openmrs.module.bahmnioha.service;

import com.google.gson.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bahmnioha.model.OHARequest;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.springframework.stereotype.Component;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;

@Component
public class BahmniOHAServiceImpl implements BahmniOHAService {


    private static Log log = LogFactory.getLog(BahmniOHAServiceImpl.class);
    ObsToOHARequestMapper obsToOHARequestMapper = new ObsToOHARequestMapperImpl();

    @Override
    public void calculateOHAAssesment(BahmniEncounterTransaction en) {
        System.out.println("Calculating Assessment");

        try {
            OHARequest ohaRequest = obsToOHARequestMapper.mapObsToOHARequest(en);
            String response = callOhaAPI(ohaRequest);
            createAssesment(response,en);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void createAssesment(String response, BahmniEncounterTransaction en) {
        BahmniObservation oha = obsToOHARequestMapper.find("OHA", en.getObservations(), null);
        Date obsDatetime = oha!=null? oha.getObservationDateTime()!=null?oha.getEncounterDateTime():null:null;
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(response);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject diabetesJson = jsonObject.get("diabetes").getAsJsonObject();
        JsonObject cvdAssessmentJson = jsonObject.get("cvd_assessment").getAsJsonObject();
        JsonObject highRiskConditionJson = cvdAssessmentJson.get("high_risk_condition").getAsJsonObject();
        JsonObject cvdRiskResultJson = cvdAssessmentJson.get("cvd_risk_result").getAsJsonObject();
        JsonObject guidelinesJson = cvdAssessmentJson.get("guidelines").getAsJsonObject();
        JsonObject bloodPressureJson = jsonObject.get("blood_pressure").getAsJsonObject();


        BahmniObservation ohaAssessement = createObs("OHA Assessment", null, en, obsDatetime);
        BahmniObservation diabetesObs = createObs("Diabetes", ohaAssessement, en, obsDatetime);
        BahmniObservation diabetesValue = createObs("Diabetes Value", diabetesObs, en, obsDatetime);
        BahmniObservation diabetesStatus = createObs("Diabetes Status", diabetesObs, en, obsDatetime);
        BahmniObservation diabetesCode = createObs("Diabetes Code", diabetesObs, en, obsDatetime);
        diabetesValue.setValue(((JsonPrimitive)diabetesJson.get("value")).getAsInt());

        diabetesCode.setValue((diabetesJson.get("code")).getAsString());
        if(diabetesJson.get("risk")!=null){
            BahmniObservation diabetesRisk = createObs("Diabetes Risk", diabetesObs, en, obsDatetime);
            diabetesRisk.setValue((diabetesJson.get("risk")).getAsString());
        }
        diabetesStatus.setValue((diabetesJson.get("status")).getAsBoolean());

        if(diabetesJson.get("output")!=null){
            JsonArray diabetesOutput = diabetesJson.get("output").getAsJsonArray();
            if(diabetesOutput.size()==4){
                BahmniObservation diabetesOutputObs= createObs("Output", diabetesObs, en, obsDatetime);
                BahmniObservation diabetesOutputCode= createObs("Output Code", diabetesOutputObs, en, obsDatetime);
                BahmniObservation diabetesOutputName= createObs("Output Name", diabetesOutputObs, en, obsDatetime);
                BahmniObservation diabetesOutputColor= createObs("Output Color", diabetesOutputObs, en, obsDatetime);
                BahmniObservation diabetesOutputDescription= createObs("Output Description", diabetesOutputObs, en, obsDatetime);
                diabetesOutputCode.setValue(diabetesOutput.get(0).getAsString());
                diabetesOutputName.setValue(diabetesOutput.get(1).getAsString());
                diabetesOutputColor.setValue(diabetesOutput.get(2).getAsString());
                diabetesOutputDescription.setValue(diabetesOutput.get(3).getAsString());
            }
        }
        BahmniObservation cvdAssessement = createObs("CVD Assessment", ohaAssessement, en, obsDatetime);
        BahmniObservation csdHighRiskCondition = createObs("High Risk Condition", cvdAssessement, en, obsDatetime);
        BahmniObservation csdHighRiskStatus = createObs("High Risk Condition Status", csdHighRiskCondition, en, obsDatetime);
        csdHighRiskStatus.setValue(highRiskConditionJson.get("status").getAsBoolean());

        if(!highRiskConditionJson.get("reason").isJsonNull()){
            BahmniObservation csdHighRiskReason = createObs("High Risk Condition Reason", csdHighRiskCondition, en, obsDatetime);
            csdHighRiskReason.setValue(highRiskConditionJson.get("reason").getAsString());
        }
        if(!highRiskConditionJson.get("code").getAsString().isEmpty()){
            BahmniObservation csdHighRiskCode = createObs("High Risk Condition Code", csdHighRiskCondition, en, obsDatetime);
            csdHighRiskCode.setValue(highRiskConditionJson.get("code").getAsString());
        }


        BahmniObservation cvdRiskResult = createObs("CVD Risk Result", cvdAssessement, en, obsDatetime);
        BahmniObservation cvdRisk = createObs("CVD Risk", cvdRiskResult, en, obsDatetime);
        cvdRisk.setValue(cvdRiskResultJson.get("risk").getAsInt());
        if(!cvdRiskResultJson.get("risk_range").isJsonNull() &&  !cvdRiskResultJson.get("risk_range").getAsString().isEmpty()){
            BahmniObservation cvdRiskRange = createObs("CVD Risk Range", cvdRiskResult, en, obsDatetime);
            cvdRiskRange.setValue(cvdRiskResultJson.get("risk_range").getAsString());
        }

    //setting guidelines

        BahmniObservation guidelines = createObs("Guidelines", cvdAssessement, en, obsDatetime);
        BahmniObservation label = createObs("Guidelines Label", guidelines, en, obsDatetime);
        BahmniObservation score = createObs("Guidelines Score", guidelines, en, obsDatetime);
        BahmniObservation interval = createObs("Guidelines, Follow Up Interval", guidelines, en, obsDatetime);
        BahmniObservation message = createObs("Guidelines, Follow Up Message", guidelines, en, obsDatetime);
        label.setValue(guidelinesJson.get("label").getAsString());
        score.setValue(guidelinesJson.get("score").getAsString());
        message.setValue(guidelinesJson.get("follow_up_message").getAsString());
        interval.setValue(guidelinesJson.get("follow_up_interval").getAsInt());

        //setting BP
        BahmniObservation bloodPressure = createObs("Blood Pressure", ohaAssessement, en, obsDatetime);
        BahmniObservation bp = createObs("Blood Pressure Value", bloodPressure, en, obsDatetime);
        BahmniObservation code = createObs("Blood Pressure Code", bloodPressure, en, obsDatetime);
        BahmniObservation target = createObs("Blood Pressure Target", bloodPressure, en, obsDatetime);
        code.setValue(bloodPressureJson.get("code").getAsString());
        bp.setValue(bloodPressureJson.get("bp").getAsString());
        target.setValue(bloodPressureJson.get("target").getAsString());
        //set BP output

        if(bloodPressureJson.get("output")!=null){
            JsonArray bpOutput =bloodPressureJson.get("output").getAsJsonArray();
            if(bpOutput.size()==4){
                BahmniObservation bpOutputObs= createObs("Output", bloodPressure, en, obsDatetime);
                BahmniObservation bpOutputCode= createObs("Output Code", bpOutputObs, en, obsDatetime);
                BahmniObservation bpOutputName= createObs("Output Name", bpOutputObs, en, obsDatetime);
                BahmniObservation bpOutputColor= createObs("Output Color", bpOutputObs, en, obsDatetime);
                BahmniObservation bpOutputDescription= createObs("Output Description", bpOutputObs, en, obsDatetime);
                bpOutputCode.setValue(bpOutput.get(0).getAsString());
                bpOutputName.setValue(bpOutput.get(1).getAsString());
                bpOutputColor.setValue(bpOutput.get(2).getAsString());
                bpOutputDescription.setValue(bpOutput.get(3).getAsString());
            }
        }



    }


    private String callOhaAPI(OHARequest ohaRequest) throws IOException {
        Gson gson = new Gson();
        String str=gson.toJson(ohaRequest);
        // System.out.println(str);
        URL url = new URL("http://128.199.199.111:8000/hearts");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Content-Length", "" + Integer.toString(str.getBytes().length));
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Content-Type", "application/json");

        connection.connect();

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
        wr.writeBytes (str);
        wr.flush ();
        wr.close ();
        System.out.println(connection.getResponseCode());
        InputStream response = connection.getInputStream();
        String content = new java.util.Scanner(response).useDelimiter("\\A").next();
        System.out.println(content);
        return content;
    }
     BahmniObservation createObs(String conceptName, BahmniObservation parent, BahmniEncounterTransaction encounterTransaction, Date obsDatetime) {
         Collection<BahmniObservation> observations= parent==null?encounterTransaction.getObservations():parent.getGroupMembers();
         BahmniObservation bahmniObservation = obsToOHARequestMapper.find(conceptName, observations, parent);
         if(bahmniObservation!=null){
             return  bahmniObservation;
         }
        Concept concept = Context.getConceptService().getConceptByName(conceptName);
        BahmniObservation newObservation = new BahmniObservation();
        newObservation.setConcept(new EncounterTransaction.Concept(concept.getUuid(), conceptName));
        newObservation.setObservationDateTime(obsDatetime);
       if(parent == null ){
           encounterTransaction.addObservation(newObservation);
       }else{
           parent.addGroupMember(newObservation);
       }
        return newObservation;
    }


}
