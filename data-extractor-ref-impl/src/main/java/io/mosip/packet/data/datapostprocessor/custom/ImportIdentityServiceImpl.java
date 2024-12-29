package io.mosip.packet.data.datapostprocessor.custom;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.packet.core.constant.ApiName;
import io.mosip.packet.core.constant.LoggerFileConstant;
import io.mosip.packet.core.dto.ResponseWrapper;
import io.mosip.packet.core.exception.ApisResourceAccessException;
import io.mosip.packet.core.logger.DataProcessLogger;
import io.mosip.packet.core.service.DataRestClientService;
import io.mosip.packet.data.dto.IdRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImportIdentityServiceImpl implements ImportIdentityService {

    private static final String PROVINCE = "province";
    private static final String DISTRICT = "district";
    private static final String CONSTITUENCY = "constituency";
    private static final String SUBFOLDER = "SUBFOLDER";
    private static final String LANG_CODE = "eng";
    private static final String HIERARCHY_NAME = "hierarchyName";
    private static final String NAME = "name";
    private static final String REFERENCE_ID = "INITIALIZATION";

    private final Logger logger = DataProcessLogger.getLogger(ImportIdentityServiceImpl.class);
    private Map<String, Map<String, Object>> locationMemoryMap = new ConcurrentHashMap<>();

    @Autowired
    private DataRestClientService dataRestClientService;

    @Override
    public ResponseWrapper importIdentity(IdRequestDto idRequestDto, Map<String, Object> demoDetails) throws ApisResourceAccessException {
        logger.info("Writer Request: {}", idRequestDto);
        // Fetches location detail
        getLocationDetails(idRequestDto, (String) demoDetails.get(SUBFOLDER));
        // Generates UIN
        generateUIN(idRequestDto);
        // Add Identity call
        return addIdentity(idRequestDto);
    }

    private void getLocationDetails(IdRequestDto idRequestDto, String locationCode) throws ApisResourceAccessException {
        try {
            // Location details fetch
            //String locationCode = (String) ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).get(SUBFOLDER);
            Map<String, Object> identity = ((Map<String, Object>) idRequestDto.getRequest().getIdentity());
            if (locationMemoryMap.get(locationCode) != null && !locationMemoryMap.get(locationCode).isEmpty()) {
                getLocationData(locationMemoryMap.get(locationCode), identity);
            } else {
                List<String> pathSegments = Arrays.asList(locationCode, LANG_CODE);
                ResponseWrapper response = (ResponseWrapper) dataRestClientService.getApi(ApiName.MASTER_LOCATION_GET, pathSegments, "", "", ResponseWrapper.class, REFERENCE_ID);
                if (response != null && response.getResponse() != null) {
                    Map<String, Object> locationLocalMap = new HashMap<>();
                    Map<String, Object> responseMap = (Map<String, Object>) response.getResponse();
                    List<Map<String, Object>> locationDtos = (List<Map<String, Object>>) responseMap.get("locations");
                    locationDtos.stream().forEach(locationDtoMap -> {
                            setLocationData(locationDtoMap, identity);
                            prepareLocationMap(locationLocalMap, locationDtoMap);
                        }
                    );
                    locationMemoryMap.putIfAbsent(locationCode, locationLocalMap);
                    logger.info("Location fetch success.");
                }
            }
        } catch (ApisResourceAccessException e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private void getLocationData(Map<String, Object> locationMap, Map<String, Object> identity) {
        identity.put(PROVINCE, locationMap.get(PROVINCE));
        identity.put(DISTRICT, locationMap.get(DISTRICT));
        identity.put(CONSTITUENCY, locationMap.get(CONSTITUENCY));
    }

    private void setLocationData(Map<String, Object> locationMap, Map<String, Object> identity) {
        if (PROVINCE.equalsIgnoreCase(String.valueOf(locationMap.get(HIERARCHY_NAME)))) {
            identity.put(PROVINCE, getLangMap(locationMap));
        } else if(DISTRICT.equalsIgnoreCase(String.valueOf(locationMap.get(HIERARCHY_NAME)))) {
            identity.put(DISTRICT, getLangMap(locationMap));
        } else if(CONSTITUENCY.equalsIgnoreCase(String.valueOf(locationMap.get(HIERARCHY_NAME)))){
            identity.put(CONSTITUENCY, getLangMap(locationMap));
        }
    }

    private void prepareLocationMap(Map<String, Object> locationLocalMap, Map<String, Object> locationDBMap) {

        if (PROVINCE.equalsIgnoreCase(String.valueOf(locationDBMap.get(HIERARCHY_NAME)))) {
            locationLocalMap.put(PROVINCE, getLangMap(locationDBMap));
        } else if(DISTRICT.equalsIgnoreCase(String.valueOf(locationDBMap.get(HIERARCHY_NAME)))) {
            locationLocalMap.put(DISTRICT, getLangMap(locationDBMap));
        } else if(CONSTITUENCY.equalsIgnoreCase(String.valueOf(locationDBMap.get(HIERARCHY_NAME)))){
            locationLocalMap.put(CONSTITUENCY, getLangMap(locationDBMap));
        }
    }

    private List<Map> getLangMap(Map<String, Object> locationDtoMap) {
        return Arrays.asList(Map.of("language", LANG_CODE, "value", String.valueOf(locationDtoMap.get(NAME))));
    }

    private ResponseWrapper addIdentity(IdRequestDto idRequestDto) throws ApisResourceAccessException {
        //logger.info("Add Identity Request: {}", (new Gson()).toJson(idRequestDto));
        try {

            ResponseWrapper response = (ResponseWrapper) dataRestClientService.postApi(ApiName.ADD_IDENTITY,
                    null, "", "", idRequestDto, ResponseWrapper.class, REFERENCE_ID);
            logger.info("Add Identity call completed.");
            return response;
        } catch (ApisResourceAccessException e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private void generateUIN(IdRequestDto idRequestDto) throws ApisResourceAccessException {
        try {
            // UIN generation
            ResponseWrapper response = (ResponseWrapper) dataRestClientService.getApi(ApiName.GET_UIN, null, "", "", ResponseWrapper.class, REFERENCE_ID);
            if (response != null && response.getResponse() != null) {
                Map<String, String> responseMap = (Map<String, String>) response.getResponse();
                ((Map<String, Object>) idRequestDto.getRequest().getIdentity()).put("UIN", responseMap.get("uin"));
                logger.info("UIN Generation success.");
            }
        } catch (ApisResourceAccessException e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

}