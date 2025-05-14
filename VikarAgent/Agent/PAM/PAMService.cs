using OS2Vikar;
using OS2Vikar_Password_Agent.PAM.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http.Headers;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Text.Json;

namespace OS2Vikar_Password_Agent.PAM
{
    internal class PAMService
    {
        private static Settings settings;
        private static readonly string cyberArkAppId;
        private static readonly string cyberArkSafe;
        private static readonly string cyberArkObject;
        private static readonly string cyberArkAPI;
        private static readonly bool cyberArkEnabled;
        static PAMService()
        {
            settings = new Settings();
            cyberArkAppId = settings.GetStringValue("CyberArk.CyberArkAppId");
            cyberArkSafe = settings.GetStringValue("CyberArk.CyberArkSafe");
            cyberArkObject = settings.GetStringValue("CyberArk.CyberArkObject");
            cyberArkAPI = settings.GetStringValue("CyberArk.CyberArkAPI");
            cyberArkEnabled = settings.GetBooleanValue("CyberArk.Enabled");
        }
        public static string GetApiKey()
        {
            if (cyberArkEnabled)
            {
                string apiKey = null;
                HttpClient httpClient = GetHttpClient();
                var response = httpClient.GetAsync($"/AIMWebService/api/Accounts?AppID={cyberArkAppId}&Safe={cyberArkSafe}&Object={cyberArkObject}");
                response.Wait();
                response.Result.EnsureSuccessStatusCode();
                var responseString = response.Result.Content.ReadAsStringAsync();
                responseString.Wait();
                CyberArk cyberArk = JsonSerializer.Deserialize<CyberArk>(responseString.Result);
                if (cyberArk != null && cyberArk.Password != null)
                {
                    apiKey = cyberArk.Password;
                }
                return apiKey;
            } else
            {
                return settings.GetStringValue("os2vikarApiKey");
            }
        }
        private static HttpClient GetHttpClient()
        {
            var httpClient = new HttpClient();
            httpClient.BaseAddress = new Uri(cyberArkAPI);
            httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            return httpClient;
        }
    }
}
