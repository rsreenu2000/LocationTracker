# LocationTracker

This project implements a simple end-to-end system that allows location sharing to/receiving from the Cloud, using AWS IoT and Google Cloud Platform to host the Server API's and communication protocols MQTT, HTTP and Push Notification.

# Project Description / Requirements:

## Android Mobile App:

To make it simple, the same app will be the sender (send location info to the cloud) and receiver (receive the location updates from the cloud), so it doesn't need to handle multiple phones.

As Sender:

In background mode, the app should report it's current location (latitude, longitude, accuracy) to the cloud every 5 minutes (even when it's not running in the foreground). When the user launches the app (foreground mode), it has a very simple UI with a button that, when clicked, gets the current phone location and sends to the cloud (in addition to the regular periodic location sharing done in background).

The app should use MQTT protocol to send the location info to the cloud (as described below in AWS IoT), using a JSON payload - example:

`{
    "latitude": 37.427440,
    "longitude": -122.169118,
    "accuracy": 18
}`

As Receiver:

The app should also handle a push notification that will be sent by the Cloud (implemented using FCM) whenever there is a location shared to the cloud, and show a notification on the notification curtain with the location info received.  When user clicks this notification from the notification curtain, it should launch the app and show the location info received in the UI. If the app is already in the foreground when the notification is received, it should not add a notification in the curtain, just show the received location info in the UI.

## Cloud: AWS IoT

Implement a topic in AWS IoT to handle the location message coming from the phone and a Lambda function (implemented in Java) to be notified whenever there is a message sent to that topic. This lambda function should extract the JSON payload sent by the phone, make sure it's valid (contains the fields listed above) and send it to a RESTful API implemented on Google Platform using HTTP POST request.

## Cloud: Google Platform

Implement a RESTful API to be reached by a HTTP POST request to receive the location information sent by AWS IoT (as described above). Upon receiving a location info, this API should invoke a Google Places API to get the place name related to the received latitude/longitude (get the address if place name is not available), and then send a push notification to the phone with the same information including the place - example:

`{
    "latitude": 37.427440,
    "longitude": -122.169118,
    "accuracy": 18,
    "place": "Stanford University"
}`

Upon receiving this push notification, the phone should react as described in the Mobile App section.


## Additional - Thread producer/consumer

When the app is running in the foreground, it should have an internal producer/consumer mechanism (using thread and wait/notify) to add an extra tag (some string) to the location information received in the notification. This producer/consumer should work as follows:

- It has an internal list, let's call it "tag-list", with enforced capacity of up to 5 elements (location info)
- It has a queue (another list without limit, let's call it "wait-list") where new locations received are added
- It has a Thread, let's call it "move-thread", which moves location info elements from the "wait-list" to the "tag-list" whenever there is available room in the "tag-list"

When a new element is added to the "tag-list", it should be tagged (a "tag" field added to the location info JSON with some unique string).

When a new location info is received in the push notification, it should be added to the "wait-list"
The "move-thread" should be always running moving elements from the "wait-list" to the "tag-list" when possible. If it tries to move and element and the "tag-list" is full (already has 5 elements), then the "move-thread" should go to the 'wait' state.

There should be a button on the UI to consume elements from the "tag-list", and show the info in the UI (the info displayed in the UI should show the location info and the added tag).

When an element is "consumed" from the tag-list, if the tag-list was full, it should notify the waiting thread, so it will wake up and try to add move elements to the room that was just made available (if there are elements waiting in the wait-list).
