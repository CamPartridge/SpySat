// plugins/supertokens.client.js
import SuperTokens from "supertokens-web-js";
import Session from "supertokens-web-js/recipe/session";
import ThirdParty from "supertokens-web-js/recipe/thirdparty";

export default defineNuxtPlugin(() => {
    SuperTokens.init({
        appInfo: {
            appName: "SpySat",
            apiDomain: "http://localhost:3030",
            websiteDomain: "http://localhost:3000",
            apiBasePath: "/auth",
            websiteBasePath: "/auth"
        },
        recipeList: [
            Session.init(),
            ThirdParty.init(),
        ],
    });
});
