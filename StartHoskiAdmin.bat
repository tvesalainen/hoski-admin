SET PATH=%PATH%;C:\Program Files (x86)\Java\jre7\bin

java -Xmx512m -verbose:gc -classpath lib/appengine-api.jar;lib/appengine-remote-api.jar;lib/HoskiLib.jar;opencsv-2.3.jar -jar HoskiAdmin.jar hoskiadmin.properties