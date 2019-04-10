# Who we are
[Lorenzo Leschiera](https://www.linkedin.com/in/lorenzo-leschiera-55a706156/)
[Matte Rizza](https://www.linkedin.com/in/matteo-rizza/)
[Shend Osmanaj](https://www.linkedin.com/in/shendosmanaj/)
[Link](https://www.hackster.io/149963/monitoring-workers-stress-levels-87f46a) for Hackster
[Link](https://www.slideshare.net/LorenzoLeschiera/monitoring-workersstress-levels) for SlideShare

# Stress-Detector
An IoT solution that reduces the risk of industrial accidents and health hazards, reduces costs and improves productivity.

The basic idea is to use a sensor to detect heartbeat and analyze data received through cloud computing, to promptly report heartbeats or abnormalities.

This proof of concept use the AWS IoT APIs to securely publish-to and subscribe-from MQTT topics. It uses Cognito federated identities in conjunction with AWS IoT to create a client certificate and private key and store it in a local Java keystore. This identity is then used to authenticate to AWS IoT. 

## Requirements
- Polar Heart Rate OH1 Sensor
- AndroidStudio
- Android API 10 or greater

## Using the project
1. Import the project into Android Studio.

   - From the Welcome screen, click on "Import project".
   - Browse to the AndroidPubSub directory and press OK.
   - Accept the messages about adding Gradle to the project.
   - If the SDK reports some missing Android SDK packages (like Build Tools or the Android API package), follow the instructions to install them.
2. Import the libraries :

   - Gradle will take care of downloading these dependencies automatically for you.
3. This sample will create a certificate and key, save it in the local java key store and upload the certificate to the AWS IoT platform. To upload the certificate, it requires a Cognito Identity with access to AWS IoT to upload the device certificate. Use Amazon Cognito to create a new identity pool ( or you can reuse an identity pool that you previously created):

   - In the Amazon Cognito Console, press the Manage Identity Pools button and on the resulting page press the Create new identity pool button.

   - Give your identity pool a name and ensure that Enable access to unauthenticated identities under the Unauthenticated identities         section is checked. This allows the sample application to assume the unauthenticated role associated with this identity pool. Press       the Create Pool button to create your identity pool
   
      - As part of creating the identity pool, Cognito will setup two roles in Identity and Access Management (IAM). 
     These will be named something similar to: Cognito_<<PoolName>>Auth_Role and Cognito_<<PoolName>>Unauth_Role. 
     You can view them by pressing the View Details button on the console. Now press the Allow button to create the roles.
   - Note the Identity pool ID value that shows up in red in the "Getting started with Amazon Cognito" page. It should look similar to: `us-east-1:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx". Also, note the region that is being used. These will be used in the application code later.

    - Next, we will attach a policy to the unauthenticated role to setup permissions to access the required AWS IoT APIs. This is done by first creating the IAM Policy shown below in the IAM Console and then attaching it to the unauthenticated role. In the IAM console, Search for the pool name that you created and click on the link for the unauth role. 
Click on the "Add inline policy" button and add the following policy using the JSON tab. Click on "Review Policy", give the policy a descriptive name and then click on "Create Policy". This policy allows the sample app to create a new certificate (including private key) and attach a policy to the certificate.
``` 
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "iot:AttachPrincipalPolicy",
        "iot:CreateKeysAndCertificate"
      ],
      "Resource": [
        "*"
      ]
    }
  ]
}
```
The configuration we have setup up to this point will enable the Sample App to connect to the AWS IoT platform using Cognito and upload certificates and policies. Next, we will need to create a policy, that we will attach to the Device Certificate that will authorize the certificate to connect to the AWS IoT message broker and perform publish, subscribe and receive operations. To create the policy in AWS IoT,

Navigate to the AWS IoT Console and press the Get Started button. On the resulting page click on Secure on the side panel and the click on Policies.

Click on Create

Give the policy a name. Note this name as you will use it in the application when making the attach policy API call.

Click on Advanced Mode and replace the default policy with the following text and then click the Create button.
```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "iot:Connect",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iot:Publish",
        "iot:Subscribe",
        "iot:Receive"
      ],
      "Resource": "*"
    }
  ]
}
```
Note: To keep things simple, This policy allows access to all the topics under your AWS IoT account. This can be used for getting started and prototypes. In product, you should scope this policy down to specific topics, specify them explicitly as ARNs in the resource section: "Resource": "arn:aws:iot:<REGION>:<ACCOUNT ID>:topic/<<mytopic/mysubtopic>>".

Open the project.

Open AWSIoT.java and update the following constants:
```
MY_REGION = Regions.US_EAST_1;
```
This would be the name of the IoT region that you noted down previously.

```
CUSTOMER_SPECIFIC_ENDPOINT = "<CHANGE_ME>";
```
The customer specific endpoint can be found on the IoT console settings page. Navigate to the AWS IoT Console and press the Settings button.

```AWS_IOT_POLICY_NAME = "CHANGE_ME";```
This would be the name of the AWS IoT policy that you created previously.

```
KEYSTORE_NAME = "iot_keystore";
KEYSTORE_PASSWORD = "password";
CERTIFICATE_ID = "default";
```
For these parameters, the default values will work for the sample application. The keystore name is the name used when writing the keystore file to the application's file directory. The password is the password given to protect the keystore when written. Certificate ID is the alias in the keystore for the certificate and private key entry.

Note: If you end up creating a keystore off of the device you will need to update this to match the alias given when importing the certificate into the keystore.

Open res/raw/awsconfiguration.json and update the values for PoolId with the ID of the Cognito Identity Pool created above and Region with the region of the Cognito Identity Pool created above (for example us-east-1):
```
"PoolId": "REPLACE_ME",
"Region": "REPLACE_ME"
```
Build and run the sample app.

