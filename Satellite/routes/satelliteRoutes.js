var express = require('express');
var router = express.Router();
const satelliteController = require('../controllers/satelliteController.js')
const filtersController = require('../controllers/filtersController.js')

router.get('/getall', satelliteController.getall)

router.post('/filters', filtersController.startFilters)
router.get('/filters', filtersController.getByFilter )

module.exports = router