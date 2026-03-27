#define MyAppName "Rill"
#ifndef MyAppVersion
  #define MyAppVersion "0.0.0"
#endif
#ifndef SourceDir
  #define SourceDir "."
#endif

[Setup]
AppId={{7A1E6F6C-6D36-45F5-92C0-62AFB640E1D5}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher=Indolyn
DefaultDirName={autopf}\Rill
DefaultGroupName=Rill
DisableProgramGroupPage=yes
OutputDir={#SourceDir}\output
OutputBaseFilename=rill-{#MyAppVersion}-windows-x64-setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible

[Types]
Name: "default"; Description: "Default installation"
Name: "full"; Description: "Full installation"
Name: "custom"; Description: "Custom installation"; Flags: iscustom

[Components]
Name: "core"; Description: "Core server and runtime"; Types: default full custom; Flags: fixed
Name: "cli"; Description: "CLI client"; Types: default full custom; Flags: fixed
Name: "mysqlcompat"; Description: "MySQL/Navicat compatibility server"; Types: full custom
Name: "gui"; Description: "GUI client"; Types: full custom

[Files]
Source: "{#SourceDir}\runtime\*"; DestDir: "{app}\runtime"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "{#SourceDir}\bin\*"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "{#SourceDir}\server\rill-server.jar"; DestDir: "{app}\server"; Components: core; Flags: ignoreversion
Source: "{#SourceDir}\server\rill-mysql-server.jar"; DestDir: "{app}\server"; Components: mysqlcompat; Flags: ignoreversion
Source: "{#SourceDir}\client\rill-cli.jar"; DestDir: "{app}\client"; Components: cli; Flags: ignoreversion
Source: "{#SourceDir}\client\rill-gui.jar"; DestDir: "{app}\client"; Components: gui; Flags: ignoreversion

[Icons]
Name: "{group}\Rill CLI"; Filename: "{app}\bin\rill-cli.cmd"; Components: cli
Name: "{group}\Rill GUI"; Filename: "{app}\bin\rill-gui.cmd"; Components: gui
Name: "{group}\Rill Server"; Filename: "{app}\bin\rill-server.cmd"; Components: core
Name: "{group}\Rill MySQL Compatible Server"; Filename: "{app}\bin\rill-mysql.cmd"; Components: mysqlcompat

[Run]
Filename: "{app}\bin\rill-gui.cmd"; Description: "Launch Rill GUI"; Flags: nowait postinstall skipifsilent; Components: gui
