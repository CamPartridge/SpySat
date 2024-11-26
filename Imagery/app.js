var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
const cors = require('cors');

var imageRouter = require("./routes/imageRoutes.js")

var app = express();

app.use(cors({
    origin: 'http://localhost:3000',  // Specify the frontend's URL
    credentials: true,               // Allow credentials (cookies, etc.)
    methods: ['GET', 'POST', 'PUT', 'DELETE'],  // Allow specific methods if needed
}));


app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

//routes
app.use('/', imageRouter);

module.exports = app;
