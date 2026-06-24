package h848.software.gphoto4java.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapovací vrstva, která překládá standardizované názvy (StandardProperty)
 * na konkrétní PTP cesty používané konkrétním modelem fotoaparátu přes gphoto2.
 */
public class PropertyMapper {
    // Mapování StandardProperty na prioritní seznam PTP uzlů
    private static final Map<StandardProperty, List<String>> ALIASES = new HashMap<>();

    static {
        // Inicializace aliasů (od nejpravděpodobnějších standardů přes Canon až po Nikon)
        ALIASES.put(StandardProperty.CAMERA_NAME, Arrays.asList("cameramodel", "model"));
        ALIASES.put(StandardProperty.LENS_NAME, Arrays.asList("lensname"));
        ALIASES.put(StandardProperty.EXPOSURE_PROGRAM, Arrays.asList("expprogram", "autoexposuremode", "shootingmode", "exposureprogram", "capturemode"));
        ALIASES.put(StandardProperty.SHUTTER_SPEED, Arrays.asList("shutterspeed", "shutter", "exposuretime"));
        ALIASES.put(StandardProperty.APERTURE, Arrays.asList("f-number", "aperture", "fnumber"));
        ALIASES.put(StandardProperty.ISO, Arrays.asList("iso", "isospeed"));
        ALIASES.put(StandardProperty.WHITE_BALANCE, Arrays.asList("whitebalance", "wb"));
        ALIASES.put(StandardProperty.IMAGE_FORMAT, Arrays.asList("imageformat", "imagequality", "picturequality"));
        ALIASES.put(StandardProperty.FOCUS_MODE, Arrays.asList("focusmode", "afmode", "autofocus"));
        ALIASES.put(StandardProperty.METERING_MODE, Arrays.asList("exposuremetermode", "expmeteringmode", "meteringmode", "metering"));
        ALIASES.put(StandardProperty.FOCAL_LENGTH, Arrays.asList("focallength", "focal"));
    }

    private final Map<StandardProperty, String> resolvedMap = new HashMap<>();

    /**
     * Zjistí, pod jakým jménem se dané vlastnosti skutečně nacházejí v právě připojeném foťáku.
     * @param availableConfigs Kompletní seznam cest vrácených z `gphoto2 --list-config`
     */
    public void initialize(List<String> availableConfigs) {
        resolvedMap.clear();

        for (StandardProperty standardProp : StandardProperty.values()) {
            List<String> aliases = ALIASES.get(standardProp);
            if (aliases != null) {
                for (String alias : aliases) {
                    // gphoto2 obvykle vrací celou cestu (např. /main/capturesettings/expprogram)
                    // Hledáme shodu podle koncového uzlu.
                    for (String configPath : availableConfigs) {
                        String[] parts = configPath.split("/");
                        String leaf = parts[parts.length - 1].toLowerCase();
                        
                        if (leaf.equals(alias.toLowerCase())) {
                            resolvedMap.put(standardProp, configPath);
                            break;
                        }
                    }
                    if (resolvedMap.containsKey(standardProp)) {
                        break; // Našli jsme pro tuto vlastnost shodu, jdeme na další StandardProperty
                    }
                }
            }
        }
    }

    /**
     * Vrátí přesný PTP název (path) pro danou standardizovanou vlastnost.
     * @return PTP cesta nebo null, pokud fotoaparát tuto vlastnost nepodporuje.
     */
    public String getNativeName(StandardProperty prop) {
        return resolvedMap.get(prop);
    }
}
