using Serilog;
using System;
using System.Collections.Generic;
using System.DirectoryServices;
using System.DirectoryServices.AccountManagement;
using System.Linq;

namespace OS2Vikar
{
    public class ADStub
    {
        private ILogger logger;
        private Settings settings;
        private ExchangeService exchangeService;
        private readonly string managerDN;
        private readonly List<string> securityGroupsOnCreate;
        private readonly List<string> securityGroupsOUDNs;

        private ActiveDirectoryPowershellRunner activeDirectoryPowershellRunner = new ActiveDirectoryPowershellRunner("InternalPowershell\\createUser.ps1");

        public ADStub(ILogger logger, Settings settings)
        {
            this.settings = settings;
            this.logger = logger;
            this.exchangeService = new ExchangeService(
                settings.GetStringValue("emailServer"),
                settings.GetStringValue("emailOnlineDomain"),
                settings.GetBooleanValue("emailOnlineEnabled"),
                settings.GetBooleanValue("emailUsePsSnapIn"),
                logger);

            this.managerDN = settings.GetStringValue("managerDN");
            this.securityGroupsOUDNs = new List<string>();
            string securityGroupsOUDN = settings.GetStringValue("syncADGroupsOUDN");
            if (!string.IsNullOrEmpty(securityGroupsOUDN))
            {
                this.securityGroupsOUDNs.AddRange(securityGroupsOUDN.Split(';'));
            }

            this.securityGroupsOnCreate = new List<string>();
            string securityGroupsOnCreateString = settings.GetStringValue("securityGroupsOnCreate");
            if (!string.IsNullOrEmpty(securityGroupsOnCreateString))
            {
                this.securityGroupsOnCreate.AddRange(securityGroupsOnCreateString.Split(';'));
            }
        }

        public ADStubResponse SetExpire(string userId, string tts, bool performCheck, string cpr)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                bool checkForErrorReason = false;
                using (PrincipalContext ctx = GetPrincipalContext())
                {
                    using (var user = UserPrincipal.FindByIdentity(ctx, IdentityType.SamAccountName, userId))
                    {
                        if (user == null)
                        {
                            if (performCheck)
                            {
                                checkForErrorReason = true;
                            } else
                            {
                                result.Message = "Opdater-kontoudløb fejlede: Der findes ingen AD konto med bruger-id: " + userId;
                            }
                        }
                        else
                        {
                            bool shouldSetExpire = true;
                            if (settings.GetBooleanValue("checkStatusWhenSetExpire"))
                            {
                                if (!user.DistinguishedName.EndsWith(settings.GetStringValue("adUserOu")))
                                {
                                    shouldSetExpire = false;
                                    checkForErrorReason = true;
                                }
                            }

                            if (shouldSetExpire)
                            {
                                using (var entry = (DirectoryEntry)user.GetUnderlyingObject())
                                {
                                    // enable user
                                    int flags = (int)entry.Properties["useraccountcontrol"].Value;
                                    entry.Properties["useraccountcontrol"].Value = (flags & ~0x0002);

                                    DateTime ts = DateTime.Parse((tts + " 00:00")).AddDays(1);
                                    logger.Information("Setting expire on " + userId + " to " + ts.ToString("yyyy-MM-dd HH:mm"));

                                    entry.Properties["accountExpires"].Value = ts.ToFileTime().ToString();
                                    entry.CommitChanges();

                                    result.Success = true;
                                }
                            }
                        }
                    }
                }

                if (checkForErrorReason)
                {
                    using (DirectoryEntry entry = GetUserFromCpr(cpr))
                    {
                        if (entry != null)
                        {
                            string newAccount = entry.Properties["SamAccountName"].Value.ToString();
                            result.Message = "Opdater-kontoudløb fejlede: vikarkontoen er lukket (" + userId + ") og personen har fået en ny AD konto: " + newAccount;
                        }
                        else
                        {
                            result.Message = "Opdater-kontoudløb fejlede: vikarkontoen er lukket: " + userId;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to set expire for user " + userId + " with " + tts.ToString());
                result.Message = "Opdater-kontoudløb fejlede: " + ex.Message;
            }

            return result;
        }

        public ADStubResponse UpdateLicense(string userId, string shouldHaveLicense)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                if ("true".Equals(shouldHaveLicense))
                {
                    AddMember(userId, settings.GetStringValue("adLicenseGroup"));
                }
                else
                {
                    RemoveMember(userId, settings.GetStringValue("adLicenseGroup"));
                }

                result.Success = true;
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to update license for user " + userId);
                result.Message = "Licensopdatering fejlede: " + ex.Message;
            }

            return result;
        }

        // payload = navn:tts:cpr
        public ADStubResponse AssociateAccount(string userId, string payload)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                int idx = payload.LastIndexOf(":");
                if (idx <= 0 || idx == (payload.Length - 1))
                {
                    result.Message = "Brugertilknytning fejlede: ugyldigt payload = '" + payload + "'";
                    return result;
                }

                string cpr = payload.Substring(idx + 1);

                string tmp = payload.Substring(0, idx);
                idx = tmp.LastIndexOf(":");
                if (idx <= 0 || idx == (tmp.Length - 1))
                {
                    result.Message = "Brugertilknytning fejlede: ugyldigt payload = '" + payload + "'";
                    return result;
                }

                string tts = tmp.Substring(idx + 1);
                string name = tmp.Substring(0, idx);

                string givenname = name;
                string surname = "Vikar";

                idx = name.LastIndexOf(" ");
                if (idx > 0 && idx < (name.Length - 1))
                {
                    givenname = name.Substring(0, idx);
                    surname = name.Substring(idx + 1);
                }

                using (PrincipalContext ctx = GetPrincipalContext())
                {
                    using (var user = UserPrincipal.FindByIdentity(ctx, IdentityType.SamAccountName, userId))
                    {
                        if (user == null)
                        {
                            result.Message = "Brugertilknytningen fejlede: Der findes ingen AD konto med bruger-id: " + userId;
                        }
                        else
                        {
                            string cprAttribute = settings.GetStringValue("adPropertyCpr");

                            using (var entry = (DirectoryEntry) user.GetUnderlyingObject())
                            {
                                logger.Information("Setting displayName to " + name + " on " + userId);

                                entry.InvokeSet("givenName", givenname);
                                entry.InvokeSet("sn", surname);
                                entry.InvokeSet("displayName", name);
                                entry.Properties[cprAttribute].Value = cpr;

                                DateTime ts = DateTime.Parse((tts + " 00:00")).AddDays(1);
                                logger.Information("Setting expire on " + userId + " to " + ts.ToString("yyyy-MM-dd HH:mm"));

                                int flags = (int)entry.Properties["useraccountcontrol"].Value;
                                entry.Properties["useraccountcontrol"].Value = (flags & ~0x0002);
                                entry.Properties["accountExpires"].Value = ts.ToFileTime().ToString();
                                entry.CommitChanges();

                                entry.Rename("CN=" + name + " (" + userId + ")");
                                entry.CommitChanges();

                                result.Success = true;
                            }

                            // then execute powershell if configured
                            if (settings.GetBooleanValue("runPowerShellOnCreate"))
                            {
                                activeDirectoryPowershellRunner.Run(userId, ctx.ConnectedServer);
                            }
                            else
                            {
                                logger.Information("powershell execution disabled - will not run createUser.ps1");
                            }
                        }
                    }
                }

                string licenseGroupDn = settings.GetStringValue("adLicenseGroup");
                if (!string.IsNullOrEmpty(licenseGroupDn) && IsMember(userId, licenseGroupDn))
                {
                    // enable the mailbox
                    string emailAlias = userId + settings.GetStringValue("emailDomain");
                    try
                    {
                        logger.Information("Attempting to create mailbox '" + emailAlias + "' for AD account " + userId);

                        if (exchangeService.MailboxExists(emailAlias))
                        {
                            logger.Warning($"Mailbox {emailAlias} not enabled because it already exists");

                            // not an error, but we need to store it on backend for debugging purposes
                            result.Message = $"Mailbox {emailAlias} not enabled because it already exists";
                        }
                        else
                        {
                            exchangeService.EnableMailbox(userId, emailAlias);
                        }
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex, "Failed to enable mailbox for " + userId + " with alias " + emailAlias + ". ErrorMessage: " + ex.Message);

                        // this IS an error, so I guess backend should actually log this as ERROR (i.e. a success create with a non-empty message should be error-logged)
                        result.Message = "Failed to enable mailbox for " + userId + " with alias " + emailAlias + ". ErrorMessage: " + ex.Message;

                        // we will not change result.status to failed, because the AD account was still created, and it IS useable (until proven otherwise)
                    }

                    // HACK! When calling the above code, Exchange might set the displayName to a wrong value, so we attempt to set it AGAIN

                    try
                    {
                        logger.Information("Attempting to reset displayName to " + name + " on " + userId + " after enabling mailbox");

                        using (PrincipalContext ctx = GetPrincipalContext())
                        {
                            using (var user = UserPrincipal.FindByIdentity(ctx, IdentityType.SamAccountName, userId))
                            {
                                if (user != null)
                                {
                                    using (var entry = (DirectoryEntry)user.GetUnderlyingObject())
                                    {
                                        entry.InvokeSet("displayName", name);
                                        entry.CommitChanges();
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        logger.Warning("Failed to set displayName on 2nd attempt: " + ex.Message);
                    }
                }
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to associate user " + userId + " with " + payload);
                result.Message = "Brugertilknytningen fejlede: " + ex.Message;
            }

            return result;
        }

        public ADStubResponse SyncADGroups(string userId, string payload)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                List<string> groupsToBeIn = payload.Split(',').ToList();
                using (PrincipalContext context = new PrincipalContext(ContextType.Domain))
                {
                    using (UserPrincipal user = UserPrincipal.FindByIdentity(context, IdentityType.SamAccountName, userId))
                    {
                        if (user == null)
                        {
                            logger.Error("SyncADGroups: No user with userId = " + userId);
                            result.Message = "Håndter AD gruppe medlemskaber fejlede. Kunne ikke finde bruger med brugernavn: " + userId + ".";
                            return result;
                        }

                        var userGroups = user.GetAuthorizationGroups();
                        if (userGroups == null)
                        {
                            logger.Error("SyncADGroups: could not lookup groups for user with userId = " + userId);
                            result.Message = "Håndter AD gruppe medlemskaber fejlede. Kunne ikke finde grupper for brugeren: " + userId + ".";
                            return result;
                        }

                        // which to remove
                        foreach (var userGroup in userGroups)
                        {
                            // skip non-groups
                            if (!(userGroup is GroupPrincipal))
                            {
                                continue;
                            }

                            GroupPrincipal gp = (GroupPrincipal) userGroup;
                            if (gp?.DistinguishedName == null)
                            {
                                continue;
                            }

                            bool legalGroup = false;

                            foreach (string securityGroupsOUDN in securityGroupsOUDNs)
                            {
                                if (gp.DistinguishedName.ToLower().Contains(securityGroupsOUDN.ToLower()))
                                {
                                    legalGroup = true;
                                    break;
                                }
                            }

                            if (legalGroup)
                            {
                                string groupUuid = gp.Guid.ToString();

                                if (!groupsToBeIn.Contains(groupUuid))
                                {
                                    logger.Information("Removing " + userId + " from " + gp.Name);
                                    gp.Members.Remove(user);
                                    gp.Save();
                                }
                            }
                        }

                        // which to add
                        foreach (var groupToBeIn in groupsToBeIn)
                        {
                            GroupPrincipal gp = GroupPrincipal.FindByIdentity(context, IdentityType.Guid, groupToBeIn);
                            if (gp == null)
                            {
                                logger.Warning("Unable to find Group with ObjectGuid = " + groupToBeIn + " when updating groups for " + userId);
                                continue;
                            }

                            if (!user.IsMemberOf(gp))
                            {
                                logger.Information("Adding " + userId + " to " + gp.Name);

                                gp.Members.Add(user);
                                gp.Save();
                            }
                        }
                    }
                }
                
                result.Success = true;
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to handle indirectly assigned AD groups for AD account " + userId);
                result.Message = "Håndter AD gruppe medlemskaber fejlede. Brugernavn: " + userId + ". " + ex.Message;
            }

            return result;
        }

        public ADStubResponse CreateAccount(string userId, string withLicense)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                using (PrincipalContext ctx = GetPrincipalContext(settings.GetStringValue("adUserOu")))
                {
                    using (var newUser = new UserPrincipal(ctx))
                    {
                        newUser.Name = "Vikar (" + userId + ")";
                        newUser.GivenName = "Vikar";
                        newUser.Surname = userId;
                        newUser.SamAccountName = userId;
                        newUser.SetPassword(Guid.NewGuid().ToString());
                        newUser.AccountExpirationDate = null;
                        newUser.ExpirePasswordNow();
                        newUser.PasswordNotRequired = false;
                        newUser.UserPrincipalName = userId + settings.GetStringValue("emailDomain");
                        newUser.Enabled = true;

                        if (settings.GetBooleanValue("adSetInitialDisplayName"))
                        {
                            newUser.DisplayName = "Vikarkonto";
                        }

                        newUser.Save();

                        if ("true".Equals(withLicense))
                        {
                            AddMember(userId, settings.GetStringValue("adLicenseGroup"));
                        }

                        // add to configured groups
                        foreach (string groupDn in securityGroupsOnCreate)
                        {
                            AddMember(userId, groupDn);
                        }

                        // set manager
                        if (!string.IsNullOrEmpty(managerDN))
                        {
                            DirectoryEntry newUserEntry = newUser.GetUnderlyingObject() as DirectoryEntry;
                            newUserEntry.Properties["manager"].Value = managerDN;
                            newUserEntry.CommitChanges();
                        }
                    }
                }

                result.Success = true;
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to create user with userId: " + userId);
                result.Message = "Oprettelse fejlede: " + ex.Message;
            }

            return result;
        }

        public ADStubResponse ChangePassword(string userId, string newPassword)
        {
            using (PrincipalContext ctx = GetPrincipalContext())
            {
                using (UserPrincipal user = UserPrincipal.FindByIdentity(ctx, userId))
                {
                    if (user != null)
                    {
                        try
                        {
                            // Note that this ONLY works if you are domain joined, and if you are either a domain admin, or
                            // have been granted the Change Password right (and if not a domain admin, the username/password must be supplied,
                            // for reasons unknown)
                            user.SetPassword(newPassword);

                            return new ADStubResponse()
                            {
                                Success = true
                            };
                        }
                        catch (PasswordException ex)
                        {
                            logger.Information(ex, "Error during password validation");

                            return new ADStubResponse()
                            {
                                Success = false,
                                Message = ex.Message
                            };
                        }
                    }
                    else
                    {
                        logger.Warning("Cannot find user '" + userId + "' in Active Directory");
                    }
                }
            }

            return new ADStubResponse()
            {
                Success = true,
                Message = "Could not find user: " + userId
            };
        }

        public ADStubResponse SetAuthorizationCode(string userId, string code)
        {
            using (PrincipalContext ctx = GetPrincipalContext())
            {
                using (UserPrincipal user = UserPrincipal.FindByIdentity(ctx, userId))
                {
                    if (user != null)
                    {
                        try
                        {
                            string authCodeAttribute = settings.GetStringValue("adPropertyAuthorizationCode");
                            using (var entry = (DirectoryEntry)user.GetUnderlyingObject())
                            {
                                entry.Properties[authCodeAttribute].Value = code;
                                entry.CommitChanges();
                            }

                            return new ADStubResponse()
                            {
                                Success = true
                            };
                        }
                        catch (Exception ex)
                        {
                            logger.Information(ex, "Error setting authorization code.");

                            return new ADStubResponse()
                            {
                                Success = false,
                                Message = ex.Message
                            };
                        }
                    }
                    else
                    {
                        logger.Warning("Cannot find user '" + userId + "' in Active Directory");
                    }
                }
            }

            return new ADStubResponse()
            {
                Success = true,
                Message = "Could not find user: " + userId
            };
        }

        public ADStubResponse DeleteAccount(string userId)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                using (PrincipalContext ctx = GetPrincipalContext())
                {
                    using (var user = UserPrincipal.FindByIdentity(ctx, IdentityType.SamAccountName, userId))
                    {
                        if (user == null)
                        {
                            result.Message = "Slet AD konto fejlede: Der findes ingen AD konto med bruger-id: " + userId;
                        }
                        else
                        {
                            user.Delete();
                            result.Success = true;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to delete AD account " + userId);
                result.Message = "Slet AD konto fejlede: " + ex.Message;
            }

            return result;
        }

        public ADStubResponse DisableAccount(string userId)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                using (PrincipalContext ctx = GetPrincipalContext())
                {
                    using (var user = UserPrincipal.FindByIdentity(ctx, IdentityType.SamAccountName, userId))
                    {
                        if (user == null)
                        {
                            result.Message = "Disable AD konto fejlede: Der findes ingen AD konto med bruger-id: " + userId;
                        }
                        else
                        {
                            user.Enabled = false;
                            user.Save();
                            result.Success = true;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to disable AD account " + userId);
                result.Message = "Disable AD konto fejlede: " + ex.Message;
            }

            return result;
        }

        public ADStubResponse AddToEmployeeSignatureGroup(string userId)
        {
            ADStubResponse result = new ADStubResponse();
            result.Success = false;

            try
            {
                var group = settings.GetStringValue("adEmployeeSignatureGroup");
                AddMember(userId, group);
                result.Success = true;
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Failed to add AD account " + userId + " to employee signature group");
                result.Message = "Tilføj AD konto til medarbejdersignaturgruppe fejlede. Brugernavn: " + userId + ". " + ex.Message;
            }

            return result;
        }

        public ADStubResponse UnlockAccount(string userId)
        {
            using (PrincipalContext ctx = GetPrincipalContext())
            {
                using (UserPrincipal user = UserPrincipal.FindByIdentity(ctx, userId))
                {
                    if (user != null)
                    {
                        user.UnlockAccount();

                        return new ADStubResponse()
                        {
                            Success = true
                        };
                    }
                    else
                    {
                        logger.Warning("Cannot find user '" + userId + "' in Active Directory");
                    }
                }
            }

            return new ADStubResponse()
            {
                Success = false,
                Message = "Could not find user: " + userId
            };
        }

        private bool IsMember(string userId, string groupDn)
        {
            using (PrincipalContext context = new PrincipalContext(ContextType.Domain))
            {
                using (UserPrincipal user = UserPrincipal.FindByIdentity(context, IdentityType.SamAccountName, userId))
                {
                    if (user == null)
                    {
                        logger.Error("AddMember: No user with userId = " + userId);
                        return false;
                    }

                    using (GroupPrincipal group = GroupPrincipal.FindByIdentity(context, IdentityType.DistinguishedName, groupDn))
                    {
                        if (group == null)
                        {
                            logger.Error("IsMember: No group with DN " + groupDn);
                            return false;
                        }

                        return group.Members.Contains(user);
                    }
                }
            }
        }

        private bool AddMember(string userId, string groupDn)
        {
            using (PrincipalContext context = new PrincipalContext(ContextType.Domain))
            {
                using (UserPrincipal user = UserPrincipal.FindByIdentity(context, IdentityType.SamAccountName, userId))
                {
                    if (user == null)
                    {
                        logger.Error("AddMember: No user with userId = " + userId);
                        return false;
                    }

                    using (GroupPrincipal group = GroupPrincipal.FindByIdentity(context, IdentityType.DistinguishedName, groupDn))
                    {
                        if (group == null)
                        {
                            logger.Error("AddMember: No group with DN " + groupDn);
                            return false;
                        }

                        try
                        {
                            group.Members.Add(user);
                            group.Save();

                            logger.Information("Added " + userId + " to " + group.Name);
                        }
                        catch (PrincipalExistsException)
                        {
                            logger.Warning("User " + userId + " was already a member of the group");
                        }
                    }
                }
            }

            return true;
        }

        private bool RemoveMember(string userId, string groupDn)
        {
            using (PrincipalContext context = new PrincipalContext(ContextType.Domain))
            {
                using (UserPrincipal user = UserPrincipal.FindByIdentity(context, IdentityType.SamAccountName, userId))
                {
                    if (user == null)
                    {
                        logger.Error("RemoveMember: No user with userId = " + userId);
                        return false;
                    }

                    using (GroupPrincipal group = GroupPrincipal.FindByIdentity(context, IdentityType.DistinguishedName, groupDn))
                    {
                        if (group == null)
                        {
                            logger.Error("RemoveMember: No group with DN " + groupDn);
                            return false;
                        }

                        if (group.Members.Remove(user))
                        {
                            logger.Information("Removed " + userId + " from " + group.Name);
                            group.Save();
                        }
                        else
                        {
                            logger.Warning("Could not remove " + userId + " from " + group.Name);
                        }
                    }
                }
            }

            return true;
        }

        private PrincipalContext GetPrincipalContext(string ldapPath = null)
        {
            if (ldapPath != null)
            {
                return new PrincipalContext(ContextType.Domain, null, ldapPath);
            }

            return new PrincipalContext(ContextType.Domain);
        }

        public List<ADGroupSyncDto> GetGroupsFromOu()
        {
            List<ADGroupSyncDto> groups = new List<ADGroupSyncDto>();

            foreach (string securityGroupsOUDN in securityGroupsOUDNs)
            {
                using (DirectoryEntry securityGroupsOUEntry = new DirectoryEntry(@"LDAP://" + securityGroupsOUDN))
                {
                    if (securityGroupsOUEntry.Children != null)
                    {
                        foreach (DirectoryEntry child in securityGroupsOUEntry.Children)
                        {
                            AddGroupsRecursive(groups, child);
                            child.Close();
                        }
                    }
                }
            }

            return groups;
        }

        private void AddGroupsRecursive(List<ADGroupSyncDto> groups, DirectoryEntry currentEntry)
        {
            // if entry is group, add to list
            if (currentEntry.Properties["objectClass"]?.Contains("group") == true)
            {
                ADGroupSyncDto dto = new ADGroupSyncDto();
                dto.Name = currentEntry.Name.Replace("CN=", "");
                dto.ObjectGuid = currentEntry.Guid.ToString();
                groups.Add(dto);
            }

            if (currentEntry.Children != null)
            {
                foreach (DirectoryEntry child in currentEntry.Children)
                {
                    AddGroupsRecursive(groups, child);
                    child.Close();
                }
            }
        }

        public DirectoryEntry GetUserFromCpr(string cpr)
        {
            var filter = string.Format("(&(objectClass=user)(objectClass=person)({0}={1}))", settings.GetStringValue("adPropertyCpr"), cpr);
            return SearchForDirectoryEntry(filter);
        }

        private DirectoryEntry SearchForDirectoryEntry(string filter)
        {
            using (DirectoryEntry entry = new DirectoryEntry()) {
                using (DirectorySearcher search = new DirectorySearcher(entry))
                {
                    search.Filter = filter;

                    var result = search.FindOne();
                    if (result != null)
                    {
                        return result.GetDirectoryEntry();
                    }

                    return null;
                }
            }
        }
    }
}
