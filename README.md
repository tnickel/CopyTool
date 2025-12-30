# File Sync Tool

A lightweight Java SWT application for synchronizing a single source file to multiple destination directories.

## Features

- **Single Source Sync**: Select one master file to propagate to multiple locations.
- **Multiple Destinations**: Dynamically add or remove destination folders using a user-friendly interface.
- **Manual & Automatic Sync**:
    - **Manual**: Trigger a sync instantly with a button click.
    - **Automatic**: Set an interval (in minutes) and let the tool sync in the background. Start and stop the timer as needed.
- **File Details**: Double-click any destination folder in the list to view the file's modification date and a preview of its content.
- **Persistence**: Configuration (interval, source, destinations) is automatically saved to a CSV file and loaded on startup.
- **Configurable Storage**: Run the application with a custom root directory to store configuration in a specific location.

## Technology Stack

- **Language**: Java 11+
- **GUI Framework**: SWT (Standard Widget Toolkit)
- **Build System**: Maven

## Installation & Usage

### Prerequisites
- Java Development Kit (JDK) 11 or higher.
- Maven.
- Eclipse IDE (recommended for development).

### Import into Eclipse
1.  Clone this repository or download the source.
2.  In Eclipse, go to **File > Import...**.
3.  Select **Maven > Existing Maven Projects**.
4.  Browse to the project directory and click **Finish**.

### Running the Application
1.  Right-click `src/main/java/com/antigravity/sync/FileTool.java`.
2.  Select **Run As > Java Application**.

### Configuration
The application stores its settings in `config/config.csv`.
- By default, it creates this folder in the current working directory.
- To specify a custom storage location, pass the path as a command-line argument:
    ```bash
    java -jar file-sync-tool.jar "C:/My/Custom/Config/Path"
    ```

## Building
To build a standalone JAR (if configured in `pom.xml`):
```bash
mvn clean package
```

## License
[Your License Here]
