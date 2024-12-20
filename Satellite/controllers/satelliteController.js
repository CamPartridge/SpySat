const { MongoClient, ServerApiVersion } = require('mongodb');
const { Transform } = require('stream');
const { startFilters } = require('./filtersController.js')

const uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";
// const client = new MongoClient(uri)
const client = new MongoClient(uri, {
    serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true
    }
});

const satelliteController = {
    getall: async (req, res) => {
        try {
            console.time("Total operation time")
            await client.connect();
            console.timeEnd("Total operation time")

            await client.db("admin").command({ ping: 1 });
            console.log("Pinged your deployment. You successfully connected to MongoDB!");
            console.log("Connected to Mongo");

            const db = client.db('SpySat');
            const collection = db.collection('satellites');

            console.time("MongoDB query time")
            const cursor = collection.find({ OBJECT_TYPE: 'PAYLOAD' }).batchSize(1000);
            console.timeEnd("MongoDB query time");

            res.status(200);
            res.setHeader('Content-Type', 'application/json');

            let itemCount = 0;

            const transformStream = new Transform({
                writableObjectMode: true,
                transform(chunk, encoding, callback) {
                    itemCount++;
                    callback(null, JSON.stringify(chunk) + ',');
                }
            });

            // Start the JSON array in the response
            res.write('[');

            // Pipe the cursor stream to the transform stream, then pipe it to the response
            const stream = cursor.stream();
            stream.pipe(transformStream).pipe(res);

            // Listen for the end of the stream and close the array
            stream.on('end', async () => {
                // Close the JSON array
                res.write('{}]');  // Adding an empty object to handle the trailing comma

                await client.close(); // Close the client after the stream ends
                console.log("Disconnected from Mongo");
                console.log("Number of satellites sent: " + itemCount)

                try {
                    await startFilters(req, res) // Assuming startFilters is in the same file
                } catch (filterError) {
                    console.error("Error triggering Redis filters:", filterError);
                }
            });

            // Handle any errors during streaming
            stream.on('error', (err) => {
                console.error("Stream error:", err);
                res.status(500).send("Error during data streaming");
            });

        } catch (e) {
            console.error(e);
            res.status(503).send("Mongo Service unavailable");
        }

    },
    searchForSatellites: async (req, res) => {
        const { searchQuery } = req.query;
        console.time("Total operation time")
        try {
            await client.connect();
            console.timeEnd("Total operation time")

            await client.db("admin").command({ ping: 1 });
            console.log("Pinged your deployment. You successfully connected to MongoDB!");
            console.log("Connected to Mongo");

            const db = client.db('SpySat');
            const collection = db.collection('satellites');

            const query = {
                $or: [
                    { OBJECT_NAME: { $regex: searchQuery, $options: 'i' } }, 
                    { OBJECT_ID: searchQuery }
                ]
            };

            const satellites = await collection.find(query).toArray();

            res.status(200).json(satellites);
        } catch (error) {
            res.status(500).json({ error: error.message });
        } finally {
            await client.close();
        }
    }
}

module.exports = satelliteController;