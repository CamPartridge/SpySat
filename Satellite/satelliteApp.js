var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var satelliteRouter = require('./routes/satelliteRoutes.js')
const compression = require('compression');

var app = express();

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));
app.use(compression({ level: 1 }));


app.use('/', satelliteRouter);

module.exports = app;
