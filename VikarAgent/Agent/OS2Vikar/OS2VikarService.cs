using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using OS2Vikar_Password_Agent.PAM;
using Serilog;
using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;

namespace OS2Vikar
{
    public class OS2VikarService
    {
        private ADStub adStub;
        private ILogger logger;
        private bool enabled;
        private string baseUrl;
        private string apiKey;

        public OS2VikarService(ADStub adStub, ILogger logger, Settings settings)
        {
            this.logger = logger;
            this.adStub = adStub;

            enabled = settings.GetBooleanValue("syncADGroupsToOS2vikarEnabled");
            baseUrl = settings.GetStringValue("os2vikarBaseUrl");
            apiKey = PAMService.GetApiKey();
        }

        public void SyncADGroups()
        {
            if (!enabled)
            {
                return;
            }

            logger.Information("Starting to sync AD Groups to OS2vikar");
            List<ADGroupSyncDto> groups = adStub.GetGroupsFromOu();

            string url = baseUrl + "api/adgroups";

            logger.Information("Found " + groups.Count + " AD groups, sending them to " + url);

            using (var httpClient = GetHttpClient())
            {
                var content = new StringContent(JsonConvert.SerializeObject(groups, getSerializerSettings()), Encoding.UTF8, "application/json");
                var response = httpClient.PostAsync(new Uri(url), content).Result;

                if (!response.IsSuccessStatusCode)
                {
                    logger.Error("Failed to send AD groups: " + response.StatusCode);
                }
            }

            logger.Information("Finished syncing AD Groups to OS2vikar");
        }

        private HttpClient GetHttpClient()
        {
            var handler = new HttpClientHandler();

            handler.ClientCertificateOptions = ClientCertificateOption.Manual;
            handler.ServerCertificateCustomValidationCallback =
                (httpRequestMessage, cert, cetChain, policyErrors) =>
                {
                    return true;
                };

            var httpClient = new HttpClient(handler);
            httpClient.DefaultRequestHeaders.Add("ApiKey", apiKey);

            return httpClient;
        }

        private JsonSerializerSettings getSerializerSettings()
        {
            // api requires camel case
            return new JsonSerializerSettings
            {
                ContractResolver = new DefaultContractResolver
                {
                    NamingStrategy = new CamelCaseNamingStrategy()
                }
            };
        }
    }
}
