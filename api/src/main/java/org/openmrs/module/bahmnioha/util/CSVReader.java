package org.openmrs.module.bahmnioha.util;

import org.bahmni.csv.CSVEntity;
import org.bahmni.csv.CSVFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader<T extends CSVEntity> {

    public List<T> readCsvFileToMemory(CSVFile csvFile, Class<T> entityClass) {
        try {
            T csvEntity;
            List<T> csvRowsFromFile = new ArrayList<>();
            csvFile.openForRead();
            while ((csvEntity = (T) csvFile.readEntity(entityClass)) != null) {
                csvRowsFromFile.add(csvEntity);
            }
            return csvRowsFromFile;
        } catch (IOException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
