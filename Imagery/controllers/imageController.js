const { MongoClient, ServerApiVersion } = require('mongodb');
const { getLatLngObj } = require("tle.js");


const mongoUri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";

// const client = new MongoClient(uri)
const client = new MongoClient(mongoUri, {
    serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true
    }
});

const imageController = {
    getRecentImageBySatelliteNoradID: async (req, res) => {
        const { NORAD_CAT_ID } = req.query;

        let longitude
        let latitude

        let latAndLonDetails = await getLatAndLonFromNoradID(NORAD_CAT_ID);

        

        if (latAndLonDetails == "No satellite found") {
            res.status(404).json({"message" : "No satellite found with the Norad Catalog ID"})
        } else if(latAndLonDetails == "Does not recieve imagery"){
            res.status(404).json({"message" : "Satellite does not carry a payload"})
        } else {
            longitude = latAndLonDetails.lng
            latitude = latAndLonDetails.lat

            const imageUrl = `https://api.nasa.gov/planetary/earth/assets?lon=${longitude}&lat=${latitude}&dim=0.25&api_key=vKTjnNRjx9n3KPeRim9f6UE0wbRtmvM4DSvPQuvG`;

            const fetch = (...args) => import('node-fetch').then(({ default: fetch }) => fetch(...args));

            const response = await fetch(imageUrl);
            if (!response.ok) {
                console.error("Error fetching image:", response.statusText);
                return res.status(404).json({ "message": response.statusText });
            }
    
            const data = await response.json();
            res.status(200).json({ "url": data.url });
        }
    }
}

module.exports = imageController;

const getLatAndLonFromNoradID = async (noradID) => {
    await client.connect();
    const db = client.db('SpySat');
    const collection = db.collection('satellites');

    const result = await collection.findOne({ NORAD_CAT_ID: Number(noradID) });

    if (result) {

        if(result.OBJECT_TYPE == "PAYLOAD"){

            
            //create tle object
            const tle = `${result.TLE_LINE0}
            ${result.TLE_LINE1}
            ${result.TLE_LINE2}`
            
            //get the lat and log of the satellite
            const currentTime = Date.now();
            const latLonObj = getLatLngObj(tle, currentTime);
            
            await client.close();
            return latLonObj
        }

        return "Does not recieve imagery"

    } else {

        await client.close();
        return "No satellite found"
    }





}

