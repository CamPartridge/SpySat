package spysat.satdata;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import java.util.*;

import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


public class Main {
    public static void main(String[] args) {
        JSONArray satData = getSatelliteData();
        calculateOrbits(satData);
        cleanUpSatData(satData);

        addAllDataToMongo(satData);
        deleteOldDocuments();
    }

    public static void addAllDataToMongo(JSONArray satData){
        String uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("SpySat");
            MongoCollection<Document> collection = database.getCollection("satellites");

//            List<Document> documents = new ArrayList<>();
            List<WriteModel<Document>> bulkOperations = new ArrayList<>();

            for (int i = 0; i < Objects.requireNonNull(satData).size(); i++) {
                JSONObject satellite = (JSONObject) satData.get(i);
                Integer noradCatId = Integer.parseInt(satellite.get("NORAD_CAT_ID").toString());
                Document filter = new Document("NORAD_CAT_ID", noradCatId);
//                System.out.println(satData.get(i).toString());
                Document updates = Document.parse(satData.get(i).toString());
                Document updateOperation = new Document("$set", updates);


                bulkOperations.add(new UpdateOneModel<>(filter, updateOperation, new UpdateOptions().upsert(true)));
                if (bulkOperations.size() == 2000) {
                    collection.bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));
                }
            }
            if (!bulkOperations.isEmpty()) {
                collection.bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));
            }

            mongoClient.close();

            System.out.println("Mongo updated successfully!");
        } catch (Exception e){
            System.out.println("Something went wrong when updating database");
            e.printStackTrace();
        }
    }

    public static void deleteOldDocuments() {
        String uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("SpySat");
            MongoCollection<Document> collection = database.getCollection("satellites");

            Date currentDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Document filter = new Document("DECAY_DATE", new Document("$lt", sdf.format(currentDate)));

            collection.deleteMany(filter);

            System.out.println("Old documents deleted successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static JSONArray getSatelliteData(){
        try {
            String baseURL = "https://www.space-track.org";
            String authPath = "/ajaxauth/login";
            String userName = "cpartridge@student.neumont.edu";
            String password = "SpySat!bookwyrmbam";
            String query = "/basicspacedata/query/class/gp/epoch/%3Enow-10/orderby/norad_cat_id/format/json";
//            String query = "/basicspacedata/query/class/gp/decay_date/null-val/epoch/%3Enow-30/country_code/BOL/orderby/object_name/format/json";

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
            System.out.println(satData.size());
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

    public static void cleanUpSatData(JSONArray satData) {
        for (int i = 0; i < Objects.requireNonNull(satData).size(); i++) {
            JSONObject jsonObject = (JSONObject) satData.get(i);
            jsonObject.put("NORAD_CAT_ID", Integer.valueOf(jsonObject.get("NORAD_CAT_ID").toString()));
            jsonObject.put("REV_AT_EPOCH", Integer.valueOf(jsonObject.get("REV_AT_EPOCH").toString()));
            jsonObject.put("DECAY_DATE", jsonObject.get("DECAY_DATE") == null ? null: jsonObject.get("DECAY_DATE").toString());


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
            jsonObject.remove("FILE");
            jsonObject.remove("GP_ID");

    }

}
}