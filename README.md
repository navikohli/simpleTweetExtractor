# Simple Twitter Crawler

#### How to run?
* Import the schema (schema.mwb) into your DB
* Amend the jdbc connection string in ```settings.properties``` & your ```pom.xml```
* Create a jar using ```mvn clean package assembly:assembly```
* Make sure that you have a ```twitter4j.properties``` file with your twitter API keys in the folder you want to run the app from (together with the ```settings.properties```)

