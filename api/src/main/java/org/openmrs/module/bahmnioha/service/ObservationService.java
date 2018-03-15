package org.openmrs.module.bahmnioha.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.*;
import org.openmrs.api.APIException;
import org.openmrs.api.ObsService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmnioha.CIELDictionary;
import org.openmrs.module.bahmnioha.util.RulesEngineProperties;

import java.util.Arrays;
import java.util.List;

public class ObservationService {
//
//    public enum ConceptRepo {
//        WEIGHT("Weight", "concept.weight.uuid", CIELDictionary.WEIGHT_UUID),
//        HEIGHT("Height", "concept.height.uuid", CIELDictionary.HEIGHT_UUID);
//
//        private String cName;
//        private String cUuidKey;
//        private String cielUuid;
//
//        ConceptRepo(String cName, String cUuidKey, String cielUuid) {
//            this.cName = cName;
//            this.cUuidKey = cUuidKey;
//            this.cielUuid = cielUuid;
//        }
//    }
//
//    public static Double getLatestObsValueNumeric(Patient patient, ConceptRepo concept, String visitUuid) throws Exception {
//        Obs observation = getLatestObservation(patient, identifyConceptUuid(concept), concept.cName, visitUuid);
//        if (observation != null) {
//            return observation.getValueNumeric();
//        }
//        return null;
//    }
//
//    private static Obs getLatestObservation(Patient patient, String conceptUuid, String name, String visitUuid) throws Exception {
//        org.openmrs.Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
//        if (concept == null) {
//            throw new APIException(String.format("Concept [%s] is not configured in rules.", name));
//        }
//        List<Obs> observations;
//        if (visitUuid != null) {
//            observations = getObsFromGivenVisit(patient, visitUuid, concept);
//        } else {
//            observations = getObsFromAcrossVisits(patient, concept);
//        }
//        if (CollectionUtils.isEmpty(observations)) {
//            return null;
//        }
//        return observations.get(0);
//    }
//
//    private static List<Obs> getObsFromGivenVisit(Patient patient, String visitUuid, Concept concept) {
//        List<Obs> observations = null;
//        VisitService visitService = Context.getVisitService();
//        Visit visit = visitService.getVisitByUuid(visitUuid);
//        if (visit.getNonVoidedEncounters().size() != 0) {
//            observations = getObservations(Arrays.asList(patient.getPerson()), visit.getNonVoidedEncounters(), Arrays.asList(concept));
//        }
//        return observations;
//    }
//
//    private static List<Obs> getObsFromAcrossVisits(Patient patient, Concept concept) {
//        return getObservations(Arrays.asList(patient.getPerson()), null, Arrays.asList(concept));
//    }
//
//    private static List<Obs> getObservations(List<Person> patients, List<Encounter> encounters, List<Concept> concepts) {
//        ObsService obsService = Context.getObsService();
//        return obsService.getObservations(patients, encounters, concepts,
//                null, null, null, null, null, null, null, null, false);
//    }
//
//    private static String identifyConceptUuid(ConceptRepo concept) {
//        String conceptUuid = RulesEngineProperties.getProperty(concept.cUuidKey);
//        return StringUtils.isBlank(conceptUuid) ? concept.cielUuid : conceptUuid;
//    }
}
