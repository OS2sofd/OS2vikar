using Newtonsoft.Json;
using Serilog;
using System;
using System.Dynamic;
using WebSocketSharp;

namespace OS2Vikar
{
    public class WSCommunication
    {
        private static string VERSION = "1.9.2";

        private WebSocket socket;
        private ADStub adStub;
        private ILogger logger;
        private Settings settings;
        private HMacUtil hmacUtil;

        public WSCommunication(ADStub adStub, ILogger logger, Settings settings, HMacUtil hmacUtil)
        {
            this.logger = logger;
            this.adStub = adStub;
            this.settings = settings;
            this.hmacUtil = hmacUtil;

            socket = new WebSocket(settings.GetStringValue("webSocketUrl"));
            socket.SslConfiguration.EnabledSslProtocols = System.Security.Authentication.SslProtocols.Tls12;
        }

        public void Connect()
        {
            if (!socket.IsAlive)
            {
                logger.Information("Attempting to connect");
                socket.Connect();
            }
        }

        public void Disconnect()
        {
            logger.Debug("Manual disconnect performed");
            socket.Close();
        }

        public void Init(WebSocketSharp.LogLevel logLevel = WebSocketSharp.LogLevel.Info)
        {
            socket.OnMessage += (sender, e) =>
            {
                logger.Debug("Message received");

                if (e.IsText)
                {
                    try
                    {
                        dynamic message = JsonConvert.DeserializeObject(e.Data);

                        LogRequest(message);

                        if (!ValidateMessage(message))
                        {
                            logger.Error("Invalid signature on message: " + message.transactionUuid);
                            return;
                        }

                        string command = (string)message.command;
                        switch (command)
                        {
                            case "AUTHENTICATE":
                                Reply((string)message.transactionUuid, (string)message.command, null, true);
                                break;

                            case "SET_PASSWORD":
                                var changePasswordResponse = adStub.ChangePassword((string)message.target, (string)message.payload);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, changePasswordResponse.Success, changePasswordResponse.Message);
                                break;

                            case "SET_AUTHORIZATION_CODE":
                                var setAutorizationCodeResponse = adStub.SetAuthorizationCode((string)message.target, (string)message.payload);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, setAutorizationCodeResponse.Success, setAutorizationCodeResponse.Message);
                                break;

                            case "CREATE_ACCOUNT":
                                var createAccountResult = adStub.CreateAccount((string)message.target, (string)message.payload);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, createAccountResult.Success, createAccountResult.Message);
                                break;

                            case "SET_EXPIRE":
                                ADStubResponse setExpireResult = null;
                                if (settings.GetBooleanValue("checkStatusWhenSetExpire"))
                                {
                                    string payload = (string)message.payload;
                                    string[] split = payload.Split(',');
                                    setExpireResult = adStub.SetExpire((string)message.target, split[0], Boolean.Parse(split[1]), split[2]);
                                } else
                                {
                                    string payload = (string)message.payload;
                                    setExpireResult = adStub.SetExpire((string)message.target, payload, false, null);
                                }
                                
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, setExpireResult.Success, setExpireResult.Message);
                                break;

                            case "UPDATE_LICENSE":
                                var updateLicenseResult = adStub.UpdateLicense((string)message.target, (string)message.payload);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, updateLicenseResult.Success, updateLicenseResult.Message);
                                break;

                            case "ASSOCIATE_ACCOUNT":
                                var associateAccountResult = adStub.AssociateAccount((string)message.target, (string)message.payload);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, associateAccountResult.Success, associateAccountResult.Message);
                                break;

                            case "DELETE_ACCOUNT":
                                var deleteAccountResult = adStub.DeleteAccount((string)message.target);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, deleteAccountResult.Success, deleteAccountResult.Message);
                                break;

                            case "DISABLE_ACCOUNT":
                                var disableAccountResult = adStub.DisableAccount((string)message.target);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, disableAccountResult.Success, disableAccountResult.Message);
                                break;

                            case "EMPLOYEE_SIGNATURE":
                                var employeeSignatureResult = adStub.AddToEmployeeSignatureGroup((string)message.target);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, employeeSignatureResult.Success, employeeSignatureResult.Message);
                                break;
                            case "AD_GROUPS_SYNC":
                                var adGroupSyncResult = adStub.SyncADGroups((string)message.target, (string)message.payload);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, adGroupSyncResult.Success, adGroupSyncResult.Message);
                                break;
                            case "UNLOCK_ACCOUNT":
                                var unlockAccountResponse = adStub.UnlockAccount((string)message.target);
                                Reply((string)message.transactionUuid, (string)message.command, (string)message.target, unlockAccountResponse.Success, unlockAccountResponse.Message);
                                break;

                            default:
                                logger.Error("Unknown request: " + message.command);
                                break;
                        }
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex, "Get error on request");
                    }
                }
                else
                {
                    logger.Warning("Got non-text message: binary=" + e.IsBinary + ", ping=" + e.IsPing);
                }
            };

            socket.OnClose += (sender, e) =>
            {
                logger.Information("Connection closed. Reason=" + e.Reason + ", code=" + e.Code);
            };

            socket.OnError += (sender, e) =>
            {
                logger.Error(e.Exception, "Error on connection: " + e.Message);
            };

            Connect();
        }

        private bool ValidateMessage(dynamic message)
        {
            string command = (string)message.command;

            switch (command)
            {
                case "AUTHENTICATE":
                    {
                        string mac = hmacUtil.Encode(((string)message.transactionUuid + "." + (string)message.command));
                        return Equals(mac, (string)message.signature);
                    }

                case "CREATE_ACCOUNT":
                case "SET_EXPIRE":
                case "UPDATE_LICENSE":
                case "ASSOCIATE_ACCOUNT":
                case "SET_PASSWORD":
                case "DELETE_ACCOUNT":
                case "DISABLE_ACCOUNT":
                case "EMPLOYEE_SIGNATURE":
                case "SET_AUTHORIZATION_CODE":
                case "AD_GROUPS_SYNC":
                case "UNLOCK_ACCOUNT":
                    {
                        string mac = hmacUtil.Encode(((string)message.transactionUuid + "." + (string)message.command + "." + (string)message.target + "." + (string)message.payload));
                        return Equals(mac, (string)message.signature);
                    }

                default:
                    logger.Information("Unknown command: " + (string) message.command);
                    break;
            }

            return false;
        }

        internal void Reply(string transactionUuid, string command, string target, bool valid, string message = null)
        {
            dynamic response = new ExpandoObject();
            response.transactionUuid = transactionUuid;
            response.command = command;
            response.target = target;
            response.status = (valid) ? "true" : "false";
            response.clientVersion = VERSION;

            if ("AUTHENTICATE".Equals(command))
            {
                response.signature = hmacUtil.Encode(transactionUuid + "." + command + "." + (valid ? "true" : "false"));
            }
            else
            {
                response.signature = hmacUtil.Encode(transactionUuid + "." + command + "." + target + "." + (valid ? "true" : "false"));
            }

            // message is not under signature - used for debugging only
            if (message != null)
            {
                response.message = message;
            }

            socket.Send(JsonConvert.SerializeObject(response));

            LogResponse(response);
        }

        private void LogResponse(dynamic response)
        {
            logger.Information("Response for " + response.command + " (" + response.transactionUuid + "): target=" + response.target + ", result=" + response.status);
        }

        private void LogRequest(dynamic request)
        {
            if ("AUTHENTICATE".Equals(request.command))
            {
                logger.Information("Request for " + request.command + " (" + request.transactionUuid + ")");
            }
            else
            {
                logger.Information("Request for " + request.command + " (" + request.transactionUuid + "): target=" + request.target);
            }
        }
    }
}
