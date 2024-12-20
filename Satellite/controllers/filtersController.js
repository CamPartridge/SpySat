const { MongoClient, ServerApiVersion } = require('mongodb');
const { createClient } = require('redis');

//REDIS
const redisClient = createClient({
    url: 'redis://default:SpySat!redis!password@spysat_redis:6379'
    // url: 'redis://default:SpySat!redis!password@localhost:6379'
});

//MONGO
const uri = "mongodb+srv://cambry:SpySatmongo@spysat.f3iwa.mongodb.net/?retryWrites=true&w=majority&appName=SpySat";
const mongoClient = new MongoClient(uri, {
    serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true
    }
});
const db = mongoClient.db('SpySat');
const collection = db.collection('satellites');

redisClient.on('error', err => console.log('Redis Client Error', err));

//FILTERS
let numberOfFilters = 0;
const ORBITAL_LEVELS = ["LEO", "MEO", "HEO", "GEO", "OTHER"]
const SATELLITE_SIZE = ["LARGE", "MEDIUM", "SMALL"]
const SATELLITE_TYPE = ["PAYLOAD", "ROCKET BODY", "DEBRIS", "UNKNOWN"]
const DISCIPLINES = ["ASTRONOMY", "EARTH SCIENCE", "PLANETARY SCIENCE", "SOLAR PHYSICS",
    "SPACE PHYSICS", "LIFE SCIENCE", "MICROGRAVITY", "HUMAN CREW", "ENGINEERING",
    "COMMUNICATIONS", "NAVIGATION/GLOBAL POSITIONING", "SURVEILLANCE AND OTHER MILITARY",
    "RESUPPLY/REFURBISHMENT/REPAIR", "TECHNOLOGY APPLICATIONS", "UNCATEGORIZED COSMOS", "OTHER"];
const EVERYTHING = ["EVERYTHING"]

const COUNTRIES = [
    "ALGERIA", "ARAB SATELLITE COMMUNICATIONS ORGANIZATION", "ARGENTINA", "ASIA SATELLITE TELECOMMUNICATIONS COMPANY (ASIASAT)",
    "AUSTRALIA", "AZERBAIJAN", "BANGLADESH", "BELARUS", "BOLIVIA", "BRAZIL", "BULGARIA", "CANADA", "CHILE", "CHINA/BRAZIL",
    "COMMONWEALTH OF INDEPENDENT STATES (FORMER USSR)", "CZECH REPUBLIC (FORMER CZECHOSLOVAKIA)", "CZECHIA", "DENMARK",
    "ECUADOR", "EGYPT", "ESTONIA", "EUROPEAN ORGANISATION FOR THE EXPLOITATION OF METEOROLOGICAL SATELLITES",
    "EUROPEAN SPACE AGENCY", "EUROPEAN TELECOMMUNICATIONS SATELLITE ORGANIZATION", "FRANCE", "FRANCE/GERMANY",
    "FRANCE/ITALY", "GERMANY", "GLOBALSTAR", "GREECE", "HUNGARY", "INDIA", "INDONESIA", "INTERNATIONAL MOBILE SATELLITE ORGANIZATION (INMARSAT)",
    "INTERNATIONAL SPACE STATION", "INTERNATIONAL TELECOMMUNICATIONS SATELLITE ORGANIZATION", "IRAN", "IRAQ", "ISRAEL", "ITALY",
    "JAPAN", "KAZAKHSTAN", "KENYA", "KUWAIT", "LAOS", "LITHUANIA", "LUXEMBOURG", "MALAYSIA", "MEXICO", "MOROCCO", "NETHERLANDS", "NEW ICO",
    "NEW ZEALAND", "NIGERIA", "NORTH ATLANTIC TREATY ORGANIZATION", "NORTH KOREA", "NORWAY", "O3B NETWORKS",
    "ORBCOMM", "PAKISTAN", "PEOPLE'S REPUBLIC OF CHINA", "PERU", "PHILIPPINES (REPUBLIC OF THE PHILIPPINES)", "POLAND",
    "PORTUGAL", "QATAR", "REGIONAL AFRICAN SATELLITE COMMUNICATIONS ORGANIZATION", "REPUBLIC OF RWANDA",
    "REPUBLIC OF SLOVENIA", "REPUBLIC OF TUNISIA", "SAUDI ARABIA", "SEA LAUNCH", "SINGAPORE", "SINGAPORE/TAIWAN",
    "SOCIETE EUROPEENNE DES SATELLITES", "SOUTH AFRICA", "SOUTH KOREA", "SPAIN", "SWEDEN", "TAIWAN (REPUBLIC OF CHINA)", "THAILAND",
    "TURKEY", "TURKMENISTAN/MONACO", "UNITED ARAB EMIRATES", "UNITED KINGDOM", "UNITED STATES", "UNITED STATES/BRAZIL",
    "URUGUAY", "VENEZUELA", "VIETNAM"
];

const COUNTRY_CODES = [
    "ALG", "AB", "ARGN", "AC", "AUS", "AZER", "BGD", "BELA", "BOL",
    "BRAZ", "BGR", "CA", "CHLE", "CHBZ", "CIS", "CZCH", "CZ", "DEN",
    "ECU", "EGYP", "EST", "EUME", "ESA", "EUTE", "FR", "FGER", "FRIT",
    "GER", "GLOB", "GREC", "HUN", "IND", "INDO", "IM", "ISS", "ITSO", "IRAN",
    "IRAK", "ISRA", "IT", "JPN", "KAZ", "KEN", "KWT", "LAOS", "LTU", "LUXE",
    "MALA", "MEX", "MA", "NETH", "NICO", "NZ", "NIG", "NATO", "NKOR", "NOR",
    "O3B", "ORB", "PAKI", "PRC", "PER", "RP", "POL", "POR", "QAT", "RASC",
    "RWA", "SVN", "TUN", "SAUD", "SEAL", "SING", "STCT", "SES", "SAFR",
    "SKOR", "SPN", "SWED", "ROC", "THAI", "TURK", "TMMC", "UAE", "UK",
    "US", "USBZ", "URY", "VENZ", "VTNM"
];

const filtersController = {
    startFilters: async (req, res) => {
        try {

            numberOfFilters = 0;
            await mongoClient.connect();
            await redisClient.connect();

            await doAllFilters();
            res.status(200).json({ 'Message': "Successfully updated redis" });
        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },
    getByFilter: async (req, res) => {
        try {
            const { size, level, type, country, discipline } = req.query;

            // Ensure Redis client is connected
            if (!redisClient.isOpen) {
                await redisClient.connect();
            }

            let filterKeys = [];

            // Collect filter keys based on query parameters
            if (size) filterKeys.push(size);
            if (level) filterKeys.push(level);
            if (type) filterKeys.push(type);
            if (country) filterKeys.push(country);
            if (discipline) filterKeys.push(discipline);

            let filteredSatellites = [];

            if (filterKeys.length === 0) {
                const allSatellites = await redisClient.get('EVERYTHING');
                filteredSatellites = JSON.parse(allSatellites || '[]');
            } else {
                // Fetch data for all provided filters
                const filterPromises = filterKeys.map((key) => redisClient.get(key));
                const filterResults = await Promise.all(filterPromises);

                // Parse results and filter out null or empty arrays
                const parsedResults = filterResults
                    .map((result) => JSON.parse(result || '[]'))
                    .filter((arr) => arr.length > 0);

                // Perform intersection of all arrays based on OBJECT_ID
                if (parsedResults.length > 0) {
                    filteredSatellites = parsedResults.reduce((acc, curr) =>
                        acc.filter((satellite) =>
                            curr.some(
                                (otherSatellite) =>
                                    satellite.OBJECT_ID === otherSatellite.OBJECT_ID
                            )
                        )
                    );
                }
            }

            // Response with filtered satellites or an empty array
            res.status(200).json(filteredSatellites);
        } catch (error) {
            res.status(500).json({ error: error.message });
        } finally {
            // Disconnect Redis if connected
            if (redisClient.isOpen) {
                await redisClient.disconnect();
            }
        }
    }




}

module.exports = filtersController

const addFilterToRedis = async (filterName, groupOfSatellites) => {
    await redisClient.set(filterName, groupOfSatellites)
}

const doAllFilters = async () => {
    for (const everything of Object.values(EVERYTHING)) {
        console.time(everything + " filter time")
        getEverything()
            .then(async (satellite) => {
                await addFilterToRedis(everything, JSON.stringify(satellite))
            })
            .then(async () => {
                console.log(everything + ' Data added to Redis successfully!');
                console.timeEnd(everything + " filter time")

                numberOfFilters++

                if (numberOfFilters > COUNTRY_CODES.length + ORBITAL_LEVELS.length + SATELLITE_SIZE.length + SATELLITE_TYPE.length + DISCIPLINES.length) {
                    await mongoClient.close();
                    await redisClient.quit();
                    console.log("CONNECTIONS CLOSED")
                }
            })
            .catch((error) => {
                console.log(everything + " Data unable to be added to Redis");
                console.log('Error:', error);
                console.timeEnd(everything + " filter time")
            });
    }

    for (const level of Object.values(ORBITAL_LEVELS)) {
        console.time((level == "OTHER" ? "OTHER_ORBIT" : level) + " filter time")
        getOrbitFilters(level)
            .then(async (satellite) => {
                // console.log(satellite)
                await addFilterToRedis((level == "OTHER" ? "OTHER_ORBIT" : level), JSON.stringify(satellite))
            })
            .then(async () => {
                console.log((level == "OTHER" ? "OTHER_ORBIT" : level) + ' Data added to Redis successfully!');
                console.timeEnd((level == "OTHER" ? "OTHER_ORBIT" : level) + " filter time");

                numberOfFilters++

                if (numberOfFilters > COUNTRY_CODES.length + ORBITAL_LEVELS.length + SATELLITE_SIZE.length + SATELLITE_TYPE.length + DISCIPLINES.length) {
                    await mongoClient.close();
                    await redisClient.quit();
                    console.log("CONNECTIONS CLOSED")
                }
            })
            .catch((error) => {
                console.log((level == "OTHER" ? "OTHER_ORBIT" : level) + " Data unable to be added to Redis");
                console.log('Error:', error);
                console.timeEnd((level == "OTHER" ? "OTHER_ORBIT" : level) + " filter time");
            });
    }

    for (const size of Object.values(SATELLITE_SIZE)) {
        console.time(size + "_SIZE filter time")
        getSizeFilters(size)
            .then(async (satellite) => {
                // console.log(satellite)
                await addFilterToRedis(size + "_SIZE", JSON.stringify(satellite))
            })
            .then(async () => {
                console.log(size + '_SIZE Data added to Redis successfully!');
                console.timeEnd(size + "_SIZE filter time")

                numberOfFilters++

                if (numberOfFilters > COUNTRY_CODES.length + ORBITAL_LEVELS.length + SATELLITE_SIZE.length + SATELLITE_TYPE.length + DISCIPLINES.length) {
                    await mongoClient.close();
                    await redisClient.quit();
                    console.log("CONNECTIONS CLOSED")
                }
            })
            .catch((error) => {
                console.log(size + "_SIZE Data unable to be added to Redis");
                console.log('Error:', error);
                console.timeEnd(size + "_SIZE filter time")
            });
    }

    for (const type of Object.values(SATELLITE_TYPE)) {
        console.time(type + " filter time")
        getTypeFilters(type)
            .then(async (satellite) => {
                // console.log(satellite)
                await addFilterToRedis(type, JSON.stringify(satellite))
            })
            .then(async () => {
                console.log(type + ' Data added to Redis successfully!');
                console.timeEnd(type + " filter time")

                numberOfFilters++

                if (numberOfFilters > COUNTRY_CODES.length + ORBITAL_LEVELS.length + SATELLITE_SIZE.length + SATELLITE_TYPE.length + DISCIPLINES.length) {
                    await mongoClient.close();
                    await redisClient.quit();
                    console.log("CONNECTIONS CLOSED")
                }
            })
            .catch((error) => {
                console.log(type + " Data unable to be added to Redis");
                console.log('Error:', error);
                console.timeEnd(type + " filter time")
            });
    }

    for (let i = 0; i < COUNTRY_CODES.length; i++) {
        console.time(COUNTRIES[i] + " filter time")
        getCountryFilters(COUNTRY_CODES[i])
            .then(async (satellite) => {
                // console.log(satellite)
                await addFilterToRedis(COUNTRIES[i], JSON.stringify(satellite))
            })
            .then(async () => {
                console.log(COUNTRIES[i] + ' Data added to Redis successfully!');
                console.timeEnd(COUNTRIES[i] + " filter time")

                numberOfFilters++

                if (numberOfFilters > COUNTRY_CODES.length + ORBITAL_LEVELS.length + SATELLITE_SIZE.length + SATELLITE_TYPE.length + DISCIPLINES.length) {
                    try {

                        await mongoClient.close();
                        await redisClient.quit();
                        console.log("CONNECTIONS CLOSED")
                    } catch (error) {
                        console.log(COUNTRIES[i] + " Data unable to be added to Redis");
                        console.log('Error:', error);
                        console.timeEnd(COUNTRIES[i] + " filter time")
                    }
                }
            })
            .catch((error) => {
                console.log(COUNTRIES[i] + " Data unable to be added to Redis");
                console.log('Error:', error);
                console.timeEnd(COUNTRIES[i] + " filter time")
            });
    }

    for (const discipline of Object.values(DISCIPLINES)) {
        console.time(discipline + " filter time")
        getDisciplineFilters(discipline)
            .then(async (satellite) => {
                // console.log(satellite)
                await addFilterToRedis(discipline, JSON.stringify(satellite))
            })
            .then(async () => {
                console.log(discipline + ' Data added to Redis successfully!');
                console.timeEnd(discipline + " filter time")

                numberOfFilters++

                if (numberOfFilters > COUNTRY_CODES.length + ORBITAL_LEVELS.length + SATELLITE_SIZE.length + SATELLITE_TYPE.length + DISCIPLINES.length ) {
                    await mongoClient.close();
                    await redisClient.quit();
                    console.log("CONNECTIONS CLOSED")
                }
            })
            .catch((error) => {
                console.log(discipline + " Data unable to be added to Redis");
                console.log('Error:', error);
                console.timeEnd(discipline + " filter time")
            });
    }
}

const getOrbitFilters = async (orbitLevel) => {
    try {
        const cursor = collection.find({ SATELLITE_ORBIT: orbitLevel }).batchSize(1000);
        const satellites = [];
        for await (const satellite of cursor) {
            satellites.push(satellite);
        }
        return satellites
    } catch (error) {
        console.error('Error fetching' + orbitLevel + 'satellites:', error);
        return {}
    }
}

const getSizeFilters = async (size) => {
    try {
        const cursor = collection.find({ RCS_SIZE: size }).batchSize(1000);
        const satellites = [];
        for await (const satellite of cursor) {
            satellites.push(satellite);
        }
        return satellites
    } catch (error) {
        console.error('Error fetching' + size + 'satellites:', error);
        return {}
    }
}

const getTypeFilters = async (type) => {
    try {
        const cursor = collection.find({ OBJECT_TYPE: type }).batchSize(1000);
        const satellites = [];
        for await (const satellite of cursor) {
            satellites.push(satellite);
        }
        return satellites
    } catch (error) {
        console.error('Error fetching ' + type + ' satellites:', error);
        return {}
    }
}

const getCountryFilters = async (country) => {
    try {
        const cursor = collection.find({ COUNTRY_CODE: country }).batchSize(1000);
        const satellites = [];
        for await (const satellite of cursor) {
            satellites.push(satellite);
        }
        return satellites
    } catch (error) {
        console.error('Error fetching ' + country + ' satellites:', error);
        return {}
    }
}

const getDisciplineFilters = async (discipline) => {
    try {
        const regex = new RegExp(`^${discipline}$`, 'i'); // 'i' for case-insensitive

        const cursor = collection.find({ DISCIPLINES: { $elemMatch: { $regex: regex } } }).batchSize(1000);
        const satellites = [];
        for await (const satellite of cursor) {
            satellites.push(satellite);
        }
        return satellites
    } catch (error) {
        console.error('Error fetching ' + discipline + ' satellites:', error);
        return {}
    }
}

const getEverything = async () => {
    try {
        const cursor = collection.find({}).batchSize(1000); 
        const satellites = [];
        for await (const satellite of cursor) {
            satellites.push(satellite);
        }
        return satellites;
    } catch (error) {
        console.error('Error fetching all satellites:', error);
        return {}
    }
}
