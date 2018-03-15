package org.openmrs.module.bahmnioha.advice;

import groovy.lang.GroovyClassLoader;
import org.apache.log4j.Logger;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.bahmniemrapi.obscalculator.ObsValueCalculator;
import org.openmrs.module.bahmnioha.service.BahmniOHAServiceImpl;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;

public class BahmniEncounterTransactionUpdateAdvice implements MethodBeforeAdvice {

    private static Logger logger = Logger.getLogger(BahmniEncounterTransactionUpdateAdvice.class);
    private BahmniOHAServiceImpl bahmniOHAServiceImpl = new BahmniOHAServiceImpl();
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {

        logger.info("BahmniEncounterTransactionUpdateAdvice : Start");
        System.out.println("Hello how are you");
        BahmniEncounterTransaction bahmniEncounterTransaction=(BahmniEncounterTransaction) args[0];
        System.out.println(bahmniEncounterTransaction.getObservations().size());
        bahmniOHAServiceImpl.calculateOHAAssesment(bahmniEncounterTransaction);
        logger.info("BahmniEncounterTransactionUpdateAdvice : Done");
    }
    
}
