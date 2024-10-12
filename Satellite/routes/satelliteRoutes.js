var express = require('express');
var router = express.Router();
const satelliteController = require('../controllers/satelliteController.js')

router.get('/getall', satelliteController.getall)

module.exports = router