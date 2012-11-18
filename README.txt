
Runtime installation
====================

Launch the application with Java Web Start from 

https://hsk-members.appspot.com/launch.html

Applications that needs all rights after launched with Web Start,
must have their jar files signed. HoskiAdmin jars are signed with self-signed 
certificate. Because of self-signing, user is notified of potential security
risks when starting the application.

Key tool commands used in creating HoskiKeystore:

keytool -genkey -keystore HoskiKeystore -alias hoski

keytool -selfcert -alias hoski -keystore HoskiKeystore -validity 10000

Development
===========

Needed jar files are in installation directorys ./lib subdirectory.

HoskiAdmin source files are in https://github.com/tvesalainen/hoski-admin.git

HoskiLib source files are in https://github.com/tvesalainen/hoski-lib.git

Good Luck!

