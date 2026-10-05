# Kernel

> The all-in-one university course management system built for the Department of Computer Science & Engineering, BUET.

Kernel replaces scattered tools (email chains, Google Drive folders, WhatsApp groups) with a single platform where teachers publish assessments, students submit work, and everyone stays on the same page.

---

## Features

- **Course Portal** — Dedicated workspace for each course with sidebar navigation (Common, Announcements, Evaluations, Timeline, Grades, Participants)
- **Assessment Engine** — Teachers create CTs, Assignments, Offlines, and Onlines with deadlines, file attachments, and auto-calculated end times
- **Submission System** — Students upload multiple files, teachers grade with CSV import, real-time status tracking
- **Deadline Calendar** — Visual month-view calendar showing all assessments across all courses, color-coded by type
- **Resource Hub** — Per-teacher resource boxes with file upload and web link sharing
- **Announcements** — Course-specific posts with smart-tags that link directly to assessments
- **Weekly Timeline** — 14-week semester view with teacher-editable week names, assessment links, and resource management
- **Grades** — Live evaluation list fetched from server, teacher grading interface, BUET grading scale reference
- **Participants** — Searchable roster with public profiles and clickable course links
- **Profile System** — Editable profiles with photo upload, nickname, social links, and password management
- **Admin Dashboard** — User approval queue, course-teacher assignment manager, system logs
- **Notifications** — Dashboard-level announcement feed aggregated from all enrolled courses
- **Role-Based Access** — Students, Teachers, and Admins see different UI and have different permissions

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | JavaFX 21 + FXML + CSS |
| Backend | Spring Boot 4.x (REST API) |
| Database | SQLite (file-based, zero config) |
| Build | Maven |
| Language | Java 17+ |

---

## Installation

There are two ways to install Kernel depending on who you are:

### Option A: End-User Install (Recommended for students/teachers)

Download the pre-built installer for your platform:

| Platform | File | How to install |
|----------|------|----------------|
| Windows | `Kernel-1.0.0.exe` | Double-click → follow the wizard → launch from Start Menu |
| macOS | `Kernel-1.0.0.dmg` | Double-click → drag Kernel to Applications → launch |
| Linux | `Kernel-1.0.0.deb` | `sudo dpkg -i Kernel-1.0.0.deb` → launch from app menu |

After installing, you need the server running. Ask your course instructor or admin for the server IP address, then:

1. Open `server.properties` in the app's installation folder
2. Set: `server.url=http://<SERVER_IP>:8080`
3. Launch Kernel and log in

> **Note:** If you're running both the server and client on the same machine, it works out of the box — no config needed.

---

### Option B: Developer Setup (Build from source)

#### Prerequisites

1. **Java JDK 17 or higher** — [Download from Oracle](https://www.oracle.com/java/technologies/downloads/) or use OpenJDK
2. **Maven** — [Download from Apache](https://maven.apache.org/download.cgi) (or use your IDE's built-in Maven)
3. **Git** — [Download from git-scm.com](https://git-scm.com/)
4. **An IDE** — IntelliJ IDEA (recommended), Eclipse, or VS Code with Java extensions

Verify your installations:
```bash
java --version    # Should show 17 or higher
mvn --version     # Should show 3.x
git --version     # Should show 2.x
```

#### Step 1: Clone the Repository

```bash
git clone https://github.com/Shafi-8832/Syncron.git
cd Syncron
```

#### Step 2: Start the API Server

This repository contains the JavaFX client.  
You must run a compatible Kernel API server separately (default: `http://localhost:8080`).

#### Step 3: Configure Client → Server URL (Optional)

If your server is not on `localhost:8080`, set either:

1. `server.properties` in project root:
   ```properties
   server.url=http://<SERVER_IP>:8080
   ```
2. or a JVM arg while running:
   ```bash
   mvn javafx:run -Dserver.url=http://<SERVER_IP>:8080
   ```

#### Step 4: Start the JavaFX Client

Open a new terminal:

```bash
cd Syncron
mvn clean javafx:run
```

The login window should appear.

#### Step 5: Log In

| Role | ID | Password |
|------|----|----------|
| Admin | `Admin` | `admin69` |
| Student | Any 7-digit roll from `buet_cse_7digit_rolls.txt` | `buet123` |
| Teacher | Teacher ID (e.g., `T101`) | `buet123` |

---

## Multi-Device Setup

Kernel uses a client-server architecture. You can run the server on one machine and connect from multiple devices on the same network.

### On the Server Machine

1. Start the Spring Boot server normally (Step 2 above)
2. Find your IP address:
   - Windows: `ipconfig` → look for IPv4 Address (e.g., `192.168.1.5`)
   - macOS/Linux: `ifconfig` or `ip addr`
3. Make sure port 8080 is not blocked by your firewall

### On Client Machines

**Option A — Config file:**
1. Create or edit `server.properties` in the app's root folder
2. Set: `server.url=http://192.168.1.5:8080` (use the server's actual IP)
3. Launch the app

**Option B — JVM argument:**
```bash
mvn javafx:run -Dserver.url=http://192.168.1.5:8080
```

All clients pointing to the same server will see real-time changes (new assessments, announcements, grades, etc.)

---

## Building Installers

To distribute Kernel as a native app (`.exe`, `.dmg`, `.deb`), use `jpackage` (bundled with JDK 14+).

> Important: You can only build installers for the OS you're currently on. Build `.exe` on Windows, `.dmg` on macOS, `.deb` on Linux.

```bash
# Step 1: Build the project
mvn clean package -DskipTests

# Step 2: Collect dependencies
mvn dependency:copy-dependencies -DoutputDirectory=target/deps

# Step 3: Create installer (pick your platform)
```

**Windows (.exe):** (requires [WiX Toolset](https://wixtoolset.org/))
```bash
jpackage --name Kernel --app-version 1.0.0 --vendor CSE-BUET --type exe --input target/deps --main-jar Syncron-1.0-SNAPSHOT.jar --main-class com.syncron.Launcher --dest dist --win-shortcut --win-menu
```

**macOS (.dmg):**
```bash
jpackage --name Kernel --app-version 1.0.0 --vendor CSE-BUET --type dmg --input target/deps --main-jar Syncron-1.0-SNAPSHOT.jar --main-class com.syncron.Launcher --dest dist
```

**Linux (.deb):**
```bash
jpackage --name Kernel --app-version 1.0.0 --vendor CSE-BUET --type deb --input target/deps --main-jar Syncron-1.0-SNAPSHOT.jar --main-class com.syncron.Launcher --dest dist
```

**Portable folder (any OS, no installer needed):**
```bash
jpackage --name Kernel --type app-image --input target/deps --main-jar Syncron-1.0-SNAPSHOT.jar --main-class com.syncron.Launcher --dest dist
```

The output appears in the `dist/` folder. The installer bundles a JRE so users don't need Java installed (~80-150 MB).

> Note: The installer only packages the **client**. The Spring Boot server must be hosted separately.

---

## Project Structure

```
Syncron/
├── src/main/java/com/syncron/
│   ├── HelloApplication.java          # App entry point
│   ├── Launcher.java                  # JavaFX launcher (used by jpackage)
│   ├── controllers/                   # UI controllers
│   ├── models/                        # Data models
│   └── utils/                         # Navigation, server config, upload helpers
├── src/main/resources/com/syncron/
│   ├── views/                         # FXML views
│   ├── styles/                        # CSS
│   └── assets/                        # Static assets/fonts/images
├── server.properties                  # Optional client server URL override
├── pom.xml                            # Maven project file
└── README.md
```

## How It Works

Kernel follows a **client-server architecture**:

- The **JavaFX frontend** handles all UI rendering and user interaction
- A separate **Kernel API server** (default `localhost:8080`) handles authentication, storage, and business logic via REST APIs
- The client talks to that API using `ServerConfig` so you can switch servers without code changes

When a teacher creates an assessment, the frontend sends a POST request to the server, which stores it in SQLite and auto-generates an announcement. When a student opens the calendar, the frontend fetches all evaluations via GET and renders them in the month grid. All changes are immediately visible to every connected client.

## Common Issues

| Problem | Solution |
|---------|----------|
| "Could not connect to Kernel Server" | Start the Spring Boot server first (Step 2), then launch the frontend |
| "Login failed: ID not found" | Run `RealDataSeeder.java` to populate the database |
| Empty course list after login | Check that courses exist in the database (run the seeder) |
| Can't connect from another device | Edit `server.properties` with the server machine's IP address |
| CSS not loading / UI looks plain | Check that `kernel_warm.css` exists at `src/main/resources/com/syncron/styles/` |
| jpackage not found | Make sure you're using JDK 14+ (not JRE). Run `java --version` to check |

## License

MIT License — see [LICENSE](LICENSE) for details.

## Development Team

* **Ahnaf Ahmed Shafi** (2405146) – CSE, BUET
* **Shabab Ahmed** (2405143) – CSE, BUET

## Supervisor

**Md. Nurul Muttakin** Lecturer, Dept. of CSE, BUET  
*Course: CSE 108 (Object Oriented Programming Sessional)*
