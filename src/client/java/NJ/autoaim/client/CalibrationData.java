package NJ.autoaim.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CalibrationData {
    private static double[] PITCH_INPUT;
    private static double[] PITCH_OUTPUT;
    private static double[] ARROW_OUTPUT;

    private static boolean dataLoaded = false;

    public static void loadCalibrationData() {
        if (dataLoaded) return;

        System.out.println("=== AutoAim: Starting to load calibration data ===");

        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            System.out.println("AutoAim: Config directory: " + configDir.toString());

            Path pitchInputPath = configDir.resolve("pitchInput.txt");
            Path pitchOutputPath = configDir.resolve("pitchOutput.txt");
            Path arrowOutputPath = configDir.resolve("arrowOutput.txt");

            System.out.println("AutoAim: Looking for files:");
            System.out.println("  - " + pitchInputPath + " (exists: " + Files.exists(pitchInputPath) + ")");
            System.out.println("  - " + pitchOutputPath + " (exists: " + Files.exists(pitchOutputPath) + ")");
            System.out.println("  - " + arrowOutputPath + " (exists: " + Files.exists(arrowOutputPath) + ")");

            // 載入三個文件
            PITCH_INPUT = loadDoubleArrayFromFile(pitchInputPath);
            PITCH_OUTPUT = loadDoubleArrayFromFile(pitchOutputPath);
            ARROW_OUTPUT = loadDoubleArrayFromFile(arrowOutputPath);

            dataLoaded = true;
            System.out.println("AutoAim calibration data loaded successfully!");
            System.out.println("PitchInput: " + PITCH_INPUT.length + " values");
            System.out.println("PitchOutput: " + PITCH_OUTPUT.length + " values");
            System.out.println("ArrowOutput: " + ARROW_OUTPUT.length + " values");

        } catch (Exception e) {
            System.err.println("Failed to load AutoAim calibration data: " + e.getMessage());
            e.printStackTrace();
            dataLoaded = false;
        }
    }

    private static double[] loadDoubleArrayFromFile(Path filePath) throws IOException {
        List<Double> values = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        values.add(Double.parseDouble(line));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid number format in " + filePath.getFileName() + ": " + line);
                    }
                }
            }
        }

        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static void loadDefaultData() {
        // 移除預設數據，必須從文件載入
        dataLoaded = false;
        System.err.println("AutoAim requires calibration files in config folder!");
    }

    public static double interpolate(double[] x, double[] y, double t) {
        if (x.length != y.length || x.length == 0) return 0;

        int mindex = 0;
        double newMin = Double.MAX_VALUE;

        for (int i = 0; i < x.length; i++) {
            double curMin = Math.abs(t - x[i]);
            if (curMin < newMin) {
                mindex = i;
                newMin = curMin;
            }
        }

        if (x[mindex] > t && mindex > 0) mindex = mindex - 1;
        if (mindex >= x.length - 1) mindex = x.length - 2;
        if (mindex < 0) mindex = 0;

        double xL = x[mindex];
        double xU = x[mindex + 1];
        if (xU == xL) return y[mindex]; // 避免除零

        double interpolation = (t - xL) / (xU - xL);
        double yL = y[mindex];
        double yU = y[mindex + 1];

        return interpolation * (yU - yL) + yL;
    }

    public static int coverDistance(double v0, double distance) {
        double v = v0;
        int n = 0;
        double dist = 0;

        while (dist < distance && n < 1000) { // 防止無限循環
            dist += v;
            v *= 0.99;
            n++;
        }

        return n;
    }

    public static double[] getTimeCompensatedOutput(int targetTime) {
        if (!dataLoaded) {
            loadCalibrationData();
            if (!dataLoaded) return new double[0]; // 數據載入失敗
        }

        // 從完整數據中提取對應時間的70個數據點
        int startIndex = targetTime * 70;
        double[] result = new double[70];

        for (int i = 0; i < 70; i++) {
            int index = startIndex + i;
            if (index < PITCH_OUTPUT.length) {
                result[i] = PITCH_OUTPUT[index];
            } else {
                // 如果超出範圍，使用最後一組數據
                int lastGroupStart = Math.max(0, (PITCH_OUTPUT.length / 70 - 1) * 70);
                int lastIndex = Math.min(lastGroupStart + i, PITCH_OUTPUT.length - 1);
                result[i] = PITCH_OUTPUT[lastIndex];
            }
        }

        return result;
    }

    public static double[] getPitchOutputInitial() {
        if (!dataLoaded) {
            loadCalibrationData();
            if (!dataLoaded) return new double[0]; // 數據載入失敗
        }

        // 返回前70個數據
        double[] result = new double[Math.min(70, PITCH_OUTPUT.length)];
        System.arraycopy(PITCH_OUTPUT, 0, result, 0, result.length);
        return result;
    }

    public static double[] getPitchInput() {
        if (!dataLoaded) {
            loadCalibrationData();
            if (!dataLoaded) return new double[0]; // 數據載入失敗
        }
        return PITCH_INPUT;
    }

    public static double[] getArrowOutput() {
        if (!dataLoaded) {
            loadCalibrationData();
            if (!dataLoaded) return new double[0]; // 數據載入失敗
        }
        return ARROW_OUTPUT;
    }

    public static boolean isDataLoaded() {
        return dataLoaded;
    }
}