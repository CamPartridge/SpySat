package spysat.satdata;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoSocketReadException;
import com.mongodb.client.*;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static com.mongodb.client.model.Filters.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.nio.charset.StandardCharsets;


public class Main implements RequestHandler<Object, String>{
    @Override
    public String handleRequest(Object input, Context context) {
        context.getLogger().log("Lambda function invoked with input: " + input);
        JSONArray satData = getSatelliteData();
        calculateOrbits(satData);
        cleanUpSatData(satData);

        addAllDataToMongo(satData);
        deleteOldDocuments();
        addDescriptions();
        addImages();
        addDisciplines();

        return "Mongo updated from aws lambda";
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

            mongoClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addDescriptions() {
        String uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("SpySat");
            MongoCollection<Document> collection = database.getCollection("satellites");


            MongoCursor<Document> cursor = collection.find(or(eq("DESCRIPTION", ""), exists("DESCRIPTION", false))).iterator();


            while (cursor.hasNext()) {
                Document satelliteDoc = cursor.next();

                String objectType = satelliteDoc.getString("OBJECT_TYPE");
                String objectId = satelliteDoc.getString("OBJECT_ID");

                String description = "";

                if ("Payload".equalsIgnoreCase(objectType)) {
                    description = scrapeSatelliteDescription(objectId);
                } else if ("Rocket Body".equalsIgnoreCase(objectType)) {
                    description = "This is a rocket body.";
                } else if ("Debris".equalsIgnoreCase(objectType)) {
                    description = "This is debris.";
                } else {
                    description = "The object is unknown.";
                }

                collection.updateOne(
                        new Document("_id", satelliteDoc.getObjectId("_id")),
                        new Document("$set", new Document("DESCRIPTION", description))
                );
            }
            mongoClient.close();

        } catch (MongoSocketReadException m){
            System.out.println("Mongo Socket automatically closed. /n" + m.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String scrapeSatelliteDescription(String object_id){
        try {
            CookieHandler.setDefault(null);
            org.jsoup.nodes.Document description = Jsoup
                    .connect("https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id="+object_id)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .referrer("https://www.google.com")
                    .get();
            Elements outerPTag = description.getElementsByClass("urone");
            Elements innerPTag = outerPTag.select("p");
            Elements innererPTag = innerPTag.select("p");
            if(!innererPTag.get(1).text().equals("")){
                return innererPTag.get(1).text();
            } else {
                return "No description available";
            }

        }
        catch(IOException e){
//            System.out.println("Website not found");
            return "No description available";
        } catch(IndexOutOfBoundsException e){
//            System.out.println("Website not found");
            return "No description available";
        }
    }

    public static void addDisciplines() {
        String uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("SpySat");
            MongoCollection<Document> collection = database.getCollection("satellites");


            MongoCursor<Document> cursor = collection.find(eq( exists("DISCIPLINES", false))).iterator();


            while (cursor.hasNext()) {
                Document satelliteDoc = cursor.next();

                String objectType = satelliteDoc.getString("OBJECT_TYPE");
                String objectId = satelliteDoc.getString("OBJECT_ID");


                List<String> disciplines = new ArrayList<>();
                if ("Payload".equalsIgnoreCase(objectType)) {
                    disciplines = scrapeSatelliteDisciplines(objectId);
                }

                collection.updateOne(
                        new Document("_id", satelliteDoc.getObjectId("_id")),
                        new Document("$set", new Document("DISCIPLINES", disciplines))
                );
            }
            mongoClient.close();

        } catch (MongoSocketReadException m){
            System.out.println("Mongo Socket automatically closed. /n" + m.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> scrapeSatelliteDisciplines(String object_id) {
        List<String> disciplineList = new ArrayList<>(); // Initialize an empty list
        try {
            CookieHandler.setDefault(null);
            org.jsoup.nodes.Document description = Jsoup
                    .connect("https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=" + object_id)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .referrer("https://www.google.com")
                    .get();

            Elements lists = description.getElementsByClass("urtwo");
            Elements ul = lists.select("ul");
            Element disciplines = ul.get(2);

            // Loop through each <li> item and add it to the list
            for (Element li : disciplines.select("li")) {
                disciplineList.add(li.text());
            }
        } catch (IOException e) {
            System.out.println("Website not found");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Discipline list not found");
        }

        return disciplineList; // Return the list, which may be empty if no <li> items were found
    }

    public static void addImages() {
        String uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("SpySat");
            MongoCollection<Document> collection = database.getCollection("satellites");


            MongoCursor<Document> cursor = collection.find(
                    and(
                            or(
                                    exists("IMAGE", false)
//                                    eq("IMAGE", "No image available")
                            )
                    )
            ).iterator();

            int numOfSat = 0;

            while (cursor.hasNext()) {
                Document satelliteDoc = cursor.next();

                String image = scrapeSatelliteImageWiki(satelliteDoc);


                collection.updateOne(
                        new Document("_id", satelliteDoc.getObjectId("_id")),
                        new Document("$set", new Document("IMAGE", image))
                );
                numOfSat++;
                System.out.println(numOfSat);
            }
            mongoClient.close();

        } catch (MongoSocketReadException m){
            System.out.println("Mongo Socket automatically closed. /n" + m.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String scrapeSatelliteImageWiki(Document satellite) {
        try {
            // Base Wikipedia API URL for search
            String baseUrlSearch = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srsearch=";

            // Formulate query using satellite name and ID
            String query = String.format("\"%s\" AND \"satellite\"", satellite.get("OBJECT_NAME").toString().split(" ")[0].split("-")[0]);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Full URL for the Wikipedia search
            String fullUrlSearch = baseUrlSearch + encodedQuery;

            // Set up cookie management
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            // Create the HTTP connection for search
            URL urlSearch = new URL(fullUrlSearch);
            HttpURLConnection connSearch = (HttpURLConnection) urlSearch.openConnection();
            connSearch.setRequestMethod("GET");
            connSearch.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
            connSearch.setRequestProperty("Accept", "application/json");

            // Read the response for search
            BufferedReader brSearch = new BufferedReader(new InputStreamReader(connSearch.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseSearch = new StringBuilder();
            String line;
            while ((line = brSearch.readLine()) != null) {
                responseSearch.append(line);
            }
            brSearch.close();
            connSearch.disconnect();

            // Parse the search response to get pageid
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(responseSearch.toString());

            // Navigate to "query" and then to "search"
            JSONObject queryObject = (JSONObject) jsonObject.get("query");
            JSONArray searchResults = (JSONArray) queryObject.get("search");

            long pageId = -1;
            // Check if there is at least one result in the array
            if (searchResults.size() > 0) {
                // Extract pageid from the first result
                JSONObject firstResult = (JSONObject) searchResults.get(0);
                pageId = (long) firstResult.get("pageid");
            } else {
                System.out.println("No search results found for: " + satellite.get("OBJECT_NAME").toString());
                return "No image available";
            }

            // Now make the request to get the page images using the pageId
            String baseUrlImage = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&pageids=";
            String fullUrlImage = baseUrlImage + pageId;

            // Create the HTTP connection for image query
            URL urlImage = new URL(fullUrlImage);
            HttpURLConnection connImage = (HttpURLConnection) urlImage.openConnection();
            connImage.setRequestMethod("GET");
            connImage.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
            connImage.setRequestProperty("Accept", "application/json");

            // Read the response for image query
            BufferedReader brImage = new BufferedReader(new InputStreamReader(connImage.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseImage = new StringBuilder();
            while ((line = brImage.readLine()) != null) {
                responseImage.append(line);
            }
            brImage.close();
            connImage.disconnect();


            // Parse the image response to get the image source
            JSONObject jsonImageObject = (JSONObject) parser.parse(responseImage.toString());
            JSONObject imageQueryObject = (JSONObject) jsonImageObject.get("query");
            JSONObject pagesObject = (JSONObject) imageQueryObject.get("pages");

            if (pagesObject.containsKey(String.valueOf(pageId))) {
                JSONObject pageObject = (JSONObject) pagesObject.get(String.valueOf(pageId));
                if (pageObject.containsKey("original")) {
                    JSONObject originalImage = (JSONObject) pageObject.get("original");
                    String imageSource = (String) originalImage.get("source");
                    return imageSource;
                } else {
                    return "No image available";
                }
            } else {
                System.out.println("No page found for the given pageId.");
                return "No image available";
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return "No image available";
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