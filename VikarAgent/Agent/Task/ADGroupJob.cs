using Quartz;
using Serilog;

namespace OS2Vikar
{
    [DisallowConcurrentExecution]
    public class ADGroupJob : IJob
    {
        public OS2VikarService os2vikar { get; set; }
        private ILogger logger { get; set; }

        public System.Threading.Tasks.Task Execute(IJobExecutionContext context)
        {
            try
            {
                os2vikar.SyncADGroups();
            }
            catch (System.Exception ex)
            {
                logger.Error(ex, "Failed to sync groups");
            }

            return System.Threading.Tasks.Task.CompletedTask;
        }
    }
}
