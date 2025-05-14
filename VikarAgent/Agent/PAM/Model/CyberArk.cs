using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Text.Json.Serialization;

namespace OS2Vikar_Password_Agent.PAM.Model
{
    public class CyberArk
    {
        [JsonPropertyName("Content")]
        public string Password { get; set; }
    }
}
