using System;
using System.Configuration;

namespace OS2Vikar
{
    public class Settings
    {
        public bool GetBooleanValue(string key)
        {
            try
            {
                return Boolean.Parse(GetStringValue(key));
            }
            catch (Exception)
            {
                return false;
            }
        }

        public string GetStringValue(string key)
        {
            try
            {
                return ConfigurationManager.AppSettings[key];
            }
            catch (Exception)
            {
                return "";
            }
        }
    }
}
