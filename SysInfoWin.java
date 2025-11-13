import com.sun.jna.Platform;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Arrays;

public class SysInfoWin {

    public static void main(String[] args) {

        if (!Platform.isWindows()) {
            System.out.println("This program can only run on Windows.");
            return;
        }

        printOSInfo();
        printUserAndComputer();
        printArchitecture();
        printMemoryInfo();
        printPagefile();
        printCPUInfo();
        printDrives();
    }

    private static void printOSInfo() {
        String version = System.getProperty("os.version");
        String name = System.getProperty("os.name");

        System.out.println("OS: " + name + " (version " + version + ")");
    }

    private static void printUserAndComputer() {
        System.out.println("Computer Name: " + System.getenv("COMPUTERNAME"));
        System.out.println("User: " + System.getProperty("user.name"));
    }

    private static void printArchitecture() {
        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
        String archWOW = System.getenv("PROCESSOR_ARCHITEW6432");

        String result;

        if (archWOW != null && archWOW.contains("64")) {
            result = "x64 (AMD64)";
        } else if (arch.contains("64")) {
            result = "x64 (AMD64)";
        } else if (arch.contains("86")) {
            result = "x86";
        } else if (arch.contains("ARM")) {
            result = "ARM/ARM64";
        } else {
            result = "Unknown (" + arch + ")";
        }

        System.out.println("Architecture: " + result);
    }

    private static void printMemoryInfo() {
        WinBase.MEMORYSTATUSEX mem = new WinBase.MEMORYSTATUSEX();
        Kernel32.INSTANCE.GlobalMemoryStatusEx(mem);

        long totalPhys = mem.ullTotalPhys.longValue() / (1024 * 1024);
        long availPhys = mem.ullAvailPhys.longValue() / (1024 * 1024);

        long totalVirtual = mem.ullTotalVirtual.longValue() / (1024 * 1024);

        System.out.printf("RAM: %dMB / %dMB%n", (totalPhys - availPhys), totalPhys);
        System.out.println("Virtual Memory: " + totalVirtual + "MB");
        System.out.println("Memory Load: " + mem.dwMemoryLoad + "%");
    }

    private static void printPagefile() {
        Psapi.PERFORMANCE_INFORMATION info = new Psapi.PERFORMANCE_INFORMATION();
        Psapi.INSTANCE.GetPerformanceInfo(info, info.size());

        long pageSize = info.PageSize.longValue();
        long totalPF = info.CommitLimit.longValue() * pageSize / (1024 * 1024);
        long usedPF = info.CommitTotal.longValue() * pageSize / (1024 * 1024);

        System.out.printf("Pagefile: %dMB / %dMB%n", usedPF, totalPF);
    }

    private static void printCPUInfo() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] cpuInfo =
                Kernel32Util.getLogicalProcessorInformation();

        long count = Arrays.stream(cpuInfo)
                .filter(info -> info.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore)
                .count();

        System.out.println("Processors: " + count);
    }

    private static void printDrives() {
        File[] drives = File.listRoots();

        System.out.println("Drives:");
        for (File drive : drives) {

            long total = drive.getTotalSpace() / (1024L * 1024 * 1024);
            long free = drive.getFreeSpace() / (1024L * 1024 * 1024);

            String fs = getDriveFS(drive.getPath());

            System.out.printf("  - %s (%s): %d GB free / %d GB total%n",
                    drive.getPath(), fs, free, total);
        }
    }

    private static String getDriveFS(String path) {
        char[] volName = new char[256];
        char[] fsName = new char[256];

        IntByReference serial = new IntByReference();
        IntByReference maxCompLen = new IntByReference();
        IntByReference flags = new IntByReference();

        boolean ok = Kernel32.INSTANCE.GetVolumeInformation(
                path,
                volName,
                volName.length,
                serial,
                maxCompLen,
                flags,
                fsName,
                fsName.length
        );

        if (!ok) return "Unknown";

        return new String(fsName).trim();
    }
}
