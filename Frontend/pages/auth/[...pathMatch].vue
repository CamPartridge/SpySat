<script>
    import { defineComponent, onMounted, onUnmounted } from 'vue';
    export default defineComponent({
        setup() {
            const loadScript = (src) => {
                const script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = src;
                script.id = 'supertokens-script';
                script.onload = () => {
                    window.supertokensUIInit("supertokensui", {
                        appInfo: {
                            appName: "SpySat",
                            apiDomain: "http://localhost:8080",
                            websiteDomain: "http://localhost:3000",
                            apiBasePath: "/user/auth",
                            websiteBasePath: "/auth"
                        },
                        recipeList: [
                            window.supertokensUIEmailPassword.init(),
                            window.supertokensUIThirdParty.init({
                                signInAndUpFeature: {
                                    providers: [
                                        window.supertokensUIThirdParty.Github.init(),
                                        window.supertokensUIThirdParty.Google.init(),
                                    ]
                                }
                            }),
                            window.supertokensUISession.init(),
                        ],
                    });
                };
                document.body.appendChild(script);
            };

            onMounted(() => {
                loadScript('https://cdn.jsdelivr.net/gh/supertokens/prebuiltui@v0.47.1/build/static/js/main.ba50d5ee.js');
            });

            onUnmounted(() => {
                const script = document.getElementById('supertokens-script');
                if (script) {
                    script.remove();
                }
            });
        },
    });
</script>

<template>
    <div id="supertokensui" />
</template>
