# 🩸 BloodBridge - Blood Donation App

<div align="center">

### ❤️ Connecting Donors. Saving Lives.

*A modern Android application that bridges the gap between blood donors, recipients, and hospitals through secure authentication, real-time communication, and cloud-based services.*

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge\&logo=android\&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge\&logo=kotlin\&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge\&logo=firebase\&logoColor=black)
![Google Maps](https://img.shields.io/badge/Google%20Maps-4285F4?style=for-the-badge\&logo=googlemaps\&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge\&logo=androidstudio\&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge\&logo=materialdesign\&logoColor=white)

</div>

---

# 📑 Table of Contents

* [📖 Overview](#-overview)
* [🎯 Objectives](#-objectives)
* [✨ Features](#-features)
* [📱 Screenshots](#-screenshots)
* [🛠 Tech Stack](#-tech-stack)
* [💻 Software Requirements](#-software-requirements)
* [📂 Project Structure](#-project-structure)
* [🚀 Future Scope](#-future-scope)

---

# 📖 Overview

BloodBridge is a modern Android application developed to simplify and digitize the blood donation process by connecting donors, recipients, and hospitals through a secure and user-friendly platform.

Traditional blood donation systems often depend on manual records, phone calls, and local donation drives, making it difficult to respond quickly during emergencies. BloodBridge addresses these challenges by leveraging **Kotlin**, **Firebase**, and **Google Maps** to provide real-time communication, cloud-based data storage, and efficient donor management.

The application enables users to register securely, donate blood, request blood during emergencies, maintain donation history, receive instant notifications, and locate suitable donors based on blood group and location. Hospitals can securely manage donor information, verify accounts, publish emergency blood requests, and provide medical reports directly to donors.

Designed with simplicity, reliability, and accessibility in mind, BloodBridge aims to make blood donation faster, more organized, and readily available whenever lives depend on it.

---

# 🎯 Objectives

### 👤 Public User

* 🔐 Authenticate using Email, Google, or Phone Number.
* ❤️ Register as a blood donor.
* 🩸 Request blood during emergencies.
* 📅 View donation history and previous donation records.
* ⏳ Track the next eligible blood donation date.
* 🤖 Access an AI-powered chatbot for instant guidance.
* 📄 View medical reports uploaded by hospitals.

### 🏥 Hospital User

* 🏥 Secure authentication using verified hospital email.
* 🚨 Publish Emergency SOS blood requests.
* 📝 Maintain donor records including age, health details, and screening reports.
* 📄 Upload donation reports accessible by donors.
* ☁ Manage donor information securely using Firebase.

### 🌍 Overall Goals

* 🎨 Deliver a clean and intuitive user interface.
* ⚡ Improve emergency response time.
* 🔒 Ensure secure cloud-based data management.
* 📱 Provide a smooth and reliable Android experience.

---

# ✨ Features

| Feature                    | Description                                                 |
| -------------------------- | ----------------------------------------------------------- |
| 🔐 Multi-Authentication    | Login using Email, Google, or Phone Authentication          |
| ❤️ Blood Donation          | Register as a donor and manage donation records             |
| 🩸 Blood Request           | Search and request blood during emergencies                 |
| 🏥 Hospital Dashboard      | Dedicated interface for hospitals                           |
| 🚨 Emergency SOS           | Hospitals can instantly notify eligible donors              |
| 🤖 AI Chatbot              | Provides guidance and answers common queries                |
| 📍 Google Maps Integration | Locate nearby donors efficiently                            |
| 📄 Medical Reports         | Hospitals upload donor reports accessible through the app   |
| ☁ Firebase Integration     | Secure Authentication, Realtime Database, and Cloud Storage |
| 🔔 Real-Time Updates       | Instant synchronization across all users                    |
| 📊 Donation History        | Maintain complete donation records                          |
| 🎨 Material Design UI      | Modern and responsive Android interface                     |

---

# 📱 Screenshots

<table align="center">
<tr>
<td align="center"><b>Login Options</b></td>
<td align="center"><b>Public User Login</b></td>
<td align="center"><b>Public User Dashboard</b></td>
<td align="center"><b>Public User Side-Menu</b></td>
</tr>

<tr>
<td><img src="assets/screenshots/01_login_options_page.jpg" width="180"></td>
<td><img src="assets/screenshots/02_public_user_login.jpg" width="180"></td>
<td><img src="assets/screenshots/03_public_user_dashboard.jpg" width="180"></td>
<td><img src="assets/screenshots/04_public_user_side_menu.jpg" width="180"></td>
</tr>

<tr>
<td align="center"><b>Public User Blood request</b></td>
<td align="center"><b>Public User Donor Page</b></td>
<td align="center"><b>Public User Chatbot</b></td>
<td align="center"><b>Public User Donation History</b></td>
</tr>

<tr>
<td><img src="assets/screenshots/05_public_user_blood_request.jpg" width="180"></td>
<td><img src="assets/screenshots/06_public_user_donor_page.jpg" width="180"></td>
<td><img src="assets/screenshots/07_public_user_chatbot.jpg" width="180"></td>
<td><img src="assets/screenshots/15_public_user_donation_history.jpg" width="180"></td>
</tr>

<tr>
<td align="center"><b>Hospital User Sign-in</b></td>
<td align="center"><b>Hospital User Details</b></td>
<td align="center"><b>Hospital User Dashboard</b></td>
<td align="center"><b>Hospital User Side-Menu</b></td>
</tr>

<tr>
<td><img src="assets/screenshots/08_hospital_user_signin.jpg" width="180"></td>
<td><img src="assets/screenshots/10_hospital_user_details.jpg" width="180"></td>
<td><img src="assets/screenshots/11_hospital_user_dashboard.jpg" width="180"></td>
<td><img src="assets/screenshots/12_hospital_user_side_menu.jpg" width="180"></td>
</tr>

</table>

---

# 🛠 Tech Stack

| Category                | Technologies                    |
| ----------------------- | ------------------------------- |
| 📱 Programming Language | Kotlin                          |
| 🎨 UI Design            | XML, Material Design Components |
| ☁ Backend               | Firebase                        |
| 🔐 Authentication       | Firebase Authentication         |
| 🗄 Database             | Firebase Realtime Database      |
| 📁 Storage              | Firebase Storage                |
| 🗺 Maps                 | Google Maps SDK                 |
| 📡 APIs                 | Google Play Services            |
| 🖥 IDE                  | Android Studio                  |
| ⚙ Build Tool            | Gradle                          |
| 🧪 Testing              | Android Emulator                |

---

# 💻 Software Requirements

| Component            | Requirement                                 |
| -------------------- | ------------------------------------------- |
| Operating System     | Windows 10/11, Linux (Ubuntu), or macOS     |
| IDE                  | Android Studio (Latest Stable Version)      |
| Programming Language | Kotlin                                      |
| Database             | Firebase Realtime Database                  |
| Authentication       | Firebase Authentication                     |
| Storage              | Firebase Storage                            |
| SDKs                 | Android SDK, Firebase SDKs                  |
| APIs                 | Google Maps API, Google Play Services       |
| UI Library           | Material Design Components                  |
| Image Loading        | Glide / Picasso                             |
| Networking           | Retrofit / Volley (Optional)                |
| Build Tool           | Gradle                                      |
| Testing              | Android Emulator or Physical Android Device |

---

# 📂 Project Structure

```text
BloodBridge-blood-donation-app-kotlin
│
├── .idea/
├── .kotlin/
|
├── app/
│   ├── src/
│   ├── .gitignore
│   ├── build.gradle.kts
│   ├── google-services.json
|   └── proguard-rules.pro
│
├── assets/
│   └── screenshots/
│
├── docs/
|   └── reports/
|
├── gradle/
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradle.bat
├── gradlew
├── README.md
└── settings.gradle.kts
```

---

# 🚀 Future Scope

Although BloodBridge successfully fulfills its primary objectives, several advanced features can further improve its effectiveness and real-world usability.

### 📍 Real-Time Donor Tracking

Integrate advanced Google Maps APIs to enable hospitals to track donors who accept emergency requests, ensuring faster arrival and optimized travel routes.

### 💬 In-App Communication

Provide secure chat and voice calling between hospitals and donors for better coordination during emergencies.


### 🔔 Smart Push Notifications

Deliver personalized notifications based on donor eligibility, nearby emergencies, and upcoming donation schedules.


### 📈 Analytics Dashboard

Provide hospitals with visual insights into donation trends, donor activity, and blood demand.
