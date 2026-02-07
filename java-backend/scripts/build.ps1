$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
cd 'e:\Download\code\java-backend'
mvn clean package
