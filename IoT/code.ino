#include <WiFi.h>
#include <DHT.h>
#include <ESP_Mail_Client.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "time.h"

#define SMTP_server "smtp.gmail.com"
#define SMTP_Port 465
#define sender_email "nguyendinhquang241003@gmail.com"
#define sender_password "qcrl ofeu ooge mcis"
#define Recipient_email "ttkayn@gmail.com"
#define Recipient_name ""
#define MQ2_A 35 // define MQ2 analog pin
#define MQ2_D 14
#define DPIN 15       // Pin to connect DHT sensor (GPIO number)
#define DTYPE DHT11
#define LED 32
#define BUZZLE 33
#define motor1 26
#define motor2 27
DHT dht(DPIN, DTYPE);
int A_value, D_value;
unsigned long epochTime;
unsigned long dataMillis = 0;

int motor = 25;
int sosdata = 0;
int sostemperature = 0;
WiFiClient client;
HTTPClient http;
const char *ssid = "NT";
const char *pass = "12345678";
const char *ntpServer = "pool.ntp.org";
const char *serverName = "https://ap-southeast-1.aws.data.mongodb-api.com/app/application-0-trdhh/endpoint/sensor";
const char *getdatatemperature;
const char *getdatagas;
StaticJsonDocument<500> doc;
SMTPSession smtp;

void setup()
{
  dht.begin();
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  pinMode(motor, OUTPUT);
  pinMode(MQ2_A, INPUT);
  pinMode(MQ2_D, INPUT);
  WiFi.begin(ssid, pass);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED)
  {
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());
  Serial.println();
  configTime(0, 0, ntpServer);
}

void loop()
{
  A_value = analogRead(MQ2_A);
  disableCore1WDT();
  // D_value = digitalRead(MQ2_D);
  if (millis() - dataMillis > 15000 || dataMillis == 0)
  {
    dataMillis = millis();
    epochTime = getTime();
    float humidity = dht.readHumidity();
    float temperature = dht.readTemperature(false);
    Serial.printf("Analog Value: %d\n", A_value);
    Serial.printf("Digital Value: %d\n", D_value);
    Serial.print("Epoch Time: ");
    Serial.print("\t");
    Serial.println(epochTime);
    Serial.print("Temperature: ");
    Serial.print("\t");
    Serial.print(temperature);
    Serial.print("Humidity: ");
    Serial.print("\t");
    Serial.print(humidity);
    Serial.print("Gas:");
    Serial.print("\t");
    Serial.print(A_value);
    if (A_value > sosdata || temperature > sostemperature)
    {
      Serial.println("Gas");

      digitalWrite(LED, HIGH);
      digitalWrite(BUZZLE, HIGH);
      digitalWrite(motor, HIGH);
      digitalWrite(motor1, HIGH);
      digitalWrite(motor2, LOW);
      sendemail();
    }
    else
    {
      Serial.println("No Gas");
      digitalWrite(LED, LOW);
      digitalWrite(BUZZLE, LOW);
      digitalWrite(motor, LOW);
      digitalWrite(motor1, LOW);
      digitalWrite(motor2, LOW);
    }
    doc["sensors"]["temperature"] = temperature;
    doc["sensors"]["humidity"] = humidity;
    doc["sensors"]["Gas"] = A_value;
    Serial.print("\t");
    Serial.println("Uploading data... ");
    POSTData();
    receiveData();
  }
}

unsigned long getTime()
{
  time_t now;
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo))
  {
    return 0;
  }
  time(&now);
  return now;
}

void POSTData()
{
  if (WiFi.status() == WL_CONNECTED)
  {
    HTTPClient http;
    http.begin(serverName);
    http.addHeader("Content-Type", "application/json");
    http.addHeader("Accept", "application/json");
    http.addHeader("X-Forwarded-Proto", "http");

    String json;
    serializeJson(doc, json);

    Serial.println(json);

    int httpResponseCode = http.POST(json);

    Serial.println(httpResponseCode);

    if (httpResponseCode == 200)
    {
      Serial.println("Data uploaded.");
      delay(200);
    }
    else
    {
      Serial.println("ERROR: Couldn't upload data.");1
      delay(200);
    }
  }
}

void receiveData()
{
  Serial.println("Getdata:");
  HTTPClient http;
  http.begin("https://ap-southeast-1.aws.data.mongodb-api.com/app/application-0-trdhh/endpoint/g");
  int httpResponseCode = http.GET();
  DynamicJsonDocument doc(1024);
  if (httpResponseCode == HTTP_CODE_OK)
  {
    String response = http.getString();
    Serial.println("Received data:");
    Serial.println(response);

    DeserializationError error = deserializeJson(doc, response);

    if (!error)
    {
      getdatatemperature = doc[0]["data"]; // Assuming the field name is empty
      Serial.println(getdatatemperature);
      getdatagas = doc[1]["data"]; // Assuming the field name is empty
      Serial.println(getdatagas);
      sosdata = atoi(getdatagas);
      sostemperature = atoi(getdatatemperature);
    }
  }

  http.end();
}

void sendemail(){
  smtp.debug(1);
  ESP_Mail_Session session;
  session.server.host_name = SMTP_server ;
  session.server.port = SMTP_Port;
  session.login.email = sender_email;
  session.login.password = sender_password;
  session.login.user_domain = "";
  SMTP_Message message;
  message.sender.name = "ESP 32";
  message.sender.email = sender_email;
  message.subject = "SOS";
  message.addRecipient(Recipient_name,Recipient_email);
  String htmlMsg = "<div style=\"color:#000000;\"><h1> SOS</h1><p>Your house is on fire. Please take immediate action!</p></div>";
  message.html.content = htmlMsg.c_str();
  message.html.content = htmlMsg.c_str();
  message.text.charSet = "us-ascii";
  message.html.transfer_encoding = Content_Transfer_Encoding::enc_7bit;
  if (!smtp.connect(&session))
    return;
  if (!MailClient.sendMail(&smtp, &message))
    Serial.println("Error sending Email, " + smtp.errorReason());
}
