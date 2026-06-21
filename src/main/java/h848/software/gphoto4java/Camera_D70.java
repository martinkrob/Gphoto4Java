package h848.software.gphoto4java;

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
                throw new DeviceBusyException("Cannot access the camera. It is likely locked by the OS file manager (e.g., gvfs).\nDetails: " + result.getErrorOutput());
            }
            throw new Gphoto2Exception("Error communicating with gphoto2. Exit code: " + result.getExitCode() + "\nOutput: " + result.getErrorOutput());
        }
    }

    public boolean isConnected() {
        CommandResult result = executor.execute("--auto-detect");
        String[] lines = result.getStandardOutput().split("\\r?\\n");
        return lines.length > 2;
    }

    public File takePhotoAndDownload() {
        CommandResult result = executor.execute("--capture-image-and-download");
        checkErrors(result);
        
        String[] lines = result.getStandardOutput().split("\\r?\\n");
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
        
        String[] lines = result.getStandardOutput().split("\\r?\\n");
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
        String[] lines = result.getStandardOutput().split("\\r?\\n");
        
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
        String[] lines = result.getStandardOutput().split("\\r?\\n");
        
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

    /**
     * Gets Bulb Mode (Read-Only)
     */
    public String getBulbMode() {
        return getConfig("bulb");
    }

    /**
     * Gets Drive Nikon DSLR Autofocus (Read-Only)
     */
    public String getDriveNikonDslrAutofocus() {
        return getConfig("autofocusdrive");
    }

    /**
     * Gets Set Nikon Control Mode
     */
    public String getSetNikonControlMode() {
        return getConfig("controlmode");
    }

    /**
     * Sets Set Nikon Control Mode
     */
    public void setSetNikonControlMode(String value) {
        setConfig("controlmode", value);
    }

    /**
     * Gets PTP Opcode
     */
    public String getPtpOpcode() {
        return getConfig("opcode");
    }

    /**
     * Sets PTP Opcode
     */
    public void setPtpOpcode(String value) {
        setConfig("opcode", value);
    }

    /**
     * Gets Camera Date and Time (Read-Only)
     */
    public String getCameraDateAndTime() {
        return getConfig("datetime");
    }

    /**
     * Gets Image Comment
     */
    public String getImageComment() {
        return getConfig("imagecomment");
    }

    /**
     * Sets Image Comment
     */
    public void setImageComment(String value) {
        setConfig("imagecomment", value);
    }

    public enum EnableImageCommentOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        EnableImageCommentOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Enable Image Comment
     */
    public String getEnableImageComment() {
        return getConfig("imagecommentenable");
    }

    /**
     * Sets Enable Image Comment
     */
    public void setEnableImageComment(EnableImageCommentOption value) {
        setConfig("imagecommentenable", value.getGphotoValue());
    }

    public enum LcdOffTimeOption {
        _10_SECONDS("10 seconds"),
        _20_SECONDS("20 seconds"),
        _1_MINUTE("1 minute"),
        _5_MINUTES("5 minutes"),
        _10_MINUTES("10 minutes");

        private final String gphotoValue;
        LcdOffTimeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets LCD Off Time
     */
    public String getLcdOffTime() {
        return getConfig("lcdofftime");
    }

    /**
     * Sets LCD Off Time
     */
    public void setLcdOffTime(LcdOffTimeOption value) {
        setConfig("lcdofftime", value.getGphotoValue());
    }

    public enum RecordingMediaOption {
        CARD("Card"),
        SDRAM("SDRAM");

        private final String gphotoValue;
        RecordingMediaOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Recording Media
     */
    public String getRecordingMedia() {
        return getConfig("recordingmedia");
    }

    /**
     * Sets Recording Media
     */
    public void setRecordingMedia(RecordingMediaOption value) {
        setConfig("recordingmedia", value.getGphotoValue());
    }

    public enum CsmMenuOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        CsmMenuOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets CSM Menu
     */
    public String getCsmMenu() {
        return getConfig("csmmenu");
    }

    /**
     * Sets CSM Menu
     */
    public void setCsmMenu(CsmMenuOption value) {
        setConfig("csmmenu", value.getGphotoValue());
    }

    public enum ReverseCommandDialOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ReverseCommandDialOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Reverse Command Dial
     */
    public String getReverseCommandDial() {
        return getConfig("reversedial");
    }

    /**
     * Sets Reverse Command Dial
     */
    public void setReverseCommandDial(ReverseCommandDialOption value) {
        setConfig("reversedial", value.getGphotoValue());
    }

    /**
     * Gets CCD Number (Read-Only)
     */
    public String getCcdNumber() {
        return getConfig("ccdnumber");
    }

    public enum ThumbSizeOption {
        NORMAL("normal"),
        LARGE("large");

        private final String gphotoValue;
        ThumbSizeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Thumb Size
     */
    public String getThumbSize() {
        return getConfig("thumbsize");
    }

    /**
     * Sets Thumb Size
     */
    public void setThumbSize(ThumbSizeOption value) {
        setConfig("thumbsize", value.getGphotoValue());
    }

    /**
     * Gets Fast Filesystem (Read-Only)
     */
    public String getFastFilesystem() {
        return getConfig("fastfs");
    }

    public enum CaptureTargetOption {
        INTERNAL_RAM("Internal RAM"),
        MEMORY_CARD("Memory card");

        private final String gphotoValue;
        CaptureTargetOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Capture Target
     */
    public String getCaptureTarget() {
        return getConfig("capturetarget");
    }

    /**
     * Sets Capture Target
     */
    public void setCaptureTarget(CaptureTargetOption value) {
        setConfig("capturetarget", value.getGphotoValue());
    }

    public enum AutofocusOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        AutofocusOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Autofocus
     */
    public String getAutofocus() {
        return getConfig("autofocus");
    }

    /**
     * Sets Autofocus
     */
    public void setAutofocus(AutofocusOption value) {
        setConfig("autofocus", value.getGphotoValue());
    }

    /**
     * Gets Serial Number (Read-Only)
     */
    public String getSerialNumber() {
        return getConfig("serialnumber");
    }

    /**
     * Gets Camera Manufacturer (Read-Only)
     */
    public String getCameraManufacturer() {
        return getConfig("manufacturer");
    }

    /**
     * Gets Camera Model (Read-Only)
     */
    public String getCameraModel() {
        return getConfig("cameramodel");
    }

    /**
     * Gets Device Version (Read-Only)
     */
    public String getDeviceVersion() {
        return getConfig("deviceversion");
    }

    /**
     * Gets Vendor Extension (Read-Only)
     */
    public String getVendorExtension() {
        return getConfig("vendorextension");
    }

    /**
     * Gets AC Power (Read-Only)
     */
    public String getAcPower() {
        return getConfig("acpower");
    }

    /**
     * Gets External Flash (Read-Only)
     */
    public String getExternalFlash() {
        return getConfig("externalflash");
    }

    /**
     * Gets Battery Level (Read-Only)
     */
    public String getBatteryLevel() {
        return getConfig("batterylevel");
    }

    /**
     * Gets Camera Orientation (Read-Only)
     */
    public String getCameraOrientation() {
        return getConfig("orientation");
    }

    /**
     * Gets Flash Open (Read-Only)
     */
    public String getFlashOpen() {
        return getConfig("flashopen");
    }

    /**
     * Gets Flash Charged (Read-Only)
     */
    public String getFlashCharged() {
        return getConfig("flashcharged");
    }

    /**
     * Gets Lens Name (Read-Only)
     */
    public String getLensName() {
        return getConfig("lensname");
    }

    /**
     * Gets Focal Length Minimum (Read-Only)
     */
    public String getFocalLengthMinimum() {
        return getConfig("minfocallength");
    }

    /**
     * Gets Focal Length Maximum (Read-Only)
     */
    public String getFocalLengthMaximum() {
        return getConfig("maxfocallength");
    }

    /**
     * Gets Maximum Aperture at Focal Length Minimum (Read-Only)
     */
    public String getMaximumApertureAtFocalLengthMinimum() {
        return getConfig("apertureatminfocallength");
    }

    /**
     * Gets Maximum Aperture at Focal Length Maximum (Read-Only)
     */
    public String getMaximumApertureAtFocalLengthMaximum() {
        return getConfig("apertureatmaxfocallength");
    }

    /**
     * Gets Low Light (Read-Only)
     */
    public String getLowLight() {
        return getConfig("lowlight");
    }

    /**
     * Gets Light Meter (Read-Only)
     */
    public String getLightMeter() {
        return getConfig("lightmeter");
    }

    /**
     * Gets AF Locked (Read-Only)
     */
    public String getAfLocked() {
        return getConfig("aflocked");
    }

    /**
     * Gets AE Locked (Read-Only)
     */
    public String getAeLocked() {
        return getConfig("aelocked");
    }

    /**
     * Gets FV Locked (Read-Only)
     */
    public String getFvLocked() {
        return getConfig("fvlocked");
    }

    public enum ImageSizeOption {
        _3008X2000("3008x2000"),
        _2240X1488("2240x1488"),
        _1504X1000("1504x1000");

        private final String gphotoValue;
        ImageSizeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Image Size
     */
    public String getImageSize() {
        return getConfig("imagesize");
    }

    /**
     * Sets Image Size
     */
    public void setImageSize(ImageSizeOption value) {
        setConfig("imagesize", value.getGphotoValue());
    }

    public enum IsoSpeedOption {
        _200("200"),
        _250("250"),
        _320("320"),
        _400("400"),
        _500("500"),
        _640("640"),
        _800("800"),
        _1000("1000"),
        _1250("1250"),
        _1600("1600");

        private final String gphotoValue;
        IsoSpeedOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets ISO Speed
     */
    public String getIsoSpeed() {
        return getConfig("iso");
    }

    /**
     * Sets ISO Speed
     */
    public void setIsoSpeed(IsoSpeedOption value) {
        setConfig("iso", value.getGphotoValue());
    }

    public enum AutoIsoOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        AutoIsoOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Auto ISO
     */
    public String getAutoIso() {
        return getConfig("autoiso");
    }

    /**
     * Sets Auto ISO
     */
    public void setAutoIso(AutoIsoOption value) {
        setConfig("autoiso", value.getGphotoValue());
    }

    public enum WhitebalanceOption {
        AUTOMATIC("Automatic"),
        DAYLIGHT("Daylight"),
        FLUORESCENT("Fluorescent"),
        TUNGSTEN("Tungsten"),
        FLASH("Flash"),
        CLOUDY("Cloudy"),
        SHADE("Shade"),
        PRESET("Preset");

        private final String gphotoValue;
        WhitebalanceOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets WhiteBalance
     */
    public String getWhitebalance() {
        return getConfig("whitebalance");
    }

    /**
     * Sets WhiteBalance
     */
    public void setWhitebalance(WhitebalanceOption value) {
        setConfig("whitebalance", value.getGphotoValue());
    }

    public enum ColorModelOption {
        SRGB__PORTRAIT_("sRGB (portrait)"),
        ADOBERGB("AdobeRGB"),
        SRGB__NATURE_("sRGB (nature)");

        private final String gphotoValue;
        ColorModelOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Color Model
     */
    public String getColorModel() {
        return getConfig("colormodel");
    }

    /**
     * Sets Color Model
     */
    public void setColorModel(ColorModelOption value) {
        setConfig("colormodel", value.getGphotoValue());
    }

    public enum MinimumShutterSpeedOption {
        _1_2000("1/2000"),
        _1_1600("1/1600"),
        _1_1250("1/1250"),
        _1_1000("1/1000"),
        _1_800("1/800"),
        _1_640("1/640"),
        _1_500("1/500"),
        _1_400("1/400"),
        _1_320("1/320"),
        _1_250("1/250"),
        _1_200("1/200"),
        _1_160("1/160"),
        _1_125("1/125");

        private final String gphotoValue;
        MinimumShutterSpeedOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Minimum Shutter Speed
     */
    public String getMinimumShutterSpeed() {
        return getConfig("minimumshutterspeed");
    }

    /**
     * Sets Minimum Shutter Speed
     */
    public void setMinimumShutterSpeed(MinimumShutterSpeedOption value) {
        setConfig("minimumshutterspeed", value.getGphotoValue());
    }

    public enum FlashShutterSpeedOption {
        _1_60S("1/60s"),
        _1_30S("1/30s"),
        _1_15S("1/15s"),
        _1_8S("1/8s"),
        _1_4S("1/4s"),
        _1_2S("1/2s"),
        _1S("1s"),
        _2S("2s"),
        _4S("4s"),
        _8S("8s"),
        _15S("15s"),
        _30S("30s");

        private final String gphotoValue;
        FlashShutterSpeedOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Flash Shutter Speed
     */
    public String getFlashShutterSpeed() {
        return getConfig("flashshutterspeed");
    }

    /**
     * Sets Flash Shutter Speed
     */
    public void setFlashShutterSpeed(FlashShutterSpeedOption value) {
        setConfig("flashshutterspeed", value.getGphotoValue());
    }

    public enum LongExpNoiseReductionOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        LongExpNoiseReductionOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Long Exp Noise Reduction
     */
    public String getLongExpNoiseReduction() {
        return getConfig("longexpnr");
    }

    /**
     * Sets Long Exp Noise Reduction
     */
    public void setLongExpNoiseReduction(LongExpNoiseReductionOption value) {
        setConfig("longexpnr", value.getGphotoValue());
    }

    public enum AssistLightOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        AssistLightOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Assist Light
     */
    public String getAssistLight() {
        return getConfig("assistlight");
    }

    /**
     * Sets Assist Light
     */
    public void setAssistLight(AssistLightOption value) {
        setConfig("assistlight", value.getGphotoValue());
    }

    public enum ExposureCompensationOption {
        _5("-5"),
        _4_5("-4.5"),
        _4("-4"),
        _3_5("-3.5"),
        _3("-3"),
        _2_5("-2.5"),
        _2("-2"),
        _1_5("-1.5"),
        _1("-1"),
        _0_5("-0.5"),
        _0("0");

        private final String gphotoValue;
        ExposureCompensationOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Exposure Compensation
     */
    public String getExposureCompensation() {
        return getConfig("exposurecompensation");
    }

    /**
     * Sets Exposure Compensation
     */
    public void setExposureCompensation(ExposureCompensationOption value) {
        setConfig("exposurecompensation", value.getGphotoValue());
    }

    public enum ExposureCompensation2Option {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ExposureCompensation2Option(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Exposure Compensation
     */
    public String getExposureCompensation2() {
        return getConfig("exposurecompensation2");
    }

    /**
     * Sets Exposure Compensation
     */
    public void setExposureCompensation2(ExposureCompensation2Option value) {
        setConfig("exposurecompensation2", value.getGphotoValue());
    }

    public enum FlashModeOption {
        RED_EYE_AUTOMATIC("Red-eye automatic"),
        AUTO("Auto"),
        REAR_CURTAIN_SYNC___SLOW_SYNC("Rear Curtain Sync + Slow Sync");

        private final String gphotoValue;
        FlashModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Flash Mode
     */
    public String getFlashMode() {
        return getConfig("flashmode");
    }

    /**
     * Sets Flash Mode
     */
    public void setFlashMode(FlashModeOption value) {
        setConfig("flashmode", value.getGphotoValue());
    }

    public enum NikonFlashModeOption {
        ITTL("iTTL"),
        MANUAL("Manual"),
        COMMANDER("Commander");

        private final String gphotoValue;
        NikonFlashModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Nikon Flash Mode
     */
    public String getNikonFlashMode() {
        return getConfig("nikonflashmode");
    }

    /**
     * Sets Nikon Flash Mode
     */
    public void setNikonFlashMode(NikonFlashModeOption value) {
        setConfig("nikonflashmode", value.getGphotoValue());
    }

    public enum FlashCommanderModeOption {
        TTL("TTL"),
        AUTO_APERTURE("Auto Aperture"),
        FULL_MANUAL("Full Manual");

        private final String gphotoValue;
        FlashCommanderModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Flash Commander Mode
     */
    public String getFlashCommanderMode() {
        return getConfig("flashcommandermode");
    }

    /**
     * Sets Flash Commander Mode
     */
    public void setFlashCommanderMode(FlashCommanderModeOption value) {
        setConfig("flashcommandermode", value.getGphotoValue());
    }

    public enum FlashCommanderPowerOption {
        FULL("Full"),
        _1_2("1/2"),
        _1_4("1/4"),
        _1_8("1/8"),
        _1_16("1/16"),
        _1_32("1/32"),
        _1_64("1/64"),
        _1_128("1/128");

        private final String gphotoValue;
        FlashCommanderPowerOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Flash Commander Power
     */
    public String getFlashCommanderPower() {
        return getConfig("flashcommanderpower");
    }

    /**
     * Sets Flash Commander Power
     */
    public void setFlashCommanderPower(FlashCommanderPowerOption value) {
        setConfig("flashcommanderpower", value.getGphotoValue());
    }

    public enum AfAreaIlluminationOption {
        AUTO("Auto"),
        OFF("Off"),
        ON("On");

        private final String gphotoValue;
        AfAreaIlluminationOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets AF Area Illumination
     */
    public String getAfAreaIllumination() {
        return getConfig("af-area-illumination");
    }

    /**
     * Sets AF Area Illumination
     */
    public void setAfAreaIllumination(AfAreaIlluminationOption value) {
        setConfig("af-area-illumination", value.getGphotoValue());
    }

    public enum AfBeepModeOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        AfBeepModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets AF Beep Mode
     */
    public String getAfBeepMode() {
        return getConfig("afbeep");
    }

    /**
     * Sets AF Beep Mode
     */
    public void setAfBeepMode(AfBeepModeOption value) {
        setConfig("afbeep", value.getGphotoValue());
    }

    public enum FNumberOption {
        F_3_5("f/3.5"),
        F_4("f/4"),
        F_4_8("f/4.8"),
        F_5_6("f/5.6"),
        F_6_7("f/6.7"),
        F_8("f/8"),
        F_9_5("f/9.5"),
        F_11("f/11"),
        F_13("f/13"),
        F_16("f/16"),
        F_19("f/19"),
        F_22("f/22");

        private final String gphotoValue;
        FNumberOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets F-Number
     */
    public String getFNumber() {
        return getConfig("f-number");
    }

    /**
     * Sets F-Number
     */
    public void setFNumber(FNumberOption value) {
        setConfig("f-number", value.getGphotoValue());
    }

    /**
     * Gets Flexible Program (Read-Only)
     */
    public String getFlexibleProgram() {
        return getConfig("flexibleprogram");
    }

    public enum ImageQualityOption {
        JPEG_BASIC("JPEG Basic"),
        JPEG_NORMAL("JPEG Normal"),
        JPEG_FINE("JPEG Fine"),
        NEF__RAW_("NEF (Raw)"),
        NEF_BASIC("NEF+Basic");

        private final String gphotoValue;
        ImageQualityOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Image Quality
     */
    public String getImageQuality() {
        return getConfig("imagequality");
    }

    /**
     * Sets Image Quality
     */
    public void setImageQuality(ImageQualityOption value) {
        setConfig("imagequality", value.getGphotoValue());
    }

    /**
     * Gets Focal Length (Read-Only)
     */
    public String getFocalLength() {
        return getConfig("focallength");
    }

    /**
     * Gets Focus Mode (Read-Only)
     */
    public String getFocusMode() {
        return getConfig("focusmode");
    }

    public enum FocusMode2Option {
        AF_S("AF-S"),
        AF_C("AF-C");

        private final String gphotoValue;
        FocusMode2Option(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Focus Mode 2
     */
    public String getFocusMode2() {
        return getConfig("focusmode2");
    }

    /**
     * Sets Focus Mode 2
     */
    public void setFocusMode2(FocusMode2Option value) {
        setConfig("focusmode2", value.getGphotoValue());
    }

    /**
     * Gets Exposure Program (Read-Only)
     */
    public String getExposureProgram() {
        return getConfig("expprogram");
    }

    public enum StillCaptureModeOption {
        SINGLE_SHOT("Single Shot"),
        BURST("Burst"),
        TIMER("Timer"),
        REMOTE("Remote"),
        QUICK_RESPONSE_REMOTE("Quick Response Remote");

        private final String gphotoValue;
        StillCaptureModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Still Capture Mode
     */
    public String getStillCaptureMode() {
        return getConfig("capturemode");
    }

    /**
     * Sets Still Capture Mode
     */
    public void setStillCaptureMode(StillCaptureModeOption value) {
        setConfig("capturemode", value.getGphotoValue());
    }

    public enum FocusMeteringModeOption {
        MULTI_SPOT("Multi-spot"),
        SINGLE_AREA("Single Area"),
        CLOSEST_SUBJECT("Closest Subject");

        private final String gphotoValue;
        FocusMeteringModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Focus Metering Mode
     */
    public String getFocusMeteringMode() {
        return getConfig("focusmetermode");
    }

    /**
     * Sets Focus Metering Mode
     */
    public void setFocusMeteringMode(FocusMeteringModeOption value) {
        setConfig("focusmetermode", value.getGphotoValue());
    }

    public enum ExposureMeteringModeOption {
        CENTER_WEIGHTED("Center Weighted"),
        MULTI_SPOT("Multi Spot"),
        CENTER_SPOT("Center Spot");

        private final String gphotoValue;
        ExposureMeteringModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Exposure Metering Mode
     */
    public String getExposureMeteringMode() {
        return getConfig("exposuremetermode");
    }

    /**
     * Sets Exposure Metering Mode
     */
    public void setExposureMeteringMode(ExposureMeteringModeOption value) {
        setConfig("exposuremetermode", value.getGphotoValue());
    }

    public enum ShutterSpeedOption {
        _0_0001S("0.0001s"),
        _0_0002S("0.0002s"),
        _0_0003S("0.0003s"),
        _0_0005S("0.0005s"),
        _0_0006S("0.0006s"),
        _0_0010S("0.0010s"),
        _0_0013S("0.0013s"),
        _0_0020S("0.0020s"),
        _0_0028S("0.0028s"),
        _0_0040S("0.0040s"),
        _0_0055S("0.0055s"),
        _0_0080S("0.0080s"),
        _0_0111S("0.0111s"),
        _0_0166S("0.0166s"),
        _0_0222S("0.0222s"),
        _0_0333S("0.0333s"),
        _0_0500S("0.0500s"),
        _0_0666S("0.0666s"),
        _0_1000S("0.1000s"),
        _0_1250S("0.1250s"),
        _0_1666S("0.1666s"),
        _0_2500S("0.2500s"),
        _0_3333S("0.3333s"),
        _0_5000S("0.5000s"),
        _0_6666S("0.6666s"),
        _1_0000S("1.0000s"),
        _1_5000S("1.5000s"),
        _2_0000S("2.0000s"),
        _3_0000S("3.0000s"),
        _4_0000S("4.0000s"),
        _6_0000S("6.0000s"),
        _8_0000S("8.0000s"),
        _10_0000S("10.0000s"),
        _15_0000S("15.0000s"),
        _20_0000S("20.0000s"),
        _30_0000S("30.0000s"),
        BULB("Bulb");

        private final String gphotoValue;
        ShutterSpeedOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Shutter Speed
     */
    public String getShutterSpeed() {
        return getConfig("shutterspeed");
    }

    /**
     * Sets Shutter Speed
     */
    public void setShutterSpeed(ShutterSpeedOption value) {
        setConfig("shutterspeed", value.getGphotoValue());
    }

    public enum ShutterSpeed2Option {
        _1_8000("1/8000"),
        _1_6000("1/6000"),
        _1_4000("1/4000"),
        _1_3000("1/3000"),
        _1_2000("1/2000"),
        _1_1500("1/1500"),
        _1_1000("1/1000"),
        _1_750("1/750"),
        _1_500("1/500"),
        _1_350("1/350"),
        _1_250("1/250"),
        _1_180("1/180"),
        _1_125("1/125"),
        _1_90("1/90"),
        _1_60("1/60"),
        _1_45("1/45"),
        _1_30("1/30"),
        _1_20("1/20"),
        _1_15("1/15"),
        _1_10("1/10"),
        _1_8("1/8"),
        _1_6("1/6"),
        _1_4("1/4"),
        _1_3("1/3"),
        _1_2("1/2"),
        _10_15("10/15"),
        _1("1"),
        _15_10("15/10"),
        _2("2"),
        _3("3"),
        _4("4"),
        _6("6"),
        _8("8"),
        _10("10"),
        _15("15"),
        _20("20"),
        _30("30"),
        BULB("Bulb");

        private final String gphotoValue;
        ShutterSpeed2Option(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Shutter Speed 2
     */
    public String getShutterSpeed2() {
        return getConfig("shutterspeed2");
    }

    /**
     * Sets Shutter Speed 2
     */
    public void setShutterSpeed2(ShutterSpeed2Option value) {
        setConfig("shutterspeed2", value.getGphotoValue());
    }

    public enum FocusAreaWrapOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        FocusAreaWrapOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Focus Area Wrap
     */
    public String getFocusAreaWrap() {
        return getConfig("focusareawrap");
    }

    /**
     * Sets Focus Area Wrap
     */
    public void setFocusAreaWrap(FocusAreaWrapOption value) {
        setConfig("focusareawrap", value.getGphotoValue());
    }

    public enum ExposureLockOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ExposureLockOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Exposure Lock
     */
    public String getExposureLock() {
        return getConfig("exposurelock");
    }

    /**
     * Sets Exposure Lock
     */
    public void setExposureLock(ExposureLockOption value) {
        setConfig("exposurelock", value.getGphotoValue());
    }

    public enum AeLAfLModeOption {
        AE_AF_LOCK("AE/AF Lock"),
        AE_LOCK_ONLY("AE Lock only"),
        AF_LOCK_ONLY("AF Lock Only"),
        AF_LOCK_HOLD("AF Lock Hold"),
        AF_ON("AF On"),
        FLASH_LEVEL_LOCK("Flash Level Lock");

        private final String gphotoValue;
        AeLAfLModeOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets AE-L/AF-L Mode
     */
    public String getAeLAfLMode() {
        return getConfig("aelaflmode");
    }

    /**
     * Sets AE-L/AF-L Mode
     */
    public void setAeLAfLMode(AeLAfLModeOption value) {
        setConfig("aelaflmode", value.getGphotoValue());
    }

    public enum FileNumberSequencingOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        FileNumberSequencingOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets File Number Sequencing
     */
    public String getFileNumberSequencing() {
        return getConfig("filenrsequencing");
    }

    /**
     * Sets File Number Sequencing
     */
    public void setFileNumberSequencing(FileNumberSequencingOption value) {
        setConfig("filenrsequencing", value.getGphotoValue());
    }

    public enum FlashSignOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        FlashSignOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Flash Sign
     */
    public String getFlashSign() {
        return getConfig("flashsign");
    }

    /**
     * Sets Flash Sign
     */
    public void setFlashSign(FlashSignOption value) {
        setConfig("flashsign", value.getGphotoValue());
    }

    public enum ViewfinderGridOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ViewfinderGridOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Viewfinder Grid
     */
    public String getViewfinderGrid() {
        return getConfig("viewfindergrid");
    }

    /**
     * Sets Viewfinder Grid
     */
    public void setViewfinderGrid(ViewfinderGridOption value) {
        setConfig("viewfindergrid", value.getGphotoValue());
    }

    public enum ImageReviewOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ImageReviewOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Image Review
     */
    public String getImageReview() {
        return getConfig("imagereview");
    }

    /**
     * Sets Image Review
     */
    public void setImageReview(ImageReviewOption value) {
        setConfig("imagereview", value.getGphotoValue());
    }

    public enum ImageRotationFlagOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ImageRotationFlagOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Image Rotation Flag
     */
    public String getImageRotationFlag() {
        return getConfig("imagerotationflag");
    }

    /**
     * Sets Image Rotation Flag
     */
    public void setImageRotationFlag(ImageRotationFlagOption value) {
        setConfig("imagerotationflag", value.getGphotoValue());
    }

    public enum ReleaseWithoutCfCardOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        ReleaseWithoutCfCardOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Release without CF card
     */
    public String getReleaseWithoutCfCard() {
        return getConfig("nocfcardrelease");
    }

    /**
     * Sets Release without CF card
     */
    public void setReleaseWithoutCfCard(ReleaseWithoutCfCardOption value) {
        setConfig("nocfcardrelease", value.getGphotoValue());
    }

    public enum FlashModeManualPowerOption {
        FULL("Full"),
        _1_2("1/2"),
        _1_4("1/4"),
        _1_8("1/8"),
        _1_16("1/16");

        private final String gphotoValue;
        FlashModeManualPowerOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Flash Mode Manual Power
     */
    public String getFlashModeManualPower() {
        return getConfig("flashmodemanualpower");
    }

    /**
     * Sets Flash Mode Manual Power
     */
    public void setFlashModeManualPower(FlashModeManualPowerOption value) {
        setConfig("flashmodemanualpower", value.getGphotoValue());
    }

    public enum AutoFocusAreaOption {
        CENTRE("Centre"),
        TOP("Top"),
        BOTTOM("Bottom"),
        LEFT("Left"),
        RIGHT("Right");

        private final String gphotoValue;
        AutoFocusAreaOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Auto Focus Area
     */
    public String getAutoFocusArea() {
        return getConfig("autofocusarea");
    }

    /**
     * Sets Auto Focus Area
     */
    public void setAutoFocusArea(AutoFocusAreaOption value) {
        setConfig("autofocusarea", value.getGphotoValue());
    }

    /**
     * Gets Flash Exposure Compensation (Read-Only)
     */
    public String getFlashExposureCompensation() {
        return getConfig("flashexposurecompensation");
    }

    public enum BracketingOption {
        ON("On"),
        OFF("Off");

        private final String gphotoValue;
        BracketingOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Bracketing
     */
    public String getBracketing() {
        return getConfig("bracketing");
    }

    /**
     * Sets Bracketing
     */
    public void setBracketing(BracketingOption value) {
        setConfig("bracketing", value.getGphotoValue());
    }

    public enum EvStepOption {
        _1_3("1/3"),
        _1_2("1/2"),
        UNKNOWN_VALUE_0002("Unknown value 0002");

        private final String gphotoValue;
        EvStepOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets EV Step
     */
    public String getEvStep() {
        return getConfig("evstep");
    }

    /**
     * Sets EV Step
     */
    public void setEvStep(EvStepOption value) {
        setConfig("evstep", value.getGphotoValue());
    }

    public enum BracketSetOption {
        AE___FLASH("AE & Flash"),
        AE_ONLY("AE only"),
        FLASH_ONLY("Flash only"),
        WB_BRACKETING("WB bracketing");

        private final String gphotoValue;
        BracketSetOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Bracket Set
     */
    public String getBracketSet() {
        return getConfig("bracketset");
    }

    /**
     * Sets Bracket Set
     */
    public void setBracketSet(BracketSetOption value) {
        setConfig("bracketset", value.getGphotoValue());
    }

    public enum BracketOrderOption {
        MTR___UNDER("MTR > Under"),
        UNDER___MTR("Under > MTR");

        private final String gphotoValue;
        BracketOrderOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Bracket Order
     */
    public String getBracketOrder() {
        return getConfig("bracketorder");
    }

    /**
     * Sets Bracket Order
     */
    public void setBracketOrder(BracketOrderOption value) {
        setConfig("bracketorder", value.getGphotoValue());
    }

    public enum WbBracketingStepOption {
        _1_EV("1 EV"),
        _2_EV("2 EV"),
        _3_EV("3 EV");

        private final String gphotoValue;
        WbBracketingStepOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets WB Bracketing Step
     */
    public String getWbBracketingStep() {
        return getConfig("wbbracketingstep");
    }

    /**
     * Sets WB Bracketing Step
     */
    public void setWbBracketingStep(WbBracketingStepOption value) {
        setConfig("wbbracketingstep", value.getGphotoValue());
    }

    public enum AeBracketingPatternOption {
        _2_IMAGES__NORMAL_AND_UNDER_("2 images (normal and under)"),
        _2_IMAGES__NORMAL_AND_OVER_("2 images (normal and over)"),
        _3_IMAGES__NORMAL_AND_2_UNDERS_("3 images (normal and 2 unders)");

        private final String gphotoValue;
        AeBracketingPatternOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets AE Bracketing Pattern
     */
    public String getAeBracketingPattern() {
        return getConfig("aebracketingpattern");
    }

    /**
     * Sets AE Bracketing Pattern
     */
    public void setAeBracketingPattern(AeBracketingPatternOption value) {
        setConfig("aebracketingpattern", value.getGphotoValue());
    }

    public enum WbBracketingPatternOption {
        _2_IMAGES__NORMAL_AND_UNDER_("2 images (normal and under)"),
        _2_IMAGES__NORMAL_AND_OVER_("2 images (normal and over)"),
        _3_IMAGES__NORMAL_AND_2_UNDERS_("3 images (normal and 2 unders)");

        private final String gphotoValue;
        WbBracketingPatternOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets WB Bracketing Pattern
     */
    public String getWbBracketingPattern() {
        return getConfig("wbbracketingpattern");
    }

    /**
     * Sets WB Bracketing Pattern
     */
    public void setWbBracketingPattern(WbBracketingPatternOption value) {
        setConfig("wbbracketingpattern", value.getGphotoValue());
    }

    /**
     * Gets AE Bracketing Count (Read-Only)
     */
    public String getAeBracketingCount() {
        return getConfig("aebracketingcount");
    }

    /**
     * Gets Burst Number (Read-Only)
     */
    public String getBurstNumber() {
        return getConfig("burstnumber");
    }

    /**
     * Gets Maximum Shots (Read-Only)
     */
    public String getMaximumShots() {
        return getConfig("maximumshots");
    }

    /**
     * Gets Auto White Balance Bias (Read-Only)
     */
    public String getAutoWhiteBalanceBias() {
        return getConfig("autowhitebias");
    }

    /**
     * Gets Tungsten White Balance Bias (Read-Only)
     */
    public String getTungstenWhiteBalanceBias() {
        return getConfig("tungstenwhitebias");
    }

    /**
     * Gets Fluorescent White Balance Bias (Read-Only)
     */
    public String getFluorescentWhiteBalanceBias() {
        return getConfig("flourescentwhitebias");
    }

    /**
     * Gets Daylight White Balance Bias (Read-Only)
     */
    public String getDaylightWhiteBalanceBias() {
        return getConfig("daylightwhitebias");
    }

    /**
     * Gets Flash White Balance Bias (Read-Only)
     */
    public String getFlashWhiteBalanceBias() {
        return getConfig("flashwhitebias");
    }

    /**
     * Gets Cloudy White Balance Bias (Read-Only)
     */
    public String getCloudyWhiteBalanceBias() {
        return getConfig("cloudywhitebias");
    }

    /**
     * Gets Shady White Balance Bias (Read-Only)
     */
    public String getShadyWhiteBalanceBias() {
        return getConfig("shadewhitebias");
    }

    public enum WhiteBalanceBiasPresetNrOption {
        _0("0"),
        _1("1");

        private final String gphotoValue;
        WhiteBalanceBiasPresetNrOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets White Balance Bias Preset Nr
     */
    public String getWhiteBalanceBiasPresetNr() {
        return getConfig("whitebiaspresetno");
    }

    /**
     * Sets White Balance Bias Preset Nr
     */
    public void setWhiteBalanceBiasPresetNr(WhiteBalanceBiasPresetNrOption value) {
        setConfig("whitebiaspresetno", value.getGphotoValue());
    }

    /**
     * Gets White Balance Bias Preset 0 (Read-Only)
     */
    public String getWhiteBalanceBiasPreset0() {
        return getConfig("whitebiaspreset0");
    }

    /**
     * Gets White Balance Bias Preset 1 (Read-Only)
     */
    public String getWhiteBalanceBiasPreset1() {
        return getConfig("whitebiaspreset1");
    }

    public enum SelftimerDelayOption {
        _2_SECONDS("2 seconds"),
        _5_SECONDS("5 seconds"),
        _10_SECONDS("10 seconds"),
        _20_SECONDS("20 seconds");

        private final String gphotoValue;
        SelftimerDelayOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Selftimer Delay
     */
    public String getSelftimerDelay() {
        return getConfig("selftimerdelay");
    }

    /**
     * Sets Selftimer Delay
     */
    public void setSelftimerDelay(SelftimerDelayOption value) {
        setConfig("selftimerdelay", value.getGphotoValue());
    }

    public enum CenterWeightAreaOption {
        _6_MM("6 mm"),
        _8_MM("8 mm"),
        _10_MM("10 mm"),
        _12_MM("12 mm");

        private final String gphotoValue;
        CenterWeightAreaOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Center Weight Area
     */
    public String getCenterWeightArea() {
        return getConfig("centerweightsize");
    }

    /**
     * Sets Center Weight Area
     */
    public void setCenterWeightArea(CenterWeightAreaOption value) {
        setConfig("centerweightsize", value.getGphotoValue());
    }

    public enum RemoteTimeoutOption {
        _1_MINUTE("1 minute"),
        _5_MINUTES("5 minutes"),
        _10_MINUTES("10 minutes"),
        _15_MINUTES("15 minutes");

        private final String gphotoValue;
        RemoteTimeoutOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Remote Timeout
     */
    public String getRemoteTimeout() {
        return getConfig("remotetimeout");
    }

    /**
     * Sets Remote Timeout
     */
    public void setRemoteTimeout(RemoteTimeoutOption value) {
        setConfig("remotetimeout", value.getGphotoValue());
    }

    public enum OptimizeImageOption {
        NORMAL("Normal"),
        VIVID("Vivid"),
        SHARPER("Sharper"),
        SOFTER("Softer"),
        DIRECT_PRINT("Direct Print"),
        PORTRAIT("Portrait"),
        LANDSCAPE("Landscape"),
        CUSTOM("Custom");

        private final String gphotoValue;
        OptimizeImageOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Optimize Image
     */
    public String getOptimizeImage() {
        return getConfig("optimizeimage");
    }

    /**
     * Sets Optimize Image
     */
    public void setOptimizeImage(OptimizeImageOption value) {
        setConfig("optimizeimage", value.getGphotoValue());
    }

    public enum SharpeningOption {
        AUTO("Auto"),
        NORMAL("Normal"),
        LOW("Low"),
        MEDIUM_LOW("Medium Low"),
        MEDIUM_HIGH("Medium high"),
        HIGH("High"),
        NONE("None");

        private final String gphotoValue;
        SharpeningOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Sharpening
     */
    public String getSharpening() {
        return getConfig("sharpening");
    }

    /**
     * Sets Sharpening
     */
    public void setSharpening(SharpeningOption value) {
        setConfig("sharpening", value.getGphotoValue());
    }

    public enum ToneCompensationOption {
        AUTO("Auto"),
        NORMAL("Normal"),
        LOW_CONTRAST("Low contrast"),
        MEDIUM_LOW("Medium Low"),
        MEDIUM_HIGH("Medium High"),
        HIGH_CONTROL("High control"),
        CUSTOM("Custom");

        private final String gphotoValue;
        ToneCompensationOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Tone Compensation
     */
    public String getToneCompensation() {
        return getConfig("tonecompensation");
    }

    /**
     * Sets Tone Compensation
     */
    public void setToneCompensation(ToneCompensationOption value) {
        setConfig("tonecompensation", value.getGphotoValue());
    }

    public enum SaturationOption {
        NORMAL("Normal"),
        MODERATE("Moderate"),
        ENHANCED("Enhanced");

        private final String gphotoValue;
        SaturationOption(String gphotoValue) { this.gphotoValue = gphotoValue; }
        public String getGphotoValue() { return gphotoValue; }
    }

    /**
     * Gets Saturation
     */
    public String getSaturation() {
        return getConfig("saturation");
    }

    /**
     * Sets Saturation
     */
    public void setSaturation(SaturationOption value) {
        setConfig("saturation", value.getGphotoValue());
    }

    /**
     * Gets Hue Adjustment (Read-Only)
     */
    public String getHueAdjustment() {
        return getConfig("hueadjustment");
    }

    /**
     * Gets Battery Level (Read-Only)
     */
    public String getBatteryLevel2() {
        return getConfig("5001");
    }

    /**
     * Gets Image Size (Read-Only)
     */
    public String getImageSize2() {
        return getConfig("5003");
    }

    /**
     * Gets Compression Setting (Read-Only)
     */
    public String getCompressionSetting() {
        return getConfig("5004");
    }

    /**
     * Gets White Balance (Read-Only)
     */
    public String getWhiteBalance() {
        return getConfig("5005");
    }

    /**
     * Gets F-Number (Read-Only)
     */
    public String getFNumber2() {
        return getConfig("5007");
    }

    /**
     * Gets Focal Length (Read-Only)
     */
    public String getFocalLength2() {
        return getConfig("5008");
    }

    /**
     * Gets Focus Mode (Read-Only)
     */
    public String getFocusMode3() {
        return getConfig("500a");
    }

    /**
     * Gets Exposure Metering Mode (Read-Only)
     */
    public String getExposureMeteringMode2() {
        return getConfig("500b");
    }

    /**
     * Gets Flash Mode (Read-Only)
     */
    public String getFlashMode2() {
        return getConfig("500c");
    }

    /**
     * Gets Exposure Time (Read-Only)
     */
    public String getExposureTime() {
        return getConfig("500d");
    }

    /**
     * Gets Exposure Program Mode (Read-Only)
     */
    public String getExposureProgramMode() {
        return getConfig("500e");
    }

    /**
     * Gets Exposure Index (film speed ISO) (Read-Only)
     */
    public String getExposureIndexFilmSpeedIso() {
        return getConfig("500f");
    }

    /**
     * Gets Exposure Bias Compensation (Read-Only)
     */
    public String getExposureBiasCompensation() {
        return getConfig("5010");
    }

    /**
     * Gets Date & Time
     */
    public String getDateTime() {
        return getConfig("5011");
    }

    /**
     * Sets Date & Time
     */
    public void setDateTime(String value) {
        setConfig("5011", value);
    }

    /**
     * Gets Still Capture Mode (Read-Only)
     */
    public String getStillCaptureMode2() {
        return getConfig("5013");
    }

    /**
     * Gets Burst Number (Read-Only)
     */
    public String getBurstNumber2() {
        return getConfig("5018");
    }

    /**
     * Gets Focus Metering Mode (Read-Only)
     */
    public String getFocusMeteringMode2() {
        return getConfig("501c");
    }

    /**
     * Gets Auto White Balance Bias (Read-Only)
     */
    public String getAutoWhiteBalanceBias2() {
        return getConfig("d017");
    }

    /**
     * Gets Tungsten White Balance Bias (Read-Only)
     */
    public String getTungstenWhiteBalanceBias2() {
        return getConfig("d018");
    }

    /**
     * Gets Fluorescent White Balance Bias (Read-Only)
     */
    public String getFluorescentWhiteBalanceBias2() {
        return getConfig("d019");
    }

    /**
     * Gets Daylight White Balance Bias (Read-Only)
     */
    public String getDaylightWhiteBalanceBias2() {
        return getConfig("d01a");
    }

    /**
     * Gets Flash White Balance Bias (Read-Only)
     */
    public String getFlashWhiteBalanceBias2() {
        return getConfig("d01b");
    }

    /**
     * Gets Cloudy White Balance Bias (Read-Only)
     */
    public String getCloudyWhiteBalanceBias2() {
        return getConfig("d01c");
    }

    /**
     * Gets Shady White Balance Bias (Read-Only)
     */
    public String getShadyWhiteBalanceBias2() {
        return getConfig("d01d");
    }

    /**
     * Gets White Balance Preset Number (Read-Only)
     */
    public String getWhiteBalancePresetNumber() {
        return getConfig("d01f");
    }

    /**
     * Gets White Balance Preset Value 0 (Read-Only)
     */
    public String getWhiteBalancePresetValue0() {
        return getConfig("d025");
    }

    /**
     * Gets White Balance Preset Value 1 (Read-Only)
     */
    public String getWhiteBalancePresetValue1() {
        return getConfig("d026");
    }

    /**
     * Gets Sharpening (Read-Only)
     */
    public String getSharpening2() {
        return getConfig("d02a");
    }

    /**
     * Gets Tone Compensation (Read-Only)
     */
    public String getToneCompensation2() {
        return getConfig("d02b");
    }

    /**
     * Gets Color Model (Read-Only)
     */
    public String getColorModel2() {
        return getConfig("d02c");
    }

    /**
     * Gets Hue Adjustment (Read-Only)
     */
    public String getHueAdjustment2() {
        return getConfig("d02d");
    }

    /**
     * Gets Reset Menu Bank (Read-Only)
     */
    public String getResetMenuBank() {
        return getConfig("d045");
    }

    /**
     * Gets Focus Area Wrap (Read-Only)
     */
    public String getFocusAreaWrap2() {
        return getConfig("d04f");
    }

    /**
     * Gets Auto ISO (Read-Only)
     */
    public String getAutoIso2() {
        return getConfig("d054");
    }

    /**
     * Gets Exposure Step (Read-Only)
     */
    public String getExposureStep() {
        return getConfig("d056");
    }

    /**
     * Gets Exposure Compensation (Read-Only)
     */
    public String getExposureCompensation3() {
        return getConfig("d058");
    }

    /**
     * Gets Centre Weight Area (Read-Only)
     */
    public String getCentreWeightArea() {
        return getConfig("d059");
    }

    /**
     * Gets Exposure Lock (Read-Only)
     */
    public String getExposureLock2() {
        return getConfig("d05e");
    }

    /**
     * Gets Focus Lock (Read-Only)
     */
    public String getFocusLock() {
        return getConfig("d05f");
    }

    /**
     * Gets Auto Meter Off Time (Read-Only)
     */
    public String getAutoMeterOffTime() {
        return getConfig("d062");
    }

    /**
     * Gets Self Timer Delay (Read-Only)
     */
    public String getSelfTimerDelay() {
        return getConfig("d063");
    }

    /**
     * Gets LCD Off Time (Read-Only)
     */
    public String getLcdOffTime2() {
        return getConfig("d064");
    }

    /**
     * Gets Long Exposure Noise Reduction (Read-Only)
     */
    public String getLongExposureNoiseReduction() {
        return getConfig("d06b");
    }

    /**
     * Gets File Number Sequencing (Read-Only)
     */
    public String getFileNumberSequencing2() {
        return getConfig("d06c");
    }

    /**
     * Gets Flash Shutter Speed (Read-Only)
     */
    public String getFlashShutterSpeed2() {
        return getConfig("d075");
    }

    /**
     * Gets Bracket Set (Read-Only)
     */
    public String getBracketSet2() {
        return getConfig("d078");
    }

    /**
     * Gets Bracket Order (Read-Only)
     */
    public String getBracketOrder2() {
        return getConfig("d07a");
    }

    /**
     * Gets Reverse Command Dial (Read-Only)
     */
    public String getReverseCommandDial2() {
        return getConfig("d086");
    }

    /**
     * Gets No CF Card Release (Read-Only)
     */
    public String getNoCfCardRelease() {
        return getConfig("d08a");
    }

    /**
     * Gets Image Comment String
     */
    public String getImageCommentString() {
        return getConfig("d090");
    }

    /**
     * Sets Image Comment String
     */
    public void setImageCommentString(String value) {
        setConfig("d090", value);
    }

    /**
     * Gets Image Comment Enable (Read-Only)
     */
    public String getImageCommentEnable() {
        return getConfig("d091");
    }

    /**
     * Gets Image Rotation (Read-Only)
     */
    public String getImageRotation() {
        return getConfig("d092");
    }

    /**
     * Gets Bracketing Enable (Read-Only)
     */
    public String getBracketingEnable() {
        return getConfig("d0c0");
    }

    /**
     * Gets Exposure Bracketing Program (Read-Only)
     */
    public String getExposureBracketingProgram() {
        return getConfig("d0c2");
    }

    /**
     * Gets Auto Exposure Bracket Count (Read-Only)
     */
    public String getAutoExposureBracketCount() {
        return getConfig("d0c3");
    }

    /**
     * Gets White Balance Bracket Step (Read-Only)
     */
    public String getWhiteBalanceBracketStep() {
        return getConfig("d0c4");
    }

    /**
     * Gets White Balance Bracket Program (Read-Only)
     */
    public String getWhiteBalanceBracketProgram() {
        return getConfig("d0c5");
    }

    /**
     * Gets Lens ID (Read-Only)
     */
    public String getLensId() {
        return getConfig("d0e0");
    }

    /**
     * Gets Lens Sort (Read-Only)
     */
    public String getLensSort() {
        return getConfig("d0e1");
    }

    /**
     * Gets Lens Type (Read-Only)
     */
    public String getLensType() {
        return getConfig("d0e2");
    }

    /**
     * Gets Min. Focal Length (Read-Only)
     */
    public String getMinFocalLength() {
        return getConfig("d0e3");
    }

    /**
     * Gets Max. Focal Length (Read-Only)
     */
    public String getMaxFocalLength() {
        return getConfig("d0e4");
    }

    /**
     * Gets Max. Aperture at Min. Focal Length (Read-Only)
     */
    public String getMaxApertureAtMinFocalLength() {
        return getConfig("d0e5");
    }

    /**
     * Gets Max. Aperture at Max. Focal Length (Read-Only)
     */
    public String getMaxApertureAtMaxFocalLength() {
        return getConfig("d0e6");
    }

    /**
     * Gets Nikon Exposure Time (Read-Only)
     */
    public String getNikonExposureTime() {
        return getConfig("d100");
    }

    /**
     * Gets AC Power (Read-Only)
     */
    public String getAcPower2() {
        return getConfig("d101");
    }

    /**
     * Gets Warning Status (Read-Only)
     */
    public String getWarningStatus() {
        return getConfig("d102");
    }

    /**
     * Gets Maximum Shots (Read-Only)
     */
    public String getMaximumShots2() {
        return getConfig("d103");
    }

    /**
     * Gets AF Locked (Read-Only)
     */
    public String getAfLocked2() {
        return getConfig("d104");
    }

    /**
     * Gets AE Locked (Read-Only)
     */
    public String getAeLocked2() {
        return getConfig("d105");
    }

    /**
     * Gets FV Locked (Read-Only)
     */
    public String getFvLocked2() {
        return getConfig("d106");
    }

    /**
     * Gets Active AF Sensor (Read-Only)
     */
    public String getActiveAfSensor() {
        return getConfig("d108");
    }

    /**
     * Gets Flexible Program (Read-Only)
     */
    public String getFlexibleProgram2() {
        return getConfig("d109");
    }

    /**
     * Gets Exposure Meter (Read-Only)
     */
    public String getExposureMeter() {
        return getConfig("d10a");
    }

    /**
     * Gets CCD Serial Number (Read-Only)
     */
    public String getCcdSerialNumber() {
        return getConfig("d10d");
    }

    /**
     * Gets Camera Orientation (Read-Only)
     */
    public String getCameraOrientation2() {
        return getConfig("d10e");
    }

    /**
     * Gets Recording Media (Read-Only)
     */
    public String getRecordingMedia2() {
        return getConfig("d10b");
    }

    /**
     * Gets External Flash Attached (Read-Only)
     */
    public String getExternalFlashAttached() {
        return getConfig("d120");
    }

    /**
     * Gets External Flash Status (Read-Only)
     */
    public String getExternalFlashStatus() {
        return getConfig("d121");
    }

    /**
     * Gets External Flash Sort (Read-Only)
     */
    public String getExternalFlashSort() {
        return getConfig("d122");
    }

    /**
     * Gets External Flash Compensation (Read-Only)
     */
    public String getExternalFlashCompensation() {
        return getConfig("d124");
    }

    /**
     * Gets External Flash Mode (Read-Only)
     */
    public String getExternalFlashMode() {
        return getConfig("d125");
    }

    /**
     * Gets Flash Exposure Compensation (Read-Only)
     */
    public String getFlashExposureCompensation2() {
        return getConfig("d126");
    }

    /**
     * Gets Optimize Image (Read-Only)
     */
    public String getOptimizeImage2() {
        return getConfig("d140");
    }

    /**
     * Gets Saturation (Read-Only)
     */
    public String getSaturation2() {
        return getConfig("d142");
    }

    /**
     * Gets AF Beep Mode (Read-Only)
     */
    public String getAfBeepMode2() {
        return getConfig("d160");
    }

    /**
     * Gets Autofocus Mode (Read-Only)
     */
    public String getAutofocusMode() {
        return getConfig("d161");
    }

    /**
     * Gets AF Assist Lamp (Read-Only)
     */
    public String getAfAssistLamp() {
        return getConfig("d163");
    }

    /**
     * Gets Auto ISO P/A/DVP Setting (Read-Only)
     */
    public String getAutoIsoPADvpSetting() {
        return getConfig("d164");
    }

    /**
     * Gets Image Review (Read-Only)
     */
    public String getImageReview2() {
        return getConfig("d165");
    }

    /**
     * Gets AF Area Illumination (Read-Only)
     */
    public String getAfAreaIllumination2() {
        return getConfig("d166");
    }

    /**
     * Gets Flash Mode (Read-Only)
     */
    public String getFlashMode3() {
        return getConfig("d167");
    }

    /**
     * Gets Flash Commander Mode (Read-Only)
     */
    public String getFlashCommanderMode2() {
        return getConfig("d168");
    }

    /**
     * Gets Flash Sign (Read-Only)
     */
    public String getFlashSign2() {
        return getConfig("d169");
    }

    /**
     * Gets Remote Timeout (Read-Only)
     */
    public String getRemoteTimeout2() {
        return getConfig("d16b");
    }

    /**
     * Gets Viewfinder Grid Display (Read-Only)
     */
    public String getViewfinderGridDisplay() {
        return getConfig("d16c");
    }

    /**
     * Gets Flash Mode Manual Power (Read-Only)
     */
    public String getFlashModeManualPower2() {
        return getConfig("d16d");
    }

    /**
     * Gets Flash Mode Commander Power (Read-Only)
     */
    public String getFlashModeCommanderPower() {
        return getConfig("d16e");
    }

    /**
     * Gets CSM Menu (Read-Only)
     */
    public String getCsmMenu2() {
        return getConfig("d180");
    }

    /**
     * Gets Bracketing Frames and Steps (Read-Only)
     */
    public String getBracketingFramesAndSteps() {
        return getConfig("d190");
    }

    /**
     * Gets Exposure Display Status (Read-Only)
     */
    public String getExposureDisplayStatus() {
        return getConfig("d1b0");
    }

    /**
     * Gets Flash Open (Read-Only)
     */
    public String getFlashOpen2() {
        return getConfig("d1c0");
    }

    /**
     * Gets Flash Charged (Read-Only)
     */
    public String getFlashCharged2() {
        return getConfig("d1c1");
    }

}
