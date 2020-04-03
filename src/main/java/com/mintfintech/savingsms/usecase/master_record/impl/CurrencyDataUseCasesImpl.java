package com.mintfintech.savingsms.usecase.master_record.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.dao.CurrencyEntityDao;
import com.mintfintech.savingsms.domain.entities.CurrencyEntity;
import com.mintfintech.savingsms.usecase.master_record.CurrencyDataUseCases;
import com.mintfintech.savingsms.usecase.models.CurrencyModel;
import io.micrometer.core.instrument.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Named;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class CurrencyDataUseCasesImpl implements CurrencyDataUseCases {

    private CurrencyEntityDao currencyEntityDao;
    private Gson gson;
    public CurrencyDataUseCasesImpl(CurrencyEntityDao currencyEntityDao, Gson gson) {
        this.currencyEntityDao = currencyEntityDao;
        this.gson = gson;
    }

    private void createCreateCurrency(String code, String name, String symbol) {
        currencyEntityDao.saveRecord(CurrencyEntity.builder().code(code).name(name).symbol(symbol).build());
    }

    @Override
    public void createDefaultRecords() {
        if(currencyEntityDao.countRecords() != 0) {
            return;
        }
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/currencies.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if(!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<CurrencyModel>>(){}.getType();
                List<CurrencyModel> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total currencies: " + responseList.size());
                responseList.forEach(currency -> createCreateCurrency(currency.getCode(), currency.getName(), currency.getSymbol()));
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Unable to create country records: " + e.getMessage());
        }
    }
}
