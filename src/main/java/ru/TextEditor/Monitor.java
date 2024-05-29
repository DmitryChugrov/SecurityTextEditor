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
            "snippingTool.exe", // Ножницы
            // Программы для захвата экрана и создания скриншотов
            "Snagit.exe", // Snagit
            "Greenshot.exe", // Greenshot
            "Lightshot.exe", // Lightshot
            "ShareX.exe", // ShareX
            "PicPick.exe", // PicPick
            "FSCapture.exe", // FastStone Capture
            "Jing.exe", // Jing
            "Screenpresso.exe", // Screenpresso
            "ScreenshotCaptor.exe", // Screenshot Captor

            // Программы для удаленного управления компьютером
            "TeamViewer.exe", // TeamViewer
            "AnyDesk.exe", // AnyDesk
            "ChromeRemoteDesktopHost.exe", // Chrome Remote Desktop (или запуск через браузер Chrome)
            "RemotePC.exe", // RemotePC
            "LogMeIn.exe", // LogMeIn
            "VNCViewer.exe", // VNC Connect (RealVNC)
            "UltraVNC.exe", // UltraVNC (или VNCViewer.exe для просмотра)
            "Splashtop.exe", // Splashtop
            "ParallelsAccess.exe", // Parallels Access
            "rutview.exe", // Remote Utilities Viewer
            "rutserv.exe", // Remote Utilities Host

            // Программы, которые совмещают функции захвата экрана и удаленного управления
            "TeamViewer.exe", // TeamViewer
            "AnyDesk.exe", // AnyDesk
            "RemotePC.exe", // RemotePC
            "LogMeIn.exe", // LogMeIn
            "Splashtop.exe" // Splashtop
    );

    public void start() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                List<String> runningScreenCaptureApps = findRunningScreenCaptureApps();
                if (runningScreenCaptureApps.isEmpty()) {
//                    System.out.println("Нет запущенных приложений для захвата экрана.");
                } else {
                    StringBuilder message = new StringBuilder("Обнаружены приложения для захвата экрана. Дальнейшее работа с файлом может быть небезопасной. Завершите процесс:\n");
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
