using StructureMap;

namespace OS2Vikar
{
    public class DefaultRegistry : Registry
    {
        public DefaultRegistry()
        {
            Scan(_ =>
            {
                _.WithDefaultConventions();
            });

            For<WSCommunication>().Singleton();
            For<OS2VikarService>().Singleton();

            Policies.FillAllPropertiesOfType<WSCommunication>();
            Policies.FillAllPropertiesOfType<OS2VikarService>();
            Policies.FillAllPropertiesOfType<ADStub>();
            Policies.FillAllPropertiesOfType<ReconnectJob>();
            Policies.FillAllPropertiesOfType<ADGroupJob>();

            Policies.Add<LoggingForClassPolicy>();
        }
    }
}