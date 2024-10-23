var express = require('express');
var cors = require("cors");
var supertokens = require("supertokens-node");
var { middleware } = require("supertokens-node/framework/express");
var { errorHandler } = require("supertokens-node/framework/express");
var Session = require("supertokens-node/recipe/session");
var EmailPassword = require("supertokens-node/recipe/emailpassword");
var ThirdParty = require("supertokens-node/recipe/thirdparty");
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');

var indexRouter = require('./routes/index');
var usersRouter = require('./routes/users');

let app = express();

const apiKey = undefined; // Set if you have one

// Initialize SuperTokens
supertokens.init({
    framework: "express",
    supertokens: {
        // connectionURI: "http://localhost:3567", // Ensure this matches your SuperTokens core instance
        connectionURI: "http://supertokens:3567", // Ensure this matches your SuperTokens core instance
        apiKey: apiKey || undefined // Ensure this matches your setup
    },
    appInfo: {
        // learn more about this on https://supertokens.com/docs/thirdpartyemailpassword/appinfo
        appName: "SpySat",
        apiDomain: "http://localhost:3030",
        websiteDomain: "http://localhost:3000",
        apiBasePath: "/auth",
        websiteBasePath: "/auth"
    },
    recipeList: [
        EmailPassword.init(),
        ThirdParty.init({
            signInAndUpFeature: {
                providers: [{
                    config: {
                        thirdPartyId: "google",
                        clients: [{
                            clientId: "1060725074195-kmeum4crr01uirfl2op9kd5acmi9jutn.apps.googleusercontent.com", // Update with my own ID
                            clientSecret: "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" // Update with your actual secret
                        }]
                    }
                }, {
                    config: {
                        thirdPartyId: "github",
                        clients: [{
                            clientId: "467101b197249757c71f", // Update with my own ID
                            clientSecret: "e97051221f4b6426e8fe8d51486396703012f5bd" // Update with your actual secret
                        }]
                    }
                }],
            }
        }),
        Session.init() // Initializes session features
    ]
});

// CORS setup
app.use(cors({
    origin: "http://localhost:3000",
    allowedHeaders: ["content-type", "st-auth-mode", ...supertokens.getAllCORSHeaders()],
    credentials: true,
}));

// SuperTokens Middleware
app.use(middleware());

// Other Middleware
app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// Routes
app.use('/', indexRouter);
app.use('/users', usersRouter);

// Error Handler
app.use(errorHandler());
app.use((err, req, res, next) => {
    // Custom error handling logic
    res.status(err.status || 500);
    res.json({ error: err.message });
});

module.exports = app;
