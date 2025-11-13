package uj.wmii.pwj.gvt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class Gvt {

    private final ExitHandler exitHandler;
    private final static String gvt = ".gvt";
    private final static Path mainPath = Paths.get(gvt);
    private final static Path latestVersion = mainPath.resolve(".latest_version");

    private final HashMap<String, Command> functionMap = new HashMap<String, Command>() {{
        put("init", new Command() {
            @Override
            public void execute(String... args) throws IOException {
                init(args);
            }
        });
        put("add", new Command() {
            @Override
            public void execute(String... args) {
                add(args);
            }
        });
        put("detach", new Command() {
            @Override
            public void execute(String... args) {
                detach(args);
            }
        });
        put("checkout", new Command() {
            @Override
            public void execute(String... args) throws IOException {
                checkout(args);
            }
        });
        put("commit", new Command() {
            @Override
            public void execute(String... args) {
                commit(args);
            }
        });
        put("history", new Command() {
            @Override
            public void execute(String... args) throws IOException {
                history(args);
            }
        });
        put("version", new Command() {
            @Override
            public void execute(String... args) throws IOException {
                version(args);
            }
        });
    }};

    public Gvt(ExitHandler exitHandler) {
        this.exitHandler = exitHandler;
    }

    public static void main(String... args) {
        Gvt gvt = new Gvt(new ExitHandler());
        gvt.mainInternal(args);
    }

    public void mainInternal(String... args) {
        if (args.length == 0) {
            exitHandler.exit(1, "Please specify command.");
            return;
        }
        if (!functionMap.containsKey(args[0])) {
            exitHandler.exit(1, "Unknown command " + args[0] + ".");
            return;
        }
        if (Files.notExists(mainPath) && !args[0].equals("init")) {
            // Should be \"init\", but the test omits the quotation marks
            exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
            return;
        }
        try {
            functionMap.get(args[0]).execute(args);
        } catch (Exception e) {
            e.printStackTrace();
            exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
        }

    }

    private Integer getLatestVersion() throws IOException {
        return Integer.parseInt(Files.readString(latestVersion));
    }

    private void setLatestVersion(Integer versionNum) throws IOException {
        Files.writeString(latestVersion, versionNum.toString());
    }

    private void createFirstVersion() throws IOException {
        Files.writeString(latestVersion, Integer.toString(-1));
        createNewVersion("GVT initialized.");
    }

    private Integer createNewVersion(String message) throws IOException {
        Integer versionNum = getLatestVersion();
        versionNum++;
        Path dirPath = mainPath.resolve(versionNum.toString());
        Files.createDirectory(dirPath);
        Path mesPath = dirPath.resolve(".message");
        Files.writeString(mesPath, message);
        setLatestVersion(versionNum);
        return versionNum;
    }

    private boolean isAdded(String fileName) throws IOException {
        Integer versionNum = getLatestVersion();
        return Files.exists(mainPath.resolve(versionNum.toString()).resolve(fileName));
    }

    private boolean existsVersion(Integer versionNum) {
        return Files.exists(mainPath.resolve(versionNum.toString()));
    }

    private String getVersionMessage(Integer versionNum) throws IOException {
        return Files.readString(mainPath.resolve(versionNum.toString()).resolve(".message"));
    }

    private void copyFiles(Path sourceDir, Path targetDir) throws IOException {
        File[] files = sourceDir.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().equals(".message")) {
                continue;
            }
            if (file.isDirectory()) {
                copyFiles(file.toPath(), targetDir.resolve(file.getName()));
            }
            else {
                Files.copy(file.toPath(), targetDir.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void addFile(Path targetDir, String fileName) throws IOException {
        Path targetFile = targetDir.resolve(fileName);
        Files.copy(Path.of(fileName), targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void removeFile(Path targetDir, String fileName) throws IOException {
        Files.delete(targetDir.resolve(fileName));
    }

    private void init(String... args) throws IOException {
        Path path = Paths.get(gvt);
        if (Files.exists(path)) {
            exitHandler.exit(10, "Current directory is already initialized");
        }
        else {
            Files.createDirectory(path);
            createFirstVersion();
            exitHandler.exit(0, "Current directory initialized successfully.");
        }
    }

    private void add(String... args) {
        if (args.length < 2) {
            exitHandler.exit(20, "Please specify file to add.");
            return;
        }
        String fileName = args[1];
        String message = (args.length > 2 && args[2].equals("-m")) ? args[3] : "File added successfully. File: " + fileName;
        try {
            if (Files.notExists(Path.of(fileName))) {
                exitHandler.exit(21, "File not found. File: " + fileName);
                return;
            }

            if (isAdded(fileName)) {
                exitHandler.exit(0, "File already added. File: " + fileName);
            }
            else {
                Integer versionNum = createNewVersion(message);
                Path sourceDir = mainPath.resolve(Integer.toString(versionNum - 1));
                Path targetDir = mainPath.resolve(Integer.toString(versionNum));
                copyFiles(sourceDir, targetDir);
                addFile(targetDir, fileName);
                exitHandler.exit(0, "File added successfully. File: " + fileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            exitHandler.exit(22, "File cannot be added. See ERR for details. File: " + fileName + ".");
        }
    }

    private void detach(String... args) {
        if (args.length < 2) {
            exitHandler.exit(30, "Please specify file to detach.");
            return;
        }
        String fileName = args[1];
        String message = (args.length > 2 && args[2].equals("-m")) ? args[3] : "File detached successfully. File: " + fileName;
        try {
            if (!isAdded(fileName)) {
                exitHandler.exit(0, "File is not added to gvt. File: " + fileName);
            }
            else {
                Integer versionNum = createNewVersion(message);
                Path sourceDir = mainPath.resolve(Integer.toString(versionNum - 1));
                Path targetDir = mainPath.resolve(Integer.toString(versionNum));
                copyFiles(sourceDir, targetDir);
                removeFile(targetDir, fileName);
                exitHandler.exit(0, "File detached successfully. File: " + fileName);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            exitHandler.exit(31, "File cannot be added. See ERR for details. File: " + fileName + ".");
        }
    }

    private void checkout(String... args) throws IOException {
        String versionStr = args[1];
        Integer versionNum;
        try {
            versionNum = Integer.parseInt(versionStr);
        }
        catch (NumberFormatException e) {
            exitHandler.exit(60, "Invalid version number: " + versionStr);
            return;
        }
        if (!existsVersion(versionNum)) {
            exitHandler.exit(60, "Invalid version number: " + versionStr);
            return;
        }
        copyFiles(mainPath.resolve(versionStr), Path.of(""));
        exitHandler.exit(0, "Checkout successful for version: " + versionStr);
    }

    private void commit(String... args) {
        if (args.length < 2) {
            exitHandler.exit(50, "Please specify file to commit.");
            return;
        }
        String fileName = args[1];
        String message = (args.length > 2 && args[2].equals("-m")) ? args[3] : "File committed successfully. File: " + fileName;
        try {
            if (Files.notExists(Path.of(fileName))) {
                exitHandler.exit(51, "File not found. File: " + fileName);
                return;
            }

            if (!isAdded(fileName)) {
                exitHandler.exit(0, "File is not added to gvt. File: " + fileName);
            }
            else {
                Integer versionNum = createNewVersion(message);
                Path sourceDir = mainPath.resolve(Integer.toString(versionNum - 1));
                Path targetDir = mainPath.resolve(Integer.toString(versionNum));
                copyFiles(sourceDir, targetDir);
                addFile(targetDir, fileName);
                exitHandler.exit(0, "File committed successfully. File: " + fileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            exitHandler.exit(52, "File cannot be committed. See ERR for details. File: " + fileName + ".");
        }
    }

    private void history(String... args) throws IOException {
        Integer latestVersionNum = getLatestVersion();
        Integer versionsToShow = latestVersionNum + 1;
        if (args.length > 2 && args[1].equals("-last")) {
            Integer temp = Integer.parseInt(args[2]);
            if (temp < versionsToShow) {
                versionsToShow = temp;
            }
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < versionsToShow; i++) {
            Integer versionNum = latestVersionNum - i;
            String message = getVersionMessage(versionNum);
            result.append(versionNum.toString())
                    .append(": ")
                    .append(message.split("\n")[0])
                    .append("\n");
        }
        exitHandler.exit(0, result.toString());
    }

    private void version(String... args) throws IOException {
        Integer versionNum;
        String message;
        if (args.length < 2) {
            versionNum = getLatestVersion();
        }
        else {
            String versionStr = args[1];
            try {
                versionNum = Integer.parseInt(versionStr);
            }
            catch (NumberFormatException e) {
                exitHandler.exit(60, "Invalid version number: " + versionStr);
                return;
            }
            if (!existsVersion(versionNum)) {
                exitHandler.exit(60, "Invalid version number: " + versionStr);
                return;
            }
        }
        message = getVersionMessage(versionNum);
        exitHandler.exit(0, "Version: " + versionNum + "\n" + message);
    }
}

interface Command {
    void execute(String... args) throws Exception;
}
