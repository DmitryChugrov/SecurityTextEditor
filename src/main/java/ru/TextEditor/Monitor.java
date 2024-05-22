package ru.TextEditor;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Monitor extends Component {
    // блэклист приложений
    private static final List<String> knownScreenCaptureApps = Arrays.asList(
            "obs64.exe", // OBS Studio
            "fraps.exe", // Fraps
            "snagit32.exe", // Snagit
            "camstudio.exe", // CamStudio
            "bandicam.exe",// Bandicam
            "discord.exe", // Discord
            "snippingTool.exe" // Ножницы
    );

    public void start() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                List<String> runningScreenCaptureApps = findRunningScreenCaptureApps();
                if (runningScreenCaptureApps.isEmpty()) {
                    System.out.println("Нет запущенных приложений для захвата экрана.");
                } else {
                    StringBuilder message = new StringBuilder("Запущенные приложения для захвата экрана:\n");
                    for (String app : runningScreenCaptureApps) {
                        message.append(app).append("\n");
                    }
                    JOptionPane.showMessageDialog(null, message.toString());
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        monitorThread.start();


        Thread screenshotMonitorThread = new Thread(new FileSystemMonitor());
        screenshotMonitorThread.start();

    }

    private static List<String> findRunningScreenCaptureApps() {
        List<String> runningApps = new ArrayList<>();

        ProcessEntry32 processEntry = new ProcessEntry32();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                String exeFile = Native.toString(processEntry.szExeFile);
                if (knownScreenCaptureApps.contains(exeFile.toLowerCase())) {
                    runningApps.add(exeFile);
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }

        return runningApps;
    }

    public static class ProcessEntry32 extends Tlhelp32.PROCESSENTRY32 {
        public static class ByReference extends ProcessEntry32 implements com.sun.jna.Structure.ByReference {}

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwSize", "cntUsage", "th32ProcessID", "th32DefaultHeapID", "th32ModuleID",
                    "cntThreads", "th32ParentProcessID", "pcPriClassBase", "dwFlags", "szExeFile");
        }

        public ProcessEntry32() {
            super();
            dwSize = new WinDef.DWORD(this.size());
        }
    }
}
