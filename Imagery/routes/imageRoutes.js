var express = require('express');
var router = express.Router();
const imageController = require("../controllers/imageController.js")


router.get('/get', imageController.getRecentImageBySatelliteNoradID)

module.exports = router