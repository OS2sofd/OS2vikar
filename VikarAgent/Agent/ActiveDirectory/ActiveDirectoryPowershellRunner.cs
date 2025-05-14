using Serilog;
using System;
using System.Management.Automation;

namespace OS2Vikar
{
    class ActiveDirectoryPowershellRunner
    {
        private static ILogger log = new LoggerConfiguration().ReadFrom.AppSettings().CreateLogger().ForContext(typeof(ActiveDirectoryPowershellRunner));
        private string createPowershellScript;

        public ActiveDirectoryPowershellRunner(string createPowershellScript)
        {
            this.createPowershellScript = createPowershellScript;
        }

        public bool Run(string sAMAccountName, string domainController, bool throwOnFailure = true)
        {
            var success = false;
            try
            {
                string script = System.IO.File.ReadAllText(createPowershellScript);
                if (!string.IsNullOrEmpty(script))
                {
                    using (PowerShell powershell = PowerShell.Create())
                    {
                        script = script + "\n\n" +
                            "$ppArg1=\"" + sAMAccountName + "\"\n" +
                            "$ppArg2=\"" + domainController + "\"\n";

                        script += "Invoke-Method -SAMAccountName $ppArg1 -DomainController $ppArg2\n";
                        script += "\n";

                        powershell.AddScript(script);

                        var result = powershell.Invoke();

                        if (result.Count == 0)
                        {
                            Exception ex = null;
                            if (powershell.Streams.Error.Count > 0)
                            {
                                ex = powershell.Streams.Error[0].Exception;
                            }
                            throw new Exception("Did not get a response from executing powershell " + createPowershellScript + " with arguments: sAMAccountName=" + sAMAccountName, ex);
                        }

                        var msg = result[result.Count - 1].ToString();
                        success = "true".Equals(msg);
                        if (!success)
                        {
                            throw new Exception("Powershell executation indicated failed operation for " + createPowershellScript + " with arguments: sAMAccountName=" + sAMAccountName + ". With message = " + msg);
                        }

                        log.Information("Invoking create powershell on " + sAMAccountName + " completed");
                    }
                }
                else
                {
                    log.Warning("No powershell script configured - skipping execution of powershell");
                }
            }
            catch (Exception e)
            {
                log.Warning(e, "running powershell failed");
                if (throwOnFailure)
                {
                    throw e;
                }
            }

            return success;
        }
    }
}