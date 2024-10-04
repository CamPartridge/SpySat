package spysat.satdata;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Main {
    public static void main(String[] args) {
        JSONArray satData = getSatelliteData();
        calculateOrbits(satData);
        cleanUpSatData(satData);
        System.out.println(satData);
    }

    public static JSONArray getSatelliteData(){
        try {
            String baseURL = "https://www.space-track.org";
            String authPath = "/ajaxauth/login";
            String userName = "cpartridge@student.neumont.edu";
            String password = "SpySat!bookwyrmbam";
//            String query = "/basicspacedata/query/class/gp/decay_date/null-val/epoch/%3Enow-30/orderby/norad_cat_id/format/json";
            String query = "/basicspacedata/query/class/gp/decay_date/null-val/epoch/%3Enow-30/country_code/BOL/orderby/object_name/format/json";

            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            URL url = new URL(baseURL+authPath);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String input = "identity="+userName+"&password="+password;

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }

            StringBuilder builder = new StringBuilder();
            String stringData;

            url = new URL(baseURL + query);

            br = new BufferedReader(new InputStreamReader((url.openStream())));
            while((stringData = br.readLine()) != null){
                builder.append(stringData);
            }

            url = new URL(baseURL + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            System.out.println(br.readLine());

            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONArray satData = (JSONArray) parser.parse(builder.toString());

            return satData;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void calculateOrbits(JSONArray satData){
        for (int i = 0; i < Objects.requireNonNull(satData).size(); i++) {
            JSONObject satellite = (JSONObject) satData.get(i);
            String satelliteOrbit;
            if (Double.parseDouble(satellite.get("MEAN_MOTION").toString()) >= 0.99 &&
                    Double.parseDouble(satellite.get("MEAN_MOTION").toString()) <= 1.01 &&
                    Double.parseDouble(satellite.get("ECCENTRICITY").toString()) < 0.01) {

                satelliteOrbit = "GEO";
            } else if (Double.parseDouble(satellite.get("PERIOD").toString()) >= 600 &&
                    Double.parseDouble(satellite.get("PERIOD").toString()) <= 800 &&
                    Double.parseDouble(satellite.get("ECCENTRICITY").toString()) < 0.25) {

                satelliteOrbit = "MEO";
            } else if (Double.parseDouble(satellite.get("MEAN_MOTION").toString()) > 11.25 &&
                    Double.parseDouble(satellite.get("ECCENTRICITY").toString()) < 0.25) {

                satelliteOrbit = "LEO";
            } else if (Double.parseDouble(satellite.get("ECCENTRICITY").toString()) > 0.25) {
                satelliteOrbit = "HEO";
            } else {
                satelliteOrbit = "OTHER";
            }

            satellite.put("SATELLITE_ORBIT", satelliteOrbit);
        }
    }

    public static void cleanUpSatData(JSONArray satData){
        for (int i = 0; i < Objects.requireNonNull(satData).size(); i++) {
            JSONObject jsonObject = (JSONObject) satData.get(i);

            // Remove the key from the object
            jsonObject.remove("CCSDS_OMM_VERS");
            jsonObject.remove("COMMENT");
            jsonObject.remove("CREATION_DATE");
            jsonObject.remove("ORIGINATOR");
            jsonObject.remove("CENTER_NAME");
            jsonObject.remove("TIME_SYSTEM");
            jsonObject.remove("MEAN_ELEMENT_THEORY");
            jsonObject.remove("MEAN_MOTION");
            jsonObject.remove("ECCENTRICITY");
            jsonObject.remove("INCLINATION");
            jsonObject.remove("RA_OF_ASC_NODE");
            jsonObject.remove("ARG_OF_PERICENTER");
            jsonObject.remove("MEAN_ANOMALY");
            jsonObject.remove("EPHEMERIS_TYPE");
            jsonObject.remove("CLASSIFICATION_TYPE");
            jsonObject.remove("ELEMENT_SET_NO");
            jsonObject.remove("BSTAR");
            jsonObject.remove("MEAN_MOTION_DOT");
            jsonObject.remove("MEAN_MOTION_DDOT");
            jsonObject.remove("SEMIMAJOR_AXIS");
            jsonObject.remove("PERIOD");
            jsonObject.remove("APOAPSIS");
            jsonObject.remove("PERIAPSIS");
            jsonObject.remove("DECAY_DATE");
            jsonObject.remove("FILE");
            jsonObject.remove("GP_ID");
        }
    }
}