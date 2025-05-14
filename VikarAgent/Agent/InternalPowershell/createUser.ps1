function Invoke-Method {
	param(
        [string] $SAMAccountName = $(throw "Please specify a sAMAccountName."),
        [string] $DomainController = $(throw "Please specify a DomainController.")
	)
	
	$result = "Creating " + $SAMAccountName + $DomainController

	$result | Out-File 'c:\logs\log.txt'
}