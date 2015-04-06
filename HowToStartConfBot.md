# Introduction #

This page explains how you can run your own conference bot.

# Prereqs #

  1. Create a new gtalk account and add all users that wish to participate to its buddy list.
  1. Ask users to add gtalk account to their buddy list to start participating in the conference.
  1. Get the latest build versions from [here](http://conf-bot.googlecode.com/files/dist-latest.zip).
  1. You will need J2SE 5 or above to run this application. Get the latest Java SE [here](http://java.com/en/download/)

# Details #

First Unzip the archive downloaded from [here](http://conf-bot.googlecode.com/files/dist-latest.zip) and go to the dist folder.

```
cd dist
```

To run the project from the command line, type the following:

```
java -jar ConfBot.jar your-bot-account@gmail.com google-account-password choose-an-admin-password
```

To distribute this project, zip up the dist folder (including the lib folder)
and distribute the ZIP file.