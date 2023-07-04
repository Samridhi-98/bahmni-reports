package org.bahmni.reports.icd10.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bahmni.reports.icd10.Icd10Service;
import org.bahmni.reports.icd10.bean.ICDRule;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class Icd10ServiceImpl implements Icd10Service {

    private String mapRules;

    public String getMapRules() {
        return mapRules;
    }

    public void setMapRules(String mapRules) {
        this.mapRules = mapRules;
    }

    public static void main(String[] args) {

    //   searchMapRules("6705321000119109",0,100,true);
        Icd10ServiceImpl icd = new Icd10ServiceImpl();
        icd.getMapRules("16705321000119109",0,100,true);
//        getMapRules("8619003",0,100,true);

    };

    private static String getResponse(){
        String res = "String to return";
        System.out.println("Test");
        return res;
    }


    private static String searchMapTest(){
        String testString = "Testing -- ";
//        System.out.println("We are here!!!");
        return testString;
    }

    @Override
    public List<ICDRule> getMapRules(String snomedCode, Integer offset, Integer limit, Boolean termActive) {
        String baseUrl = getBaseURL(offset, limit, termActive);
        String eclUrl = getEclUrl(snomedCode);
        String encodedEcl = null;
        StringBuilder sb = new StringBuilder(baseUrl);
        String urls = null;

        try {
            encodedEcl = encode(eclUrl);
            sb.append("&ecl=").append(encodedEcl);
            urls = sb.toString();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        String response = sendGETRequest(urls);
        ObjectMapper mapper = new ObjectMapper();
        List<ICDRule> rules = new ArrayList<>();

        List<ICDRule> sortedRules = null;
        try {
            JsonNode jsonNode = mapper.readValue(response, JsonNode.class);
            JsonNode itemsArr = jsonNode.get("items");
            System.out.println("itemsArr: " + itemsArr);

            if (itemsArr.isArray()) {
                for (JsonNode item : itemsArr) {
                    ICDRule rule = mapper.readValue(mapper.writeValueAsString(item), ICDRule.class);
                    rules.add(rule);
                }
            }

            Comparator<ICDRule> customComparator = (rule1, rule2) -> {
                int rule1Weight = Integer.parseInt(rule1.mapGroup + rule1.mapPriority);
                int rule2Weight = Integer.parseInt(rule2.mapGroup + rule2.mapPriority);
                int difference = rule1Weight - rule2Weight;
                return difference;
            };

            sortedRules = rules.stream().sorted(customComparator).collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sortedRules;
    }

    private String getBaseURL(Integer offset, Integer limit, Boolean termActive) {
        String baseUrl = "https://browser.ihtsdotools.org/snowstorm/snomed-ct/MAIN/SNOMEDCT-ES/2022-10-31/concepts";
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?offset=").append(offset)
                .append("&limit=").append(limit)
                .append("&termActive=").append(termActive);
        return sb.toString();
    }

    private String getEclUrl(String referencedComponentId) {
        return "^[*] 447562003 |ICD-10 complex map reference set| {{ M referencedComponentId = \"" + referencedComponentId + "\" }}";
    }

    private String sendGETRequest(String urlString) {
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
            } else {
                System.out.println("GET request failed. Response Code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response.toString();
    }

    private String encode(String rawStr) throws UnsupportedEncodingException {
        return URLEncoder.encode(rawStr, StandardCharsets.UTF_8.name());
    }

}
