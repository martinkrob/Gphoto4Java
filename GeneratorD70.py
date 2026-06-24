import re
import sys

def sanitize_identifier(text):
    # Převede text na validní jméno Enum hodnoty v Javě
    text = text.upper()
    text = re.sub(r'[^A-Z0-9_]', '_', text)
    # Pokud začíná číslem, přidáme podtržítko
    if re.match(r'^[0-9]', text):
        text = '_' + text
    return text

def to_camel_case(snake_str):
    # Převod 'f-number' na 'FNumber' nebo 'batterylevel' na 'Batterylevel'
    components = re.split(r'[^a-zA-Z0-9]', snake_str)
    return ''.join(x.title() for x in components)

def parse_config(filepath, outpath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    blocks = content.split('END\n')
    
    seen_methods = set()
    java_code = """package h848.software.gphoto4java;

import h848.software.gphoto4java.core.CommandResult;
import h848.software.gphoto4java.core.Gphoto2Executor;
import h848.software.gphoto4java.exceptions.DeviceBusyException;
import h848.software.gphoto4java.exceptions.Gphoto2Exception;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level API for camera control mapped for Nikon D70.
 * Encapsulates low-level calls of Gphoto2Executor and provides strictly typed Enums.
 */
public class Camera_D70 {

    private final Gphoto2Executor executor;

    public Camera_D70() {
        this.executor = new Gphoto2Executor();
    }

    private void checkErrors(CommandResult result) {
        if (!result.isSuccess()) {
            String err = result.getErrorOutput().toLowerCase();
            if (err.contains("device or resource busy") || err.contains("could not claim the usb device")) {
                throw new DeviceBusyException("Cannot access the camera. It is likely locked by the OS file manager (e.g., gvfs).\\nDetails: " + result.getErrorOutput());
            }
            throw new Gphoto2Exception("Error communicating with gphoto2. Exit code: " + result.getExitCode() + "\\nOutput: " + result.getErrorOutput());
        }
    }

    public boolean isConnected() {
        CommandResult result = executor.execute("--auto-detect");
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        return lines.length > 2;
    }

    public File takePhotoAndDownload() {
        CommandResult result = executor.execute("--capture-image-and-download");
        checkErrors(result);
        
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        for (String line : lines) {
            String l = line.toLowerCase();
            if (l.contains("saving file as")) {
                String[] parts = line.split(" ");
                String filename = parts[parts.length - 1];
                return new File(filename);
            }
        }
        return null;
    }

    private String getConfig(String property) {
        CommandResult result = executor.execute("--get-config", property);
        checkErrors(result);
        
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        for (String line : lines) {
            if (line.startsWith("Current:")) {
                return line.substring("Current:".length()).trim();
            }
        }
        return null;
    }

    private void setConfig(String property, String value) {
        CommandResult result = executor.execute("--set-config", property + "=" + value);
        checkErrors(result);
    }

    public List<String> getSupportedConfigs() {
        CommandResult result = executor.execute("--list-config");
        checkErrors(result);
        
        List<String> configs = new ArrayList<>();
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.startsWith("/")) {
                int lastSlash = line.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < line.length() - 1) {
                    configs.add(line.substring(lastSlash + 1));
                } else {
                    configs.add(line);
                }
            }
        }
        return configs;
    }

    public List<String> getEditableConfigs() {
        CommandResult result = executor.execute("--list-all-config");
        checkErrors(result);
        
        List<String> editable = new ArrayList<>();
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        
        String currentPath = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("/")) {
                currentPath = line;
            } else if (line.startsWith("Readonly: 0") && currentPath != null) {
                int lastSlash = currentPath.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < currentPath.length() - 1) {
                    editable.add(currentPath.substring(lastSlash + 1));
                } else {
                    editable.add(currentPath);
                }
                currentPath = null;
            }
        }
        return editable;
    }

    public List<CameraFile> listFiles() {
        CommandResult result = executor.execute("--list-files");
        checkErrors(result);
        
        List<CameraFile> files = new ArrayList<>();
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        
        String currentFolder = "";
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("There is no file in folder")) {
                continue;
            }
            if (line.startsWith("There is ")) {
                int start = line.indexOf("'/");
                int end = line.lastIndexOf("'");
                if (start != -1 && end != -1 && end > start) {
                    currentFolder = line.substring(start + 1, end);
                }
            } else if (line.startsWith("#")) {
                String[] parts = line.split("\\\\s+");
                if (parts.length >= 2) {
                    try {
                        int index = Integer.parseInt(parts[0].substring(1));
                        files.add(new CameraFile(currentFolder, parts[1], index));
                    } catch (NumberFormatException e) {
                        // ignore unparseable
                    }
                }
            }
        }
        return files;
    }

    public void takePhoto() {
        CommandResult result = executor.execute("--capture-image");
        checkErrors(result);
    }

    public CameraFile takePhotoAndGetFile() {
        CommandResult result = executor.execute("--capture-image");
        checkErrors(result);
        
        String[] lines = result.getStandardOutput().split("\\\\r?\\\\n");
        String folder = null;
        String filename = null;
        for (String line : lines) {
            if (line.startsWith("New file is in location")) {
                String[] parts = line.split(" ");
                if (parts.length >= 6) {
                    String fullPath = parts[5];
                    int lastSlash = fullPath.lastIndexOf('/');
                    if (lastSlash != -1) {
                        folder = fullPath.substring(0, lastSlash);
                        filename = fullPath.substring(lastSlash + 1);
                    }
                }
            }
        }
        
        if (folder != null && filename != null) {
            List<CameraFile> files = listFiles();
            for (CameraFile cf : files) {
                if (cf.getFolder().equals(folder) && cf.getFilename().equals(filename)) {
                    return cf;
                }
            }
            return new CameraFile(folder, filename, -1);
        }
        return null;
    }

    public File downloadFile(CameraFile file) {
        CommandResult result = executor.execute("--folder", file.getFolder(), "--get-file", String.valueOf(file.getIndex()));
        checkErrors(result);
        return new File(file.getFilename());
    }

    public void deleteFile(CameraFile file) {
        CommandResult result = executor.execute("--folder", file.getFolder(), "--delete-file", String.valueOf(file.getIndex()));
        checkErrors(result);
    }

    public void captureSequence(int frames, int intervalSeconds) {
        CommandResult result = executor.execute("--capture-image", "-F", String.valueOf(frames), "-I", String.valueOf(intervalSeconds));
        checkErrors(result);
    }

"""

    for block in blocks:
        block = block.strip()
        if not block: continue
        
        lines = block.split('\n')
        path = lines[0]
        prop_name = path.split('/')[-1]
        
        label = ""
        readonly = True
        prop_type = ""
        choices = []
        
        for line in lines[1:]:
            if line.startswith('Label:'):
                label = line[len('Label:'):].strip()
            elif line.startswith('Readonly:'):
                readonly = (line[len('Readonly:'):].strip() == '1')
            elif line.startswith('Type:'):
                prop_type = line[len('Type:'):].strip()
            elif line.startswith('Choice:'):
                # Formát "Choice: 0 10 seconds"
                parts = line.split(' ', 2)
                if len(parts) >= 3:
                    choices.append(parts[2].strip())
        
        # Generujeme kód pouze pokud víme jak
        method_name = ""
        if label:
            method_name = ''.join(x.title() for x in re.split(r'[^a-zA-Z0-9]', label) if x)
        else:
            method_name = to_camel_case(prop_name)
            
        # Ošetření duplicitních názvů metod
        original_method_name = method_name
        counter = 2
        while method_name in seen_methods:
            method_name = f"{original_method_name}{counter}"
            counter += 1
        seen_methods.add(method_name)
        
        # Setter s Enumem (pokud je RADIO nebo TOGGLE a není Readonly)
        if not readonly and prop_type in ['RADIO', 'TOGGLE'] and choices:
            enum_name = method_name + "Option"
            
            # Enum definition
            java_code += f"    public enum {enum_name} {{\n"
            enum_values = []
            for choice in choices:
                ident = sanitize_identifier(choice)
                if not ident: ident = "EMPTY"
                # Vyhnout se duplicitám (někdy gphoto2 vrací 2x stejnou Choice)
                if ident not in [e[0] for e in enum_values]:
                    enum_values.append((ident, choice))
            
            for i, (ident, val) in enumerate(enum_values):
                comma = "," if i < len(enum_values)-1 else ";"
                java_code += f'        {ident}("{val}"){comma}\n'
            
            java_code += "\n"
            java_code += "        private final String gphotoValue;\n"
            java_code += f"        {enum_name}(String gphotoValue) {{ this.gphotoValue = gphotoValue; }}\n"
            java_code += "        public String getGphotoValue() { return gphotoValue; }\n"
            java_code += "    }\n\n"
            
            # Getter vracející String
            java_code += f"    /**\n     * Gets {label}\n     * @return current value of {label}\n     */\n"
            java_code += f"    public String get{method_name}() {{\n"
            java_code += f'        return getConfig("{prop_name}");\n'
            java_code += "    }\n\n"
            
            # Setter přijímající Enum
            java_code += f"    /**\n     * Sets {label}\n     * @param value new value to set\n     */\n"
            java_code += f"    public void set{method_name}({enum_name} value) {{\n"
            java_code += f'        setConfig("{prop_name}", value.getGphotoValue());\n'
            java_code += "    }\n\n"
            
        elif not readonly and prop_type == 'TEXT':
            # Text getter and setter
            java_code += f"    /**\n     * Gets {label}\n     * @return current value of {label}\n     */\n"
            java_code += f"    public String get{method_name}() {{\n"
            java_code += f'        return getConfig("{prop_name}");\n'
            java_code += "    }\n\n"
            
            java_code += f"    /**\n     * Sets {label}\n     * @param value new value to set\n     */\n"
            java_code += f"    public void set{method_name}(String value) {{\n"
            java_code += f'        setConfig("{prop_name}", value);\n'
            java_code += "    }\n\n"
            
        else: # Readonly nebo jiné
            java_code += f"    /**\n     * Gets {label} (Read-Only)\n     * @return current value of {label}\n     */\n"
            java_code += f"    public String get{method_name}() {{\n"
            java_code += f'        return getConfig("{prop_name}");\n'
            java_code += "    }\n\n"

    java_code += "}\n"
    
    with open(outpath, 'w', encoding='utf-8') as f:
        f.write(java_code)

if __name__ == '__main__':
    parse_config('D70_parameters.txt', 'src/main/java/h848/software/gphoto4java/Camera_D70.java')
