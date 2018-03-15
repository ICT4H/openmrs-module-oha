package org.openmrs.module.bahmnioha.advisor;

import org.aopalliance.aop.Advice;
import org.openmrs.module.bahmnioha.advice.BahmniEncounterTransactionUpdateAdvice;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;

public class BahmniEncounterServiceAdvisor extends StaticMethodMatcherPointcutAdvisor implements Advisor {
    private static final String SAVE_METHOD = "save";

    @Override
    public boolean matches(Method method, Class<?> aClass) {
        return SAVE_METHOD.equals(method.getName());
    }

    @Override
    public Advice getAdvice() {
        return new BahmniEncounterTransactionUpdateAdvice();
    }
}
