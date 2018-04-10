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

import javax.net.ssl.HttpsURLConnection;
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
            if (null != response && response.length() > 0) {
                createAssesment(response,en);
            }

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
        JsonObject cvdRiskResultJson = (JsonObject) cvdAssessmentJson.get("cvd_risk_result");
        JsonObject guidelinesJson = cvdAssessmentJson.get("guidelines").getAsJsonObject();
        JsonObject bloodPressureJson = jsonObject.get("blood_pressure").getAsJsonObject();
        JsonObject lifestyleJson = jsonObject.get("lifestyle").getAsJsonObject();


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

        createObsForOutput(diabetesJson,diabetesObs,en,obsDatetime);


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


        if(cvdRiskResultJson!=null) {
            BahmniObservation cvdRiskResult = createObs("CVD Risk Result", cvdAssessement, en, obsDatetime);
            BahmniObservation cvdRisk = createObs("CVD Risk", cvdRiskResult, en, obsDatetime);
            cvdRisk.setValue(cvdRiskResultJson.get("risk").getAsInt());
            if (!cvdRiskResultJson.get("risk_range").isJsonNull() && !cvdRiskResultJson.get("risk_range").getAsString().isEmpty()) {
                BahmniObservation cvdRiskRange = createObs("CVD Risk Range", cvdRiskResult, en, obsDatetime);
                cvdRiskRange.setValue(cvdRiskResultJson.get("risk_range").getAsString());
            }
        }

    //setting guidelines

        BahmniObservation guidelines = createObs("Guidelines", cvdAssessement, en, obsDatetime);

        if(guidelinesJson.get("label")!=null) {
            BahmniObservation label = createObs("Guidelines Label", guidelines, en, obsDatetime);
            label.setValue(guidelinesJson.get("label").getAsString());
        }
        if(guidelinesJson.get("score")!=null) {
            BahmniObservation score = createObs("Guidelines Score", guidelines, en, obsDatetime);
            score.setValue(guidelinesJson.get("score").getAsString());
        }
        if(guidelinesJson.get("follow_up_message")!=null) {
            BahmniObservation message = createObs("Guidelines, Follow Up Message", guidelines, en, obsDatetime);
            message.setValue(guidelinesJson.get("follow_up_message").getAsString());
        }
        if(guidelinesJson.get("follow_up_interval")!=null) {
            BahmniObservation interval = createObs("Guidelines, Follow Up Interval", guidelines, en, obsDatetime);
            interval.setValue(guidelinesJson.get("follow_up_interval").getAsInt());
        }


        JsonObject managementJson = (JsonObject) guidelinesJson.get("management");

        if(managementJson != null) {
            BahmniObservation management = createObs("Management", guidelines, en, obsDatetime);

            if(managementJson.get("lifestyle")!=null) {
                BahmniObservation lifeStyle = createObs("Lifestyle Management", management, en, obsDatetime);
                lifeStyle.setValue(managementJson.get("lifestyle").getAsString());
            }

            if(managementJson.get("lipids")!=null) {
                BahmniObservation lipids = createObs("Lipids Management", management, en, obsDatetime);
                lipids.setValue(managementJson.get("lipids").getAsString());
            }

            if(managementJson.get("refer")!=null) {
                BahmniObservation referral = createObs("Referral", management, en, obsDatetime);
                referral.setValue(managementJson.get("refer").getAsString());
            }

            if(managementJson.get("blood-pressure")!=null) {
                BahmniObservation bpManagement = createObs("BP Management", management, en, obsDatetime);
                bpManagement.setValue(managementJson.get("blood-pressure").getAsString());
            }

        }


        //setting BP
        BahmniObservation bloodPressure = createObs("Blood Pressure", ohaAssessement, en, obsDatetime);
        BahmniObservation bp = createObs("Blood Pressure Value", bloodPressure, en, obsDatetime);
        BahmniObservation code = createObs("Blood Pressure Code", bloodPressure, en, obsDatetime);
        BahmniObservation target = createObs("Blood Pressure Target", bloodPressure, en, obsDatetime);
        code.setValue(bloodPressureJson.get("code").getAsString());
        bp.setValue(bloodPressureJson.get("bp").getAsString());
        target.setValue(bloodPressureJson.get("target").getAsString());
        //set BP output

        createObsForOutput(bloodPressureJson,bloodPressure,en,obsDatetime);

        //setting lifestyle
        BahmniObservation lifestyleObs = createObs("Lifestyle", ohaAssessement, en, obsDatetime);
        JsonObject bmiJson = lifestyleJson.get("bmi").getAsJsonObject();
        createLifeStyleObs("BMI",lifestyleObs,en,obsDatetime,bmiJson);

        JsonObject whrJson = lifestyleJson.get("whr").getAsJsonObject();
        createLifeStyleObs("WHR",lifestyleObs,en,obsDatetime,whrJson);

        JsonObject exerciseJson = lifestyleJson.get("exercise").getAsJsonObject();
        createLifeStyleObs("Exercise",lifestyleObs,en,obsDatetime,exerciseJson);

        JsonObject smokingJson = lifestyleJson.get("smoking").getAsJsonObject();
        createLifeStyleObs("Smoking",lifestyleObs,en,obsDatetime,smokingJson);

        JsonObject dietJson = lifestyleJson.get("diet").getAsJsonObject();

        if(dietJson!=null && !dietJson.isJsonNull() ){
            BahmniObservation dietObs = createObs("Diet", lifestyleObs, en, obsDatetime);

            JsonElement dietCode = dietJson.get("code");
            if(dietCode!=null && !dietCode.getAsString().isEmpty()){
                BahmniObservation dietCodeObs = createObs("Diet Code", dietObs, en, obsDatetime);
                dietCodeObs.setValue(dietCode.getAsString());

            }
            createObsForOutput(dietJson,dietObs,en,obsDatetime);
        }
    }

    public void createLifeStyleObs(String lifeStyleConceptName, BahmniObservation lifestyleObs, BahmniEncounterTransaction en, Date obsDatetime, JsonObject jsonObject1){
       // = "BMI";
        BahmniObservation bmiObs = createObs(lifeStyleConceptName, lifestyleObs, en, obsDatetime);
        BahmniObservation bmiTarget = createObs(lifeStyleConceptName+" Target", bmiObs, en, obsDatetime);

        JsonElement valueJson = jsonObject1.get("value");
        if(null!=valueJson && !valueJson.isJsonNull() && !valueJson.getAsString().isEmpty()){
            BahmniObservation value = createObs(lifeStyleConceptName+" Value", bmiObs, en, obsDatetime);
            value.setValue(jsonObject1.get("value").getAsInt());
        }
        JsonElement codeJson = jsonObject1.get("code");

        if(!codeJson.isJsonNull() && !codeJson.getAsString().isEmpty()){
            BahmniObservation code1 = createObs(lifeStyleConceptName+" Code", bmiObs, en, obsDatetime);
            code1.setValue(jsonObject1.get("code").getAsString());
        }

        createTargetObs(jsonObject1,lifeStyleConceptName,bmiObs,en,obsDatetime);

        createObsForOutput(jsonObject1,bmiObs,en,obsDatetime);
    }

    public void createTargetObs(JsonObject jsonObject1, String lifeStyleConceptName, BahmniObservation obs, BahmniEncounterTransaction en, Date obsDatetime){
        if(lifeStyleConceptName.equals("Smoking")){
            JsonElement targetJson = jsonObject1.get("smoking_calc");
            if (!targetJson.isJsonNull() ) {
                BahmniObservation target1 = createObs(  lifeStyleConceptName+" Target", obs, en, obsDatetime);
                target1.setValue(jsonObject1.get("smoking_calc").getAsBoolean());
            }

        }else {
            JsonElement targetJson = jsonObject1.get("target");
            if (!targetJson.isJsonNull() && !targetJson.getAsString().isEmpty()) {
                BahmniObservation target1 = createObs(lifeStyleConceptName + " Target", obs, en, obsDatetime);
                target1.setValue(jsonObject1.get("target").getAsString());
            }
        }

    }




    private void createObsForOutput(JsonObject jsonObject,BahmniObservation parent,BahmniEncounterTransaction en,Date obsDatetime){
        if(jsonObject.get("output")!=null && jsonObject.get("output").isJsonArray()){
            JsonArray output =jsonObject.get("output").getAsJsonArray();
            if(output.size()==4){
                BahmniObservation outputObs= createObs("Output", parent, en, obsDatetime);
                BahmniObservation code= createObs("Output Code", outputObs, en, obsDatetime);
                BahmniObservation name= createObs("Output Name", outputObs, en, obsDatetime);
                BahmniObservation color= createObs("Output Color", outputObs, en, obsDatetime);
                BahmniObservation description= createObs("Output Description", outputObs, en, obsDatetime);
                code.setValue(output.get(0).getAsString());
                name.setValue(output.get(1).getAsString());
                color.setValue(output.get(2).getAsString());
                description.setValue(output.get(3).getAsString());
            }
        }

    }

    private String callOhaAPI(OHARequest ohaRequest) throws IOException {
        Gson gson = new Gson();
        String str = gson.toJson(ohaRequest);
        String heartsAssessmentUrl = Context.getAdministrationService().getGlobalProperty("hearts_assessment_url");
        String personalAccessToken = Context.getAdministrationService().getGlobalProperty("personal_access_token");

        if (null != heartsAssessmentUrl && heartsAssessmentUrl.length() > 0) {
            URL url = new URL(heartsAssessmentUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");

            connection.setRequestProperty("Content-Length", "" + Integer.toString(str.getBytes().length));
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Content-Type", "application/json");
            if (personalAccessToken != null && personalAccessToken.length() > 0){
                connection.addRequestProperty("Authorization", "Bearer " + personalAccessToken);
            }

            connection.connect();

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(str);
            wr.flush();
            wr.close();
            System.out.println(connection.getResponseCode());
            if (connection.getResponseCode() == 200) {
                InputStream response = connection.getInputStream();
                String content = new java.util.Scanner(response).useDelimiter("\\A").next();
                System.out.println(content);
                return content;
            }
        }
        return null;
    }

     BahmniObservation createObs(String conceptName, BahmniObservation parent, BahmniEncounterTransaction encounterTransaction, Date obsDatetime) {
         if(conceptName.equalsIgnoreCase("Smoking")){
             conceptName="Smoking Response";
         }
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
